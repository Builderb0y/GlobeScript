package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ConstantValue.NumericPrecision;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.StructureParser.IntervalParser;
import builderb0y.globescript.StructureParser.IntervalStructure;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.RawTypeModel;

public class NextBetweenKeyword extends MemberKeywordData {

	public NextBetweenKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	@Override
	public Token handle(ExpressionParser parser, Token receiver, Token dot, Token word) {
		this.applyColor(word).withInfo(TokenInfo.NON_VALUE);
		IntervalStructure interval = new IntervalParser().parse(parser);
		if (interval != null) {
			NumericPrecision precision1 = NumericPrecision.from(interval.lowerBound().info.type());
			NumericPrecision precision2 = NumericPrecision.from(interval.upperBound().info.type());
			if (precision1 == null) interval.lowerBound().withTooltip("Lower bound must be numeric.");
			if (precision2 == null) interval.upperBound().withTooltip("Upper bound must be numeric.");
			RawTypeModel type;
			if (precision1 != null && precision2 != null) {
				type = NumericPrecision.max(precision1, precision2).toType(parser.environment);
			}
			else {
				type = RawTypeModel.ERROR;
			}
			return new Token(parser.reader.input, new TokenInfo(type), receiver, dot, word, interval.group());
		}
		else {
			return new Token(parser.reader.input, TokenInfo.ERROR, word, parser.error("Expected '[' or '('"));
		}
	}

	@Override
	public String toString() {
		return "random.nextBetween[min, max)";
	}
}