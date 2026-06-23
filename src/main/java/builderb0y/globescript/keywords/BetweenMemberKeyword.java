package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.StructureParser.IntervalParser;
import builderb0y.globescript.StructureParser.IntervalStructure;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public class BetweenMemberKeyword extends MemberKeywordData {

	public BetweenMemberKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	@Override
	public Token handle(ExpressionParser parser, Token receiver, Token dot, Token between) {
		this.applyColor(between).withInfo(TokenInfo.NON_VALUE);
		IntervalStructure interval = new IntervalParser().parse(parser);
		if (interval != null) {
			if (!interval.lowerBound().info.isAssignableToOrCanCast(parser.environment, receiver.info.type(), Plicity.IMPLICIT)) {
				interval.lowerBound().withTooltip("Can't implicitly cast " + interval.lowerBound().info.type() + " to " + receiver.info.type());
			}
			if (!interval.upperBound().info.isAssignableToOrCanCast(parser.environment, receiver.info.type(), Plicity.IMPLICIT)) {
				interval.upperBound().withTooltip("Can't implicitly cast " + interval.upperBound().info.type() + " to " + receiver.info.type());
			}
			return Token.builder().with(receiver).with(dot).with(between).with(interval).build(parser.reader.input, new TokenInfo(parser.environment.standardTypes.boolean_));
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, receiver, dot, between, parser.error("Expected '(' or '['"));
		}
	}

	@Override
	public String toString() {
		return "number." + this.name + "[lowerBound, upperBound)";
	}
}