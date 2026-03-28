package builderb0y.globescript.keywords;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.*;
import builderb0y.globescript.StructureParser.*;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.EnvironmentModel.ParameterModel;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public class ClassKeyword extends KeywordData {

	public ClassKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token clazz) {
		this.applyColor(clazz).withInfo(TokenInfo.NON_VALUE);
		Token name = parser.reader.nextIdentifierAfterWhitespace(Colors.TYPE);
		if (name != null) {
			String nameText = name.getIdentifierText().toString();
			RawTypeModel classType = new RawTypeModel(0, nameText, parser.environment.standardTypes.object, RawTypeModel.EMPTY_ARRAY, null);
			name.info = new TokenInfo(classType);
			parser.environment.addUserType(nameText, new TokenInfo(classType));
			parser.environment.addUserConstructor(classType, "new", new TokenInfo(classType));
			GroupStructure<MultiStructure<CustomClassFieldData>> fieldsGroup = new GroupParser<>(false, new MultiParser<>(false, this::parseField, StructureParser.operators(ExpressionParser.COMMAS))).parse(parser);
			if (fieldsGroup != null && fieldsGroup.content() != null) {
				List<ParameterModel>
					//none = new ArrayList<>();
					some = new ArrayList<>(),
					all = new ArrayList<>();
				for (CustomClassFieldData fieldData : fieldsGroup.content().values()) {
					TypeData fieldTypeData = parser.environment.getType(fieldData.type.getIdentifierText().toString());
					RawTypeModel fieldType = fieldTypeData != null ? fieldTypeData.info.type() : RawTypeModel.ERROR;
					for (NameEqualsValue nameAndValue : fieldData.namesAndInitializers) {
						String fieldName = nameAndValue.name().getIdentifierText().toString();
						parser.environment.addUserInstanceField(classType, fieldName, fieldData.type.info.assignable(true));
						ParameterModel parameter = new ParameterModel(fieldName, fieldType, false);
						all.add(parameter);
						if (nameAndValue.value() == null) {
							some.add(parameter);
						}
					}
				}
				if (some.size() > 0) parser.environment.addUserConstructor(classType, "new", new TokenInfo(classType), some.toArray(new ParameterModel[some.size()]));
				if (all.size() > some.size()) parser.environment.addUserConstructor(classType, "new", new TokenInfo(classType), all.toArray(new ParameterModel[all.size()]));
			}
			return Token.builder().with(clazz).with(name).withAll(fieldsGroup).build(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, clazz, parser.error("Expected class name"));
		}
	}

	public CustomClassFieldData parseField(ExpressionParser parser) {
		Token type = parser.reader.nextIdentifierAfterWhitespace();
		if (type == null) return null;
		TypeData typeData = parser.environment.getType(type.getIdentifierText().toString());
		if (typeData != null) typeData.applyColor(type).withInfo(typeData.info);
		else type.error("Unknown type");

		Token star = parser.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
		if (star != null) {
			GroupStructure<MultiStructure<NameEqualsValue>> contents = new GroupParser<>(false, new MultiParser<>(false, (ExpressionParser p) -> this.parseSingleField(p, type.info), StructureParser.operators(ExpressionParser.COMMAS))).parse(parser);
			if (contents != null) {
				return new CustomClassFieldData(Token.builder().with(type).with(star).withAll(contents), type, contents.content() != null ? contents.content().values() : Collections.emptyList());
			}
			else {
				return new CustomClassFieldData(List.of(type, star, parser.error("Expected '(declarations)'")), type, Collections.emptyList());
			}
		}
		else {
			NameEqualsValue nameEqualsValue = this.parseSingleField(parser, type.info);
			if (nameEqualsValue != null) {
				return new CustomClassFieldData(Token.builder().with(type).withAll(nameEqualsValue), type, Collections.singletonList(nameEqualsValue));
			}
			else {
				return new CustomClassFieldData(List.of(type, parser.error("Expected '*' or name")), type, Collections.emptyList());
			}
		}
	}

	public NameEqualsValue parseSingleField(ExpressionParser parser, TokenInfo expectedType) {
		Token name = parser.reader.nextIdentifierAfterWhitespace(Colors.INSTANCE_FIELD);
		if (name != null) {
			name.withInfo(expectedType);
			Token equals = parser.reader.hasOperatorAfterWhitespace("=", Colors.OPERATOR);
			if (equals != null) {
				Token value = parser.nextNullableSingleExpression();
				if (value == null) {
					value = parser.error("Expected expression");
				}
				else if (!value.info.isAssignableToOrCanCast(parser.environment, expectedType.type(), Plicity.IMPLICIT)) {
					value.withTooltip("Can't implicitly cast from " + value.info.type() + " to " + expectedType);
				}
				return new NameEqualsValue(name, equals, value);
			}
			else {
				return new NameEqualsValue(name, null, null);
			}
		}
		else {
			return null;
		}
	}

	@Override
	public String toString() {
		return this.name + " ExampleName(fields)";
	}

	public static record CustomClassFieldData(@NotNull List<Token> tokens, @NotNull Token type, @NotNull List<NameEqualsValue> namesAndInitializers) implements Structure {

		@Override
		public void addTo(List<Token> list) {
			list.addAll(this.tokens);
		}

		@Override
		public Token group() {
			return new Token(this.type.getEntireText(), TokenInfo.NON_VALUE, this.tokens);
		}
	}
}