package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ConstantValue.BooleanConstantValue;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.RawTypeModel;

public class ReceiverIfKeyword extends MemberKeywordData {

	public ReceiverIfKeyword(String name, TextAttributesKey color, RawTypeModel receiverType) {
		super(name, color, receiverType);
	}

	@Override
	public Token handle(ExpressionParser parser, Token condition, Token dot, Token if_) {
		this.applyColor(if_).withInfo(TokenInfo.NON_VALUE);
		Token open = parser.tryOpenGroup();
		if (open != null) {
			Token body = parser.nextNullableScript();
			if (body == null) body = parser.error("Expected body");
			Token close = parser.closeGroup();
			Token else_ = parser.reader.hasIdentifierAfterWhitespace("else", this.color);
			if (else_ != null) {
				Token elseOpen = parser.tryOpenGroup();
				if (elseOpen != null) {
					Token elseBody = parser.nextNullableScript();
					if (elseBody == null) elseBody = parser.error("Expected body");
					Token elseClose = parser.closeGroup();
					int flags = BodyIfKeyword.getFlags(condition, body, elseBody);
					TokenInfo info = new TokenInfo(RawTypeModel.commonAncestor(body.info.type(), elseBody.info.type()), flags);
					return new Token(parser.reader.input, info, condition, dot, if_, open, body, close, else_, elseOpen, elseBody, elseClose);
				}
				else {
					Token elseBody = parser.nextNullableSingleExpression();
					if (elseBody == null) elseBody = parser.error("Expected body");
					int flags = BodyIfKeyword.getFlags(condition, body, elseBody);
					TokenInfo info = new TokenInfo(RawTypeModel.commonAncestor(body.info.type(), elseBody.info.type()), flags);
					return new Token(parser.reader.input, info, condition, dot, if_, open, body, close, else_, elseBody);
				}
			}
			else {
				int flags = TokenInfo.FLAG_STATEMENT;
				if (condition.info.constant() instanceof BooleanConstantValue bool && bool.value() && body.info.jumps()) {
					flags |= TokenInfo.FLAG_JUMPS;
				}
				TokenInfo info = new TokenInfo(parser.environment.standardTypes.void_, flags);
				return new Token(parser.reader.input, info, condition, dot, if_, open, body, close);
			}
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, condition, dot, if_, parser.error("Expected '('"));
		}
	}

	@Override
	public String toString() {
		return "boolean." + this.name + " (trueBody) else (falseBody)";
	}
}