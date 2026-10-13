package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.RawTypeModel;

public class IsMemberKeyword extends MemberKeywordData {

	public IsMemberKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	@Override
	public Token handle(ExpressionParser parser, Token receiver, Token dot, Token is) {
		this.applyColor(is).withInfo(TokenInfo.NON_VALUE);
		Token.Builder builder = Token.builder();
		builder.with(receiver).with(dot).with(is);
		Token open = parser.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open != null) {
			builder.with(open);
			Token type = parser.reader.nextIdentifierAfterWhitespace();
			if (type != null) {
				builder.with(type);
				TypeData typeData = parser.environment.getType(type.getIdentifierText().toString());
				if (typeData != null) {
					typeData.applyColor(type).info = typeData.info;
				}
				else {
					type.withColor(Colors.ERROR).info = TokenInfo.ERROR;
				}
				Token name = parser.reader.nextIdentifierAfterWhitespace();
				if (name != null) {
					builder.with(name);
					name.initIdentifier(Colors.LOCAL, type.info);
					parser.environment.addUserLocal(name.getIdentifierText().toString(), type.info);
				}
			}
			else {
				type = parser.error("Expected type");
				builder.with(type);
			}
			Token close = parser.closeUnscopedGroup();
			return builder.with(close).build(parser.reader.input, new TokenInfo(parser.environment.standardTypes.boolean_));
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, receiver, dot, is, parser.error("Expected '('"));
		}
	}

	@Override
	public String toString() {
		return "object." + this.name + "(Type [optional name])";
	}
}