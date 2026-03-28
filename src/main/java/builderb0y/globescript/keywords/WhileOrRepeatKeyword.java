package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public abstract class WhileOrRepeatKeyword extends KeywordData {

	public WhileOrRepeatKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	public abstract RawTypeModel getExpectedConditionType(ExpressionParser parser);

	@Override
	public Token handle(ExpressionParser parser, Token while_) {
		this.applyColor(while_).withInfo(TokenInfo.NON_VALUE);
		Token.Builder builder = Token.builder().with(while_);
		TokenInfo info = finish(parser, this.getExpectedConditionType(parser), builder);
		return builder.build(parser.reader.input, info);
	}

	public static TokenInfo finish(ExpressionParser parser, RawTypeModel expectedConditionType, Token.Builder builder) {
		Token label = parser.reader.nextIdentifierAfterWhitespace();
		if (label != null) label.initIdentifier(Colors.LABEL, TokenInfo.NON_VALUE);
		Token open = parser.tryOpenGroup();
		if (open != null) {
			parser.environment.addLoopLabel(label != null ? label.getIdentifierText().toString() : null);
			Token condition = parser.nextScript();
			if (condition == null) {
				condition = parser.error("Expected condition");
			}
			else if (!condition.info.isAssignableToOrCanCast(parser.environment, expectedConditionType, Plicity.IMPLICIT)) {
				condition.withTooltip("Condition should be a boolean, but it was a " + condition.info.type());
			}
			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon == null) colon = parser.error("Expected ':'");
			Token body = parser.nextScript();
			if (body == null) body = parser.error("Expected body");
			Token close = parser.closeGroup();
			builder.with(label).with(open).with(condition).with(colon).with(body).with(close);
		}
		else {
			builder.with(label).with(parser.error("Expected '('"));
		}
		return new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT);
	}
}