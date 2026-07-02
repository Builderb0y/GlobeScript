package builderb0y.globescript;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ConstantValue.EnumConstantValue;
import builderb0y.globescript.ConstantValue.IntegerConstantValue;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.PendingEnvironment.ListBackedStructure;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public interface StructureParser<T extends OneOrMoreTokens> {

	public static final StructureParser<Token>
		IDENTIFIER      = (ExpressionParser parser) -> parser.reader.nextIdentifierAfterWhitespace(),
		OPERATOR        = (ExpressionParser parser) -> parser.reader.nextOperatorAfterWhitespace(),
		NULLABLE_SCRIPT =  ExpressionParser::nextNullableScript,
		SCRIPT          =  ExpressionParser::nextScript;

	public static StructureParser<Token> operator(CharSequence operator) {
		return (ExpressionParser parser) -> parser.reader.hasOperatorAfterWhitespace(operator, Colors.OPERATOR);
	}

	public static StructureParser<Token> operators(Set<CharSequence> operators) {
		return (ExpressionParser parser) -> parser.reader.hasOperatorAfterWhitespace(operators, Colors.OPERATOR);
	}

	public abstract @Nullable T parse(ExpressionParser parser);

	public default StructureParser<T> then(BiConsumer<ExpressionParser, T> action) {
		return (ExpressionParser parser) -> {
			T result = this.parse(parser);
			action.accept(parser, result);
			return result;
		};
	}

	public default <T2 extends OneOrMoreTokens> StructureParser<T2> map(Function<T, T2> mapper) {
		return (ExpressionParser parser) -> mapper.apply(this.parse(parser));
	}

	public default StructureParser<T> guard(Predicate<ExpressionParser> predicate) {
		return (ExpressionParser parser) -> predicate.test(parser) ? this.parse(parser) : null;
	}

	public static interface Structure extends OneOrMoreTokens {}

	public static record GroupStructure<T extends OneOrMoreTokens>(@NotNull Token open, T content, @Nullable Token close) implements Structure {

		@Override
		public void addTo(List<Token> list) {
			list.add(this.open);
			if (this.content != null) list.add(this.content.group());
			if (this.close != null) list.add(this.close);
		}

		@Override
		public Token group() {
			Token content = this.content.group();
			return new Token(this.open.getEntireText(), content.info, this.open, content, this.close);
		}
	}

	public static final class GroupParser<T extends OneOrMoreTokens> implements StructureParser<GroupStructure<T>> {

		public final boolean scoped;
		public final StructureParser<T> contentParser;

		public GroupParser(boolean scoped, StructureParser<T> contentParser) {
			this.scoped = scoped;
			this.contentParser = contentParser;
		}

		@Override
		public @Nullable GroupStructure<T> parse(ExpressionParser parser) {
			Token open = this.scoped ? parser.tryOpenGroup() : parser.reader.hasAfterWhitespace('(', Colors.GROUP);
			if (open == null) return null;
			T content = this.contentParser.parse(parser);
			Token close = this.scoped ? parser.closeGroup() : parser.closeUnscopedGroup();
			return new GroupStructure<>(open, content, close);
		}
	}

	public static record NameEqualsValue(@NotNull Token name, @Nullable Token equals, @Nullable Token value) implements Structure {

		@Override
		public void addTo(List<Token> list) {
			list.add(this.name);
			if (this.equals != null) list.add(this.equals);
			if (this.value != null) list.add(this.value);
		}

		@Override
		public Token group() {
			return new Token(this.name.getEntireText(), TokenInfo.NON_VALUE, this.name, this.equals, this.value);
		}
	}

	public static class NameEqualsValueParser implements StructureParser<NameEqualsValue> {

		public TokenInfo inferredType;

		public NameEqualsValueParser withType(TokenInfo inferredType) {
			this.inferredType = inferredType;
			return this;
		}

		@Override
		public @Nullable NameEqualsValue parse(ExpressionParser parser) {
			Token name = parser.reader.nextIdentifierAfterWhitespace();
			if (name == null) return null;
			Token operator = parser.reader.hasOperatorAfterWhitespace(ExpressionParser.DECLARE_ASSIGNMENTS, Colors.OPERATOR);
			if (operator == null) operator = parser.error("Expected '=' or ':='");
			Token value = parser.nextRhsOfAssignment(this.inferredType);
			return new NameEqualsValue(name, operator, value);
		}
	}

	public static record MultiStructure<T>(List<Token> tokens, List<T> values) implements Structure {

		@Override
		public void addTo(List<Token> list) {
			list.addAll(this.tokens);
		}

		@Override
		public Token group() {
			return new Token(this.tokens.getFirst().getEntireText(), TokenInfo.NON_VALUE, this.tokens);
		}
	}

	public static class MultiParser<T extends OneOrMoreTokens> implements StructureParser<MultiStructure<T>> {

		public final boolean delimitersMandatory;
		public final StructureParser<T> valueParser;
		public final StructureParser<Token> delimiterParser;

		public MultiParser(boolean delimitersMandatory, StructureParser<T> valueParser, StructureParser<Token> delimiterParser) {
			this.delimitersMandatory = delimitersMandatory;
			this.valueParser = valueParser;
			this.delimiterParser = delimiterParser;
			if (delimitersMandatory) Objects.requireNonNull(delimiterParser, "delimiterParser");
		}

		@Override
		public @Nullable MultiStructure<T> parse(ExpressionParser parser) {
			T first = this.valueParser.parse(parser);
			if (first == null) return null;
			List<T> values = new ArrayList<>(4);
			values.add(first);
			List<Token> tokens = new ArrayList<>(8);
			tokens.add(first.group());
			while (true) {
				if (this.delimiterParser != null) {
					Token delimiter = this.delimiterParser.parse(parser);
					if (delimiter == null && this.delimitersMandatory) break;
					tokens.add(delimiter);
				}
				T value = this.valueParser.parse(parser);
				if (value != null) {
					tokens.add(value.group());
					values.add(value);
				}
				else if (!this.delimitersMandatory) {
					break;
				}
			}
			return new MultiStructure<>(tokens, values);
		}
	}

	public static record ParameterStructure(List<Token> tokens, Token type, List<Token> names) implements Structure {

		@Override
		public void addTo(List<Token> list) {
			list.addAll(this.tokens);
		}

		@Override
		public Token group() {
			return new Token(this.type.getEntireText(), this.type.info, this.tokens);
		}
	}

	public static class ParameterParser implements StructureParser<ParameterStructure> {

		@Override
		public @Nullable ParameterStructure parse(ExpressionParser parser) {
			Token type = parser.reader.nextIdentifierAfterWhitespace();
			if (type == null) return null;
			TypeData typeData = parser.environment.getType(type.getIdentifierText().toString());
			if (typeData != null) {
				typeData.applyColor(type).withInfo(typeData.info);
				if (typeData.info.type().equals(parser.environment.standardTypes.void_)) {
					type.withTooltip("Void-typed variables are not allowed.");
				}
			}
			else {
				type.withColor(Colors.ERROR).withTooltip("Unknown type");
			}
			Token star = parser.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
			if (star != null) {
				GroupStructure<MultiStructure<Token>> results = new GroupParser<>(false, new MultiParser<>(true, StructureParser.IDENTIFIER.then((ExpressionParser p, Token token) -> token.withColor(Colors.PARAMETER).withInfo(type.info)), operator(","))).parse(parser);
				if (results == null) return new ParameterStructure(List.of(type, star, parser.error("Expected '('")), type, Collections.emptyList());
				List<Token> tokens = new ArrayList<>();
				tokens.add(type);
				tokens.add(star);
				results.addTo(tokens);
				return new ParameterStructure(tokens, type, results.content.values);
			}
			else {
				Token name = parser.reader.nextIdentifierAfterWhitespace(Colors.PARAMETER);
				if (name != null) {
					name.withInfo(type.info);
					return new ParameterStructure(List.of(type, name), type, Collections.singletonList(name));
				}
				else {
					return new ParameterStructure(List.of(type, parser.error("Expected name")), type, Collections.emptyList());
				}
			}
		}
	}

	public static record IntervalStructure(List<Token> tokens, Token open, Token lowerBound, Token upperBound, Token close) implements ListBackedStructure {

		public boolean lowerBoundInclusive() {
			return this.open.getEntireText().charAt(this.open.range.getStartOffset()) == '[';
		}

		public boolean upperBoundInclusive() {
			return this.close.getEntireText().charAt(this.close.range.getStartOffset()) == ']';
		}
	}

	public static class IntervalParser implements StructureParser<IntervalStructure> {

		@Override
		public @Nullable IntervalStructure parse(ExpressionParser parser) {
			Token open = parser.tryOpenGroup((int c) -> c == '[' || c == '(');
			if (open == null) {
				return null;
			}

			Token lowerBound = parser.orError(parser.nextNullableScript(), "Expected lower bound");
			Token comma = parser.orError(parser.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR), "Expected ','");
			Token upperBound = parser.orError(parser.nextNullableScript(), "Expected upper bound");
			Token close = parser.closeGroup((int c) -> c == ']' || c == ')');

			return new IntervalStructure(Token.builder().with(open).with(lowerBound).with(comma).with(upperBound).with(close), open, lowerBound, upperBound, close);
		}
	}

	public static record RangeStructure(List<Token> tokens, @Nullable Token backwards, @Nullable IntervalStructure interval, @Nullable Token step) implements ListBackedStructure {

	}

	public static class RangeParser implements StructureParser<RangeStructure> {

		@Override
		public @Nullable RangeStructure parse(ExpressionParser parser) {
			int revert = parser.reader.cursor;
			Token backwards = parser.reader.hasOperatorAfterWhitespace("-", Colors.OPERATOR);
			Token range = parser.reader.hasIdentifierAfterWhitespace("range", Colors.KEYWORD);
			if (range != null) {
				range.withInfo(TokenInfo.NON_VALUE);
				IntervalStructure interval = new IntervalParser().parse(parser);
				if (interval != null) {
					Token mod = parser.reader.hasOperatorAfterWhitespace("%", Colors.OPERATOR);
					if (mod != null) {
						Token step = parser.nextExponent();
						return new RangeStructure(Token.builder().with(backwards).with(range).with(interval.group(TokenInfo.NON_VALUE)).with(mod).with(step), backwards, interval, step);
					}
					else {
						return new RangeStructure(Token.builder().with(backwards).with(range).with(interval), backwards, interval, null);
					}
				}
				else {
					return new RangeStructure(Token.builder().with(backwards).with(range).with(parser.error("Expected interval")), backwards, null, null);
				}
			}
			else {
				parser.reader.cursor = revert;
				return null;
			}
		}
	}

	public static sealed interface CaseMatchStructure extends Structure {}

	public static record SingleCaseMatch(Token value) implements CaseMatchStructure {

		@Override
		public void addTo(List<Token> list) {
			list.add(this.value);
		}

		@Override
		public Token group() {
			return this.value;
		}
	}

	public static record RangeCaseMatch(Token range, @Nullable IntervalStructure interval) implements CaseMatchStructure {

		@Override
		public void addTo(List<Token> list) {
			list.add(this.range);
			if (this.interval != null) this.interval.addTo(list);
		}

		@Override
		public Token group() {
			return Token.builder().with(this.range).withAll(this.interval).build(this.range.getEntireText(), TokenInfo.NON_VALUE);
		}
	}

	public static class CaseMatchParser implements StructureParser<CaseMatchStructure> {

		public final RawTypeModel baseValueType;

		public CaseMatchParser(RawTypeModel baseValueType) {
			this.baseValueType = baseValueType;
		}

		@Override
		public @Nullable StructureParser.CaseMatchStructure parse(ExpressionParser parser) {
			Token range = parser.reader.hasIdentifierAfterWhitespace("range", Colors.KEYWORD);
			if (range != null) {
				if (this.baseValueType.enumConstantNames != null) {
					range.withTooltip("Cannot use ranges for enum switch");
				}
				IntervalStructure interval = new IntervalParser().parse(parser);
				if (interval != null) {
					this.verifyBound(parser, interval.lowerBound);
					this.verifyBound(parser, interval.upperBound);
					if (
						interval.lowerBound.info.constant() instanceof IntegerConstantValue lowerConstant &&
						interval.upperBound.info.constant() instanceof IntegerConstantValue upperConstant
					) {
						int lowerValue = lowerConstant.intValue();
						int upperValue = upperConstant.intValue();
						if (!interval.lowerBoundInclusive()) lowerValue++;
						if (!interval.upperBoundInclusive()) upperValue--;
						if (!(lowerValue <= upperValue)) {
							range.withTooltip("Empty range: lower bound is greater than upper bound.");
						}
					}
				}
				return new RangeCaseMatch(range.withInfo(TokenInfo.NON_VALUE), interval);
			}
			else {
				Token expression = parser.nextNullableSingleExpression();
				if (expression != null) {
					this.verifyBound(parser, expression);
					return new SingleCaseMatch(expression);
				}
				else {
					return null;
				}
			}
		}

		public void verifyBound(ExpressionParser parser, Token bound) {
			if (bound.isLeaf() && this.baseValueType.enumConstantNames != null && this.baseValueType.enumConstantNames.contains(bound.getIdentifierText().toString())) {
				bound.initIdentifier(Colors.STATIC_FIELD, new TokenInfo(new EnumConstantValue(this.baseValueType, bound.getIdentifierText().toString())));
			}
			if (!bound.info.constant().isCompileTimeConstant()) {
				bound.withTooltip("Not a compile-time constant");
			}
			if (!bound.info.isAssignableToOrCanCast(parser.environment, this.baseValueType, Plicity.IMPLICIT)) {
				bound.withTooltip("Incorrect type: expected " + this.baseValueType + ", got " + bound.info.type());
			}
		}
	}

	public static sealed interface ICaseStructure extends ListBackedStructure {

		public abstract @Nullable Token body();
	}

	public static record DefaultStructure(List<Token> tokens, Token defaultWord, @Nullable Token body) implements ICaseStructure {}

	public static record CaseStructure(List<Token> tokens, @Nullable MultiStructure<CaseMatchStructure> matches, @Nullable Token body) implements ICaseStructure {}

	public static class CaseParser implements StructureParser<ICaseStructure> {

		public final RawTypeModel baseValueType;

		public CaseParser(RawTypeModel baseValueType) {
			this.baseValueType = baseValueType;
		}

		@Override
		public @Nullable ICaseStructure parse(ExpressionParser parser) {
			Token case_ = parser.reader.hasIdentifierAfterWhitespace("case", Colors.KEYWORD);
			if (case_ != null) {
				Token.Builder builder = Token.builder().with(case_.withInfo(TokenInfo.NON_VALUE));
				Token open = parser.tryOpenGroup();
				if (open != null) {
					MultiStructure<CaseMatchStructure> matches = new MultiParser<>(true, new CaseMatchParser(this.baseValueType), StructureParser.operator(",")).parse(parser);
					Token body;
					builder
					.with(open)
					.with(matches != null ? matches.group() : parser.error("Expected values to match"))
					.with(parser.orError(parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR), "Expected ':'"))
					.with(parser.orError(body = parser.nextNullableScript(), "Expected case body"))
					.with(parser.closeGroup());
					return new CaseStructure(builder, matches, body);
				}
				else {
					return new CaseStructure(List.of(case_, parser.error("Expected '('")), null, null);
				}
			}
			Token default_ = parser.reader.hasIdentifierAfterWhitespace("default", Colors.KEYWORD);
			if (default_ != null) {
				default_.withInfo(TokenInfo.NON_VALUE);
				Token open = parser.tryOpenGroup();
				if (open != null) {
					Token body = parser.orError(parser.nextNullableScript(), "Expected default body");
					Token close = parser.closeGroup();
					return new DefaultStructure(Token.builder().with(default_).with(open).with(body).with(close), default_, body);
				}
				else {
					return new DefaultStructure(Token.builder().with(default_).with(parser.error("Expected '('")), default_, null);
				}
			}
			return null;
		}
	}
}