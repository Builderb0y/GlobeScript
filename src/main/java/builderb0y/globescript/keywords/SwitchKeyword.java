package builderb0y.globescript.keywords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ConstantValue.IntegerConstantValue;
import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.StructureParser.*;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;
import builderb0y.globescript.util.RangedBitSet;

public class SwitchKeyword extends KeywordData {

	public SwitchKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public Token handle(ExpressionParser parser, Token word) {
		this.applyColor(word).withInfo(TokenInfo.NON_VALUE);
		Token.Builder builder = Token.builder().with(word);
		Token open = parser.tryOpenGroup();
		if (open != null) {
			Token value;
			builder
			.with(open)
			.with(parser.orError(value = parser.nextNullableScript(), "Expected value to switch on"))
			.with(parser.orError(parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR), "Expected ':'"));
			RawTypeModel target;
			target = parser.environment.standardTypes.int_;
			if (value != null) {
				if (value.info.type().isEnum()) {
					target = value.info.type();
				}
				else if (!value.info.isAssignableToOrCanCast(parser.environment, parser.environment.standardTypes.int_, Plicity.IMPLICIT)) {
					value.withTooltip("Switch value must be an int or an enum");
				}
			}
			MultiStructure<ICaseStructure> cases = new MultiParser<>(false, new CaseParser(target), null).parse(parser);
			DefaultStructure defaultCase = null;
			RawTypeModel finalType;
			if (cases != null) {
				builder.withAll(cases);
				List<TokenInfo> bodyTypes = new ArrayList<>();
				if (target.isEnum()) {
					Set<String> seen = new HashSet<>();
					for (ICaseStructure iCase : cases.values()) {
						switch (iCase) {
							case CaseStructure case_ -> {
								for (CaseMatchStructure match : case_.matches().values()) {
									if (match instanceof SingleCaseMatch(Token enumName)) {
										if (!seen.add(enumName.getIdentifierText().toString())) {
											enumName.withTooltip("Duplicate case");
										}
									}
								}
							}
							case DefaultStructure default_ -> {
								if (defaultCase != null) default_.defaultWord().withTooltip("Duplicate default case");
								else defaultCase = default_;
							}
						}
						if (iCase.body() != null) bodyTypes.add(iCase.body().info);
					}
				}
				else {
					RangedBitSet matchedValues = new RangedBitSet();
					for (ICaseStructure iCase : cases.values()) {
						switch (iCase) {
							case CaseStructure case_ -> {
								if (case_.matches() != null) {
									for (CaseMatchStructure match : case_.matches().values()) {
										switch (match) {
											case SingleCaseMatch(Token number) -> {
												if (number.info.constant() instanceof IntegerConstantValue integer) {
													if (matchedValues.contains(integer.intValue())) {
														number.withTooltip("Duplicate case");
													}
													else {
														matchedValues.addSegment(integer.intValue(), integer.intValue());
													}
												}
											}
											case RangeCaseMatch(Token range, IntervalStructure interval) -> {
												if (interval != null) {
													if (
														interval.lowerBound().info.constant() instanceof IntegerConstantValue lower &&
														interval.upperBound().info.constant() instanceof IntegerConstantValue upper
													) {
														int lowerStart = lower.intValue();
														int upperEnd = upper.intValue();
														if (!interval.lowerBoundInclusive()) lowerStart++;
														if (!interval.upperBoundInclusive()) upperEnd--;
														RangedBitSet intersection = matchedValues.intersection(lowerStart, upperEnd);
														if (intersection != null) {
															range.withTooltip("Duplicate case(s): " + intersection);
														}
														matchedValues.addSegment(lowerStart, upperEnd);
													}
												}
											}
										}
									}
								}
							}
							case DefaultStructure default_ -> {
								if (defaultCase != null) default_.defaultWord().withTooltip("Duplicate default case");
								else defaultCase = default_;
							}
						}
						if (iCase.body() != null) bodyTypes.add(iCase.body().info);
					}
				}
				if (bodyTypes.isEmpty()) {
					finalType = RawTypeModel.ERROR;
				}
				else if (defaultCase == null) {
					finalType = parser.environment.standardTypes.void_;
				}
				else {
					finalType = RawTypeModel.commonAncestor(bodyTypes);
				}
			}
			else {
				builder.with(parser.error("At least one case is required"));
				finalType = RawTypeModel.ERROR;
			}
			return builder.with(parser.closeGroup()).build(parser.reader.input, new TokenInfo(finalType, TokenInfo.FLAG_STATEMENT));
		}
		else {
			return builder.with(parser.error("Expected '('")).build(parser.reader.input, TokenInfo.ERROR);
		}
	}

	@Override
	public String toString() {
		return "switch (value: case(0: body) case(1: body) default(body))";
	}
}