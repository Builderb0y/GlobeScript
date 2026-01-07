package builderb0y.globescript;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

public class Colors {

	public static final TextAttributesKey
		LINE_COMMENT       = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_LINE_COMMENT",       DefaultLanguageHighlighterColors.LINE_COMMENT),
		BLOCK_COMMENT      = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_BLOCK_COMMENT",      DefaultLanguageHighlighterColors.BLOCK_COMMENT),
		SCOPED_COMMENT     = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_SCOPED_COMMENT",     DefaultLanguageHighlighterColors.BLOCK_COMMENT),
		NORMAL_IDENTIFIER  = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_NORMAL_IDENTIFIER",  DefaultLanguageHighlighterColors.IDENTIFIER),
		ESCAPED_IDENTIFIER = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_ESCAPED_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER),
		NUMBER             = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_NUMBER",             DefaultLanguageHighlighterColors.NUMBER),
		STRING             = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_STRING",             DefaultLanguageHighlighterColors.STRING),
		GROUP              = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_GROUP",              DefaultLanguageHighlighterColors.PARENTHESES),
		OPERATOR           = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_OPERATOR",           DefaultLanguageHighlighterColors.OPERATION_SIGN),
		ERROR              = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_ERROR",                             HighlighterColors.BAD_CHARACTER),

		LOCAL              = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_LOCAL",              DefaultLanguageHighlighterColors.LOCAL_VARIABLE),
		GLOBAL             = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_GLOBAL",             DefaultLanguageHighlighterColors.GLOBAL_VARIABLE),
		PARAMETER          = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_PARAMETER",          DefaultLanguageHighlighterColors.PARAMETER),
		FIELD              = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_FIELD",              DefaultLanguageHighlighterColors.INSTANCE_FIELD),
		FUNCTION           = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_FUNCTION",           DefaultLanguageHighlighterColors.STATIC_METHOD),
		METHOD             = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_METHOD",             DefaultLanguageHighlighterColors.INSTANCE_METHOD),
		KEYWORD            = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_KEYWORD",            DefaultLanguageHighlighterColors.KEYWORD),
		TYPE               = TextAttributesKey.createTextAttributesKey("GLOBESCRIPT_TYPE",               DefaultLanguageHighlighterColors.CLASS_NAME);
}