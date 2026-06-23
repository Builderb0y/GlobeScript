package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.RawTypeModel;

public abstract class IsOrAsMemberKeyword extends MemberKeywordData {

	public IsOrAsMemberKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	public abstract TokenInfo getType(ExpressionParser parser, Token type);

	@Override
	public Token handle(ExpressionParser parser, Token receiver, Token dot, Token is) {
		this.applyColor(is).withInfo(TokenInfo.NON_VALUE);
		Token open = parser.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open != null) {
			Token type = parser.reader.nextIdentifierAfterWhitespace();
			if (type != null) {
				TypeData typeData = parser.environment.getType(type.getIdentifierText().toString());
				if (typeData != null) {
					typeData.applyColor(type).info = typeData.info;
				}
				else {
					type.withColor(Colors.ERROR).info = TokenInfo.ERROR;
				}
			}
			else {
				type = parser.error("Expected type");
			}
			Token close = parser.closeUnscopedGroup();
			return new Token(
				parser.reader.input,
				this.getType(parser, type),
				receiver, dot, is, open, type, close
			);
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, receiver, dot, is, parser.error("Expected '('"));
		}
	}

	@Override
	public String toString() {
		return "object." + this.name + "(Type)";
	}
}