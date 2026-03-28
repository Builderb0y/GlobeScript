package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class VarKeyword extends KeywordData {

	public VarKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token var_) {
		this.applyColor(var_).info = TokenInfo.NON_VALUE;
		Token star = parser.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
		if (star != null) {
			return parser.finishMultiDeclaration(var_, star);
		}
		else {
			Token name = parser.reader.nextIdentifierAfterWhitespace(Colors.LOCAL);
			if (name != null) {
				Token equals = parser.reader.hasOperatorAfterWhitespace(ExpressionParser.DECLARE_ASSIGNMENTS, Colors.OPERATOR);
				if (equals != null) {
					Token value = parser.nextNullableSingleExpression();
					if (value != null) {
						name.withInfo(value.info);
						parser.environment.addUserLocal(name.getIdentifierText().toString(), value.info.assignable(true));
						return new Token(
							parser.reader.input,
							new TokenInfo(
								switch (equals.range.getLength()) {
									case 1 -> parser.environment.standardTypes.void_;
									case 2 -> value.info.type();
									default -> throw new IllegalStateException("Unexpected declare assignment token length: " + equals.getText());
								},
								TokenInfo.FLAG_STATEMENT
							),
							var_,
							name,
							equals,
							value
						);
					}
					else { //value == null
						return new Token(parser.reader.input, TokenInfo.ERROR, var_, name, equals, parser.error("Expected value"));
					}
				}
				else { //equals == null
					return new Token(parser.reader.input, TokenInfo.ERROR, var_, name, parser.error("Expected '=' or ':='"));
				}
			}
			else { //name == null
				return new Token(parser.reader.input, TokenInfo.ERROR, var_, parser.error("Expected '*' or name"));
			}
		}
	}

	@Override
	public String toString() {
		return this.name + " name = value";
	}
}