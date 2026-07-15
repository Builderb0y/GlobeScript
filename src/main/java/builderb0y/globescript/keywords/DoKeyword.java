package builderb0y.globescript.keywords;

import java.util.Set;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.util.Util;

public class DoKeyword extends KeywordData {

	public DoKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	public static final Set<CharSequence> WHILE_UNTIL = Util.charSequenceSet("while", "until");

	@Override
	public Token handle(ExpressionParser parser, Token do_) {
		this.applyColor(do_).withInfo(TokenInfo.NON_VALUE);
		Token while_ = parser.reader.nextIdentifierAfterWhitespace();
		if (while_ == null) {
			while_ = parser.error("Expected 'while' or 'until' after 'do'");
		}
		else if (!WHILE_UNTIL.contains(while_.getIdentifierText())) {
			while_.error("Expected 'while' or 'until' after 'do'");
		}
		this.applyColor(while_).withInfo(TokenInfo.NON_VALUE);
		Token.Builder builder = Token.builder().with(do_).with(while_);
		TokenInfo info = WhileOrRepeatKeyword.finish(parser, parser.environment.standardTypes.boolean_, builder);
		return builder.build(parser.reader.input, info);
	}

	@Override
	public String toString() {
		return this.name + " <while|until> (condition: body)";
	}
}