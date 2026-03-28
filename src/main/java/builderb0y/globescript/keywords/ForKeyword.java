package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class ForKeyword extends KeywordData {

	public final EnhancedForLoopKeyword enhanced;
	public final TraditionalForLoopKeyword traditional;

	public ForKeyword(String name, TextAttributesKey color) {
		super(name, color);
		this.enhanced = new EnhancedForLoopKeyword(name, color);
		this.traditional = new TraditionalForLoopKeyword(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		this.applyColor(word).withInfo(TokenInfo.NON_VALUE);
		Token handled = this.enhanced.handle(parser, word);
		if (handled == null) handled = this.traditional.handle(parser, word);
		return handled;
	}

	@Override
	public String toString() {
		return this.enhanced + " OR " + this.traditional;
	}
}