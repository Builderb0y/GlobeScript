package builderb0y.globescript;

import java.util.Map;
import javax.swing.*;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.util.NlsContexts.ConfigurableName;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.Instances.DummySyntaxHighlighter;

public class GlobeScriptColorSettingsPage implements ColorSettingsPage {

	public static final AttributesDescriptor[] ATTRIbUTES = {
		new AttributesDescriptor("Hidden json character", Colors.JSON_HIDDEN),

		new AttributesDescriptor("Line comment",          Colors.LINE_COMMENT),
		new AttributesDescriptor("Block comment",         Colors.BLOCK_COMMENT),
		new AttributesDescriptor("Scoped comment",        Colors.SCOPED_COMMENT),
		new AttributesDescriptor("Number",                Colors.NUMBER),
		new AttributesDescriptor("String",                Colors.STRING),
		new AttributesDescriptor("Group",                 Colors.GROUP),
		new AttributesDescriptor("Operator",              Colors.OPERATOR),
		new AttributesDescriptor("Error",                 Colors.ERROR),

		new AttributesDescriptor("Local variable",        Colors.LOCAL),
		new AttributesDescriptor("Global variable",       Colors.GLOBAL),
		new AttributesDescriptor("Parameter",             Colors.PARAMETER),
		new AttributesDescriptor("Field",                 Colors.INSTANCE_FIELD),
		new AttributesDescriptor("Function",              Colors.FUNCTION),
		new AttributesDescriptor("Method",                Colors.INSTANCE_METHOD),
		new AttributesDescriptor("Keyword",               Colors.KEYWORD),
		new AttributesDescriptor("Type",                  Colors.TYPE),
		new AttributesDescriptor("Label",                 Colors.LABEL),
	};

	@Override
	public @Nullable Icon getIcon() {
		return Instances.ICON;
	}

	@Override
	public @NotNull SyntaxHighlighter getHighlighter() {
		return new DummySyntaxHighlighter();
	}

	@Override
	public @NonNls @NotNull String getDemoText() {
		return """
		<JSON_HIDDEN>"</JSON_HIDDEN>hidden JSON text<JSON_HIDDEN>",</JSON_HIDDEN>
		<LINE_COMMENT>;line comment</LINE_COMMENT>
		<BLOCK_COMMENT>;;block comment;;</BLOCK_COMMENT>
		<SCOPED_COMMENT>;(scoped comment)</SCOPED_COMMENT>

		<KEYWORD>class</KEYWORD> <TYPE>Box</TYPE><GROUP>(</GROUP><TYPE>int</TYPE> <FIELD>value</FIELD><GROUP>)</GROUP>
		<TYPE>Box</TYPE> <LOCAL>box</LOCAL> <OPERATOR>=</OPERATOR> <FUNCTION>new</FUNCTION><GROUP>(</GROUP><NUMBER>42</NUMBER><GROUP>)</GROUP>
		<TYPE>void</TYPE> <TYPE>Box</TYPE><OPERATOR>.</OPERATOR><METHOD>add</METHOD><GROUP>(</GROUP><TYPE>int</TYPE> <PARAMETER>value</PARAMETER><OPERATOR>:</OPERATOR>
			<KEYWORD>while</KEYWORD> <LABEL>working</LABEL> <GROUP>(</GROUP><GLOBAL>true</GLOBAL><OPERATOR>:</OPERATOR> <FUNCTION>print</FUNCTION><GROUP>(</GROUP><STRING>'string'</STRING><GROUP>)</GROUP><GROUP>)</GROUP>
			<PARAMETER>this</PARAMETER><OPERATOR>.</OPERATOR><FIELD>value</FIELD> <OPERATOR>+=</OPERATOR> <PARAMETER>value</PARAMETER>
		<GROUP>)</GROUP>
		""";
	}

	@Override
	public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
		return Map.ofEntries(
			Map.entry("JSON_HIDDEN",    Colors.JSON_HIDDEN   ),
			Map.entry("LINE_COMMENT",   Colors.LINE_COMMENT  ),
			Map.entry("BLOCK_COMMENT",  Colors.BLOCK_COMMENT ),
			Map.entry("SCOPED_COMMENT", Colors.SCOPED_COMMENT),
			Map.entry("NUMBER",         Colors.NUMBER        ),
			Map.entry("STRING",         Colors.STRING        ),
			Map.entry("GROUP",          Colors.GROUP         ),
			Map.entry("OPERATOR",       Colors.OPERATOR      ),
			Map.entry("ERROR",          Colors.ERROR         ),
			Map.entry("LOCAL",          Colors.LOCAL         ),
			Map.entry("GLOBAL",         Colors.GLOBAL        ),
			Map.entry("PARAMETER",      Colors.PARAMETER     ),
			Map.entry("FIELD",          Colors.INSTANCE_FIELD),
			Map.entry("FUNCTION",       Colors.FUNCTION      ),
			Map.entry("METHOD",         Colors.INSTANCE_METHOD),
			Map.entry("KEYWORD",        Colors.KEYWORD       ),
			Map.entry("TYPE",           Colors.TYPE          ),
			Map.entry("LABEL",          Colors.LABEL)
		);
	}

	@Override
	public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
		return ATTRIbUTES;
	}

	@Override
	public ColorDescriptor @NotNull [] getColorDescriptors() {
		return ColorDescriptor.EMPTY_ARRAY;
	}

	@Override
	public @ConfigurableName @NotNull String getDisplayName() {
		return "Globe Script";
	}
}