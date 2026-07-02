package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.*;
import builderb0y.globescript.ConstantValue.BooleanConstantValue;
import builderb0y.globescript.ConstantValue.NonConstantValue;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public class BodyIfKeyword extends KeywordData {

	public BodyIfKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token if_) {
		this.applyColor(if_).withInfo(TokenInfo.NON_VALUE);
		Token open = parser.tryOpenGroup();
		if (open != null) {
			Token condition = parser.nextNullableScript();
			if (condition == null) {
				condition = parser.error("Expected condition");
			}
			else if (!condition.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.boolean_, Plicity.IMPLICIT)) {
				condition.withTooltip("Condition should be a boolean, but it was a " + condition.info.type());
			}
			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon == null) colon = parser.error("Expected ':'");
			Token body = parser.nextNullableScript();
			if (body == null) body = parser.error("Expected body");
			Token close = parser.closeGroup();
			Token else_ = parser.reader.hasIdentifierAfterWhitespace("else", this.color);
			if (else_ != null) {
				else_.withInfo(TokenInfo.NON_VALUE);
				Token elseOpen = parser.tryOpenGroup();
				Token elseBody;
				if (elseOpen != null) {
					elseBody = parser.nextNullableScript();
					if (elseBody == null) elseBody = parser.error("Expected body");
					Token elseClose = parser.closeGroup();
					TokenInfo info = new TokenInfo(mergeTypes(condition, body, elseBody), getFlags(condition, body, elseBody) & ~TokenInfo.FLAG_ASSIGNABLE);
					return new Token(parser.reader.input, info, if_, open, condition, colon, body, close, else_, elseOpen, elseBody, elseClose);
				}
				else {
					elseBody = parser.nextNullableSingleExpression();
					if (elseBody == null) elseBody = parser.error("Expected body");
					TokenInfo info = new TokenInfo(mergeTypes(condition, body, elseBody), getFlags(condition, body, elseBody) & ~TokenInfo.FLAG_ASSIGNABLE);
					return new Token(parser.reader.input, info, if_, open, condition, colon, body, close, else_, elseBody);
				}
			}
			else {
				int flags = TokenInfo.FLAG_STATEMENT;
				if (condition.info.constant() instanceof BooleanConstantValue bool && bool.value() && body.info.jumps()) {
					flags |= TokenInfo.FLAG_JUMPS;
				}
				TokenInfo info = new TokenInfo(parser.environment.standardTypes.void_, flags);
				return new Token(parser.reader.input, info, if_, open, condition, colon, body, close);
			}
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, if_, parser.error("Expected '('"));
		}
	}

	public static ConstantValue mergeTypes(Token condition, Token body, Token elseBody) {
		if (condition.info.constant() instanceof BooleanConstantValue bool) {
			return (bool.value() ? body : elseBody).info.constant();
		}
		else if (body.info.constant().equals(elseBody.info.constant())) {
			return body.info.constant();
		}
		else {
			return new NonConstantValue(
				RawTypeModel.commonAncestor(body.info, elseBody.info)
			);
		}
	}

	public static int getFlags(Token condition, Token body, Token elseBody) {
		return (
			condition.info.constant() instanceof BooleanConstantValue bool
			? (bool.value() ? body.info.flags() : elseBody.info.flags())
			: (body.info.flags() & elseBody.info.flags())
		);
	}

	@Override
	public String toString() {
		return this.name + " (condition: trueBody) else (falseBody)";
	}
}