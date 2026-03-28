package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class BlockKeyword extends KeywordData {

	public BlockKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		this.applyColor(word).withInfo(TokenInfo.NON_VALUE);

		Token label = parser.reader.nextIdentifierAfterWhitespace(Colors.LABEL);
		if (label != null) label.withInfo(TokenInfo.NON_VALUE);

		Token open = parser.tryOpenGroup();
		if (open != null) {
			parser.environment.addLoopLabel(label != null ? label.getIdentifierText().toString() : null);
			Token body = parser.nextNullableScript();
			if (body == null) body = parser.error("Expected loop body");
			Token close = parser.closeGroup();
			return new Token(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT), word, label, open, body, close);
		}
		else {
			return new Token(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT), word, label, parser.error("Expected '('"));
		}
	}

	@Override
	public String toString() {
		return "block label (body)";
	}
}