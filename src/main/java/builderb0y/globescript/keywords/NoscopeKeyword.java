package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class NoscopeKeyword extends KeywordData {

	public NoscopeKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token noscope) {
		Token open = parser.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open != null) {
			Token script = parser.nextNullableScript();
			if (script == null) script = parser.error("Expected script");
			Token close = parser.closeUnscopedGroup();
			return new Token(parser.reader.input, script.info, noscope, open, script, close);
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, noscope, parser.error("Expected '('"));
		}
	}

	@Override
	public String toString() {
		return this.name + "(script)";
	}
}