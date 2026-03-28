package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;

public class TraditionalForLoopKeyword extends KeywordData {

	public TraditionalForLoopKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		Token.Builder builder = Token.builder().with(word);
		Token label = parser.reader.nextIdentifierAfterWhitespace();
		if (label != null) builder.with(label.initIdentifier(Colors.LABEL, TokenInfo.NON_VALUE));
		Token open = parser.tryOpenGroup();
		if (open != null) {
			builder.with(open);
			parser.environment.addLoopLabel(label != null ? label.getIdentifierText().toString() : null);

			Token initializer = parser.nextNullableScript();
			if (initializer == null) initializer = parser.error("Expected initializer");
			builder.with(initializer);

			Token firstComma = parser.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR);
			if (firstComma == null) firstComma = parser.error("Expected ','");
			builder.with(firstComma);

			Token condition = parser.nextNullableScript();
			if (condition == null) condition = parser.error("Expected condition");
			builder.with(condition);

			Token secondComma = parser.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR);
			if (secondComma == null) secondComma = parser.error("Expected ','");
			builder.with(secondComma);

			Token updater = parser.nextNullableScript();
			if (updater == null) updater = parser.error("Expected updater");
			builder.with(updater);

			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon == null) colon = parser.abort(ExpressionParser.COLONS, "Expected ':'");
			builder.with(colon);

			Token body = parser.nextScript();
			return builder.with(body).build(parser.reader.input, new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
		}
		else {
			return builder.with(parser.error("Expected '('")).build(parser.reader.input, TokenInfo.ERROR);
		}
	}

	@Override
	public String toString() {
		return "for label (int i = 1, i < 10, ++i: body)";
	}
}