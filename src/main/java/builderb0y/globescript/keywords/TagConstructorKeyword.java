package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.*;
import builderb0y.globescript.StructureParser.GroupParser;
import builderb0y.globescript.StructureParser.GroupStructure;
import builderb0y.globescript.StructureParser.MultiParser;
import builderb0y.globescript.StructureParser.MultiStructure;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.Plicity;

public class TagConstructorKeyword extends KeywordData {

	public TagConstructorKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		int revert = parser.reader.cursor;
		Token question = parser.reader.hasOperatorAfterWhitespace("?", Colors.OPERATOR);
		GroupStructure<MultiStructure<Token>> arguments = new GroupParser<>(
			true,
			new MultiParser<>(
				true,
				StructureParser
				.NULLABLE_SCRIPT
				.then((ExpressionParser p, Token token) -> {
					if (!token.info.isAssignableToOrCanCast(p.environment, p.environment.standardTypes.string, Plicity.IMPLICIT)) {
						token.withTooltip("Can't implicitly cast " + token.info.type() + " to String for tag constructor");
					}
				}),
				StructureParser.operator(",")
			)
		)
		.parse(parser);
		if (arguments != null) {
			TypeData tagType = parser.environment.getType(this.name);
			if (tagType != null) {
				this.applyColor(word).withInfo(tagType.info);
			}
			else {
				word.error("Environment is missing type " + this.name);
			}
			return Token.builder().with(word).with(question).withAll(arguments).build(parser.reader.input, tagType != null ? tagType.info : TokenInfo.ERROR);
		}
		else {
			parser.reader.cursor = revert;
			return null;
		}
	}

	@Override
	public String toString() {
		return this.name + "(contents)";
	}
}