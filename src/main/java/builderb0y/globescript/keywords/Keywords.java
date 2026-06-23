package builderb0y.globescript.keywords;

import java.util.*;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.datadriven.EnvironmentModel.KeywordData;
import builderb0y.globescript.datadriven.EnvironmentModel.MemberKeywordData;
import builderb0y.globescript.datadriven.RawTypeModel;

public class Keywords {

	public static final Map<String, KeywordFactory> KEYWORDS = Map.ofEntries(
		Map.entry("var",                        VarKeyword::new),
		Map.entry("class",                    ClassKeyword::new),
		Map.entry("body_if",                 BodyIfKeyword::new),
		Map.entry("switch",                  SwitchKeyword::new),
		Map.entry("for",                        ForKeyword::new),
		Map.entry("while",                    WhileKeyword::new),
		Map.entry("do",                          DoKeyword::new),
		Map.entry("repeat",                  RepeatKeyword::new),
		Map.entry("block",                    BlockKeyword::new),
		Map.entry("loop_jump",             LoopJumpKeyword::new),
		Map.entry("noscope",                NoscopeKeyword::new),
		Map.entry("tag_constructor", TagConstructorKeyword::new)
	);

	public static final Map<String, MemberKeywordFactory> MEMBER_KEYWORDS = Map.of(
		"receiver_if",     ReceiverIfKeyword::new,
		"random_if",         RandomIfKeyword::new,
		"random_between", NextBetweenKeyword::new,
		"is",                IsMemberKeyword::new,
		"as",                AsMemberKeyword::new,
		"between",      BetweenMemberKeyword::new
	);

	public static interface KeywordFactory {

		public abstract KeywordData create(String name, TextAttributesKey color);
	}

	public static interface MemberKeywordFactory {

		public abstract MemberKeywordData create(String name, TextAttributesKey color, RawTypeModel receiverType);
	}
}