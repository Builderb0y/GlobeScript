package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.*;
import builderb0y.globescript.ConstantValue.NonConstantValue;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public class RandomIfKeyword extends MemberKeywordData {

	public RandomIfKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	@Override
	public Token handle(ExpressionParser parser, Token random, Token dot, Token if_) {
		this.applyColor(if_).withInfo(TokenInfo.NON_VALUE);
		Token.Builder builder = Token.builder().with(random).with(dot).with(if_);
		Token open = parser.tryOpenGroup();
		if (open != null) {
			builder.with(open);
			Token definitelyBody;
			Token chanceOrBody = parser.nextNullableScript();
			if (chanceOrBody == null) {
				chanceOrBody = parser.error("Expected chance or body");
			}
			builder.with(chanceOrBody);
			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon != null) {
				builder.with(colon);
				if (!chanceOrBody.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.primitiveFloatingPoint, Plicity.IMPLICIT)) {
					chanceOrBody.withTooltip("Chance must be a float or double");
				}
				Token body = parser.nextNullableScript();
				if (body == null) {
					body = parser.error("Expected body");
				}
				builder.with(body);
				definitelyBody = body;
			}
			else {
				definitelyBody = chanceOrBody;
			}
			Token close = parser.closeGroup();
			builder.with(close);
			Token else_ = parser.reader.hasIdentifierAfterWhitespace("else", this.color);
			if (else_ != null) {
				Token elseOpen = parser.tryOpenGroup();
				Token elseBody;
				if (elseOpen != null) {
					builder.with(elseOpen);
					elseBody = parser.nextNullableScript();
					if (elseBody == null) elseBody = parser.error("Expected body");
					builder.with(elseBody);
					Token elseClose = parser.closeGroup();
					builder.with(elseClose);
				}
				else {
					elseBody = parser.nextNullableSingleExpression();
					if (elseBody == null) elseBody = parser.error("Expected body");
					builder.with(elseBody);
				}
				TokenInfo info = new TokenInfo(
					mergeTypes(definitelyBody, elseBody),
					getFlags(definitelyBody, elseBody)
				);
				return builder.build(parser.reader.input, info);
			}
			else {
				TokenInfo info = new TokenInfo(parser.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT);
				return builder.build(parser.reader.input, info);
			}
		}
		else {
			return builder.with(parser.error("Expected '('")).build(parser.reader.input, TokenInfo.ERROR);
		}
	}

	public static ConstantValue mergeTypes(Token body, Token elseBody) {
		if (body.info.constant().equals(elseBody.info.constant())) {
			return body.info.constant();
		}
		else {
			return new NonConstantValue(
				RawTypeModel.commonAncestor(
					body.info,
					elseBody.info
				)
			);
		}
	}

	public static int getFlags(Token body, Token elseBody) {
		int flags = TokenInfo.FLAG_STATEMENT;
		if (body.info.jumps() && elseBody.info.jumps()) {
			flags |= TokenInfo.FLAG_JUMPS;
		}
		if (body.info.generic() && elseBody.info.generic()) {
			flags |= TokenInfo.FLAG_GENERIC;
		}
		return flags;
	}

	@Override
	public String toString() {
		return "random." + this.name + " (optional chance: trueBody) else (falseBody)";
	}
}