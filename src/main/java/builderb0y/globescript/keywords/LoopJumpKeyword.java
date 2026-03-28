package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class LoopJumpKeyword extends KeywordData {

	public LoopJumpKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		this.applyColor(word).withInfo(TokenInfo.NON_VALUE);

		Token open = parser.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open == null) open = parser.error("Expected '('");

		Token label = parser.reader.nextIdentifierAfterWhitespace();
		if (label != null) label.initIdentifier(Colors.LABEL, TokenInfo.NON_VALUE);

		Token close = parser.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close == null) close = parser.error("Expected ')'");

		if (!parser.environment.hasLoopLabel(label != null ? label.getIdentifierText().toString() : null)) {
			if (label != null) label.withTooltip("No enclosing loop with this name");
			else word.withTooltip("No enclosing loop");
		}

		return new Token(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT | TokenInfo.FLAG_JUMPS), word, open, label, close);
	}

	@Override
	public String toString() {
		return this.name + "(optional label)";
	}
}