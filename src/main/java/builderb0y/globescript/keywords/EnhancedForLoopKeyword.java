package builderb0y.globescript.keywords;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.*;
import builderb0y.globescript.StructureParser.*;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public class EnhancedForLoopKeyword extends KeywordData {

	public EnhancedForLoopKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		int revert = parser.reader.cursor;
		Token.Builder builder = Token.builder().with(word);
		Token label = parser.reader.nextIdentifierAfterWhitespace();
		if (label != null) builder.with(label.initIdentifier(Colors.LABEL, TokenInfo.NON_VALUE));
		Token open = parser.tryOpenGroup();
		if (open != null) {
			builder.with(open);
			parser.environment.addLoopLabel(label != null ? label.getIdentifierText().toString() : null);
			if (this.processNextIteration(parser, builder)) {
				while (parser.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, builder)) {
					if (!this.processNextIteration(parser, builder)) {
						builder.with(parser.abort(ExpressionParser.COLONS, "Skipping remaining iteration terms due to previous syntax error"));
						break;
					}
				}
				Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
				if (colon == null) colon = parser.error("Expected ':'");
				builder.with(colon);

				Token body = parser.nextNullableScript();
				if (body == null) body = parser.error("Expected loop body");
				builder.with(body);

				return builder.with(parser.closeGroup()).build(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
			}
			else {
				parser.environment.pop();
				parser.reader.cursor = revert;
				return null;
			}
		}
		else {
			return builder.with(parser.error("Expected '('")).build(parser.reader.input, TokenInfo.ERROR);
		}
	}

	public boolean processNextIteration(ExpressionParser parser, Token.Builder builder) {
		MultiStructure<ParameterStructure> declarations = new MultiParser<>(true, new ParameterParser(), StructureParser.operator(",")).parse(parser);
		Token in;
		if (declarations != null && (in = parser.reader.hasIdentifierAfterWhitespace("in", Colors.KEYWORD)) != null) {
			List<Token> flatTypes = new ArrayList<>(4);
			for (ParameterStructure parameter : declarations.values()) {
				for (Token name : parameter.names()) {
					parser.environment.addUserParameter(name.getIdentifierText().toString(), parameter.type().info);
					flatTypes.add(parameter.type());
				}
			}
			builder.with(declarations).with(in.withInfo(TokenInfo.NON_VALUE));
			RangeStructure range = new RangeParser().parse(parser);
			if (range != null) {
				RawTypeModel first = null;
				for (Token type : flatTypes) {
					if (!type.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
						type.withTooltip("Can't use type " + type.info.type() + " in range iteration");
					}
					if (first == null) {
						first = type.info.type();
					}
					else if (!type.info.type().equals(first)) {
						type.withTooltip("All iteration variables must be of the same type for range iteration");
					}
				}
				if (range.interval() != null) {
					if (!range.interval().lowerBound().info.isAssignableToOrCanCast(parser.environment, first, Plicity.IMPLICIT)) {
						range.interval().lowerBound().withTooltip("Range bound type (" + range.interval().lowerBound().info.type() + ") must match variable type (" + first + ")");
					}
					if (!range.interval().upperBound().info.isAssignableToOrCanCast(parser.environment, first, Plicity.IMPLICIT)) {
						range.interval().upperBound().withTooltip("Range bound type (" + range.interval().upperBound().info.type() + ") must match variable type (" + first + ")");
					}
				}
				builder.with(range);
			}
			else {
				Token iterable = parser.nextNullableScript();
				if (iterable == null) {
					iterable = parser.error("Expected value to iterate over");
				}
				else if (iterable.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.iterable, Plicity.IMPLICIT)) {
					if (iterable.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.list, Plicity.IMPLICIT)) {
						switch (flatTypes.size()) {
							case 1 -> {}
							case 2 -> {
								if (!flatTypes.get(0).info.type().equals(parser.environment.standardTypes.int_)) {
									flatTypes.get(0).withTooltip("When iterating over a list with 2 variables, the first must be of type int.");
								}
							}
							default -> {
								for (int index = 2; index < flatTypes.size(); index++) {
									flatTypes.get(index).withTooltip("Trailing variable: lists can only be iterated over with 'element' or 'index, element' form.");
								}
							}
						}
					}
				}
				else if (iterable.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.map, Plicity.IMPLICIT)) {
					if (flatTypes.size() != 2) {
						for (Token type : flatTypes) {
							type.withTooltip("Trailing variable: maps can only be iterated over with 'key, value' form.");
						}
					}
				}
				else if (iterable.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.iterator, Plicity.IMPLICIT)) {
					if (flatTypes.size() != 1) {
						for (Token type : flatTypes) {
							type.withTooltip("Trailing variable: maps can only be iterated over with 'key, value' form.");
						}
					}
				}
				builder.with(iterable);
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "for label (int i in range[0, 10): body)";
	}
}