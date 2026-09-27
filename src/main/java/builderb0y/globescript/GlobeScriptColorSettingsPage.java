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
		new AttributesDescriptor("Instance field",        Colors.INSTANCE_FIELD),
		new AttributesDescriptor("Static field",          Colors.STATIC_FIELD),
		new AttributesDescriptor("Function",              Colors.FUNCTION),
		new AttributesDescriptor("Instance method",       Colors.INSTANCE_METHOD),
		new AttributesDescriptor("Static method",         Colors.STATIC_METHOD),
		new AttributesDescriptor("Property",              Colors.PROPERTY),
		new AttributesDescriptor("Keyword",               Colors.KEYWORD),
		new AttributesDescriptor("Type",                  Colors.TYPE),
		new AttributesDescriptor("Label",                 Colors.LABEL),
	};

	public GlobeScriptColorSettingsPage() {
		/*
		String text = """
		;line comment
		;;block comment;;
		;(scoped comment)

		class Box(int instanceField)
		Box box = new(42)
		void Box.instanceMethod(int parameter:
			while label (global.property:
				function('string')
			)
			this.instanceField += Type.staticField
			Type.staticMethod()
		)
		""";
		StandardTypes types = new StandardTypes() {

			@Override
			public RawTypeModel get(String name) {
				return new RawTypeModel(0, name, null, RawTypeModel.EMPTY_ARRAY, null);
			}
		};
		ExpressionParser parser = new ExpressionParser(
			new ExpressionReader(text, 0, text.length()),
			new ScriptEnvironment(types, null, new EnvironmentConfigurator("dummy") {

				@Override
				public void configure(PsiElement source, EnvironmentModel environment) {
					environment.addKeyword(new ClassKeyword("class", Colors.KEYWORD));
					environment.addKeyword(new WhileKeyword("while", Colors.KEYWORD));
					environment.addType(new TypeData("void", Colors.TYPE, new TokenInfo(types.void_)));
					environment.addType(new TypeData("int", Colors.TYPE, new TokenInfo(types.int_)));
					RawTypeModel global = new RawTypeModel(0, "Global", null, RawTypeModel.EMPTY_ARRAY, null);
					environment.addVariable(new VariableData("global", Colors.GLOBAL, new TokenInfo(global)));
					environment.addInstanceField(new FieldData(global, "property", Colors.PROPERTY, new TokenInfo(types.boolean_)));
					environment.addFunction(new FunctionData("function", Colors.FUNCTION, new TokenInfo(types.void_), new ParameterModel("message", types.string, false)));
					environment.addType(new TypeData("Type", Colors.TYPE, new TokenInfo(global)));
					environment.addStaticField(new FieldData(global, "staticField", Colors.STATIC_FIELD, new TokenInfo(types.int_)));
					environment.addStaticMethod(new MethodData("staticMethod", Colors.STATIC_METHOD, global, new TokenInfo(types.void_)));
				}
			})
		);
		StringBuilder message = new StringBuilder();
		Token tree = parser.nextScript();
		Token prev = null;
		for (Token token : Stream.concat(parser.reader.comments.stream(), tree.streamLeaves()).toArray(Token[]::new)) {
			for (PsiErrorDisplay tooltip : token.tooltips) {
				System.out.println(token.getText() + ": " + tooltip.text);
			}
			if (prev != null) message.append(text, prev.range.getEndOffset(), token.range.getStartOffset());
			String colorName = token.color.getExternalName().substring("GLOBESCRIPT_".length());
			message.append('<').append(colorName).append('>').append(token.getText()).append("</").append(colorName).append('>');
			prev = token;
		}
		System.out.println(message);
		//*/
	}

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
		<LINE_COMMENT>;line comment</LINE_COMMENT>
		<BLOCK_COMMENT>;;block comment;;</BLOCK_COMMENT>
		<SCOPED_COMMENT>;(scoped comment)</SCOPED_COMMENT>

		<KEYWORD>class</KEYWORD> <TYPE>Box</TYPE><GROUP>(</GROUP><TYPE>int</TYPE> <INSTANCE_FIELD>instanceField</INSTANCE_FIELD><GROUP>)</GROUP>
		<TYPE>Box</TYPE> <LOCAL>box</LOCAL> <OPERATOR>=</OPERATOR> <KEYWORD>new</KEYWORD><GROUP>(</GROUP><NUMBER>42</NUMBER><GROUP>)</GROUP>
		<TYPE>void</TYPE> <TYPE>Box</TYPE><OPERATOR>.</OPERATOR><INSTANCE_METHOD>instanceMethod</INSTANCE_METHOD><GROUP>(</GROUP><TYPE>int</TYPE> <PARAMETER>parameter</PARAMETER><OPERATOR>:</OPERATOR>
			<KEYWORD>while</KEYWORD> <LABEL>label</LABEL> <GROUP>(</GROUP><GLOBAL>global</GLOBAL><OPERATOR>.</OPERATOR><PROPERTY>property</PROPERTY><OPERATOR>:</OPERATOR>
				<FUNCTION>function</FUNCTION><GROUP>(</GROUP><STRING>'string'</STRING><GROUP>)</GROUP>
			<GROUP>)</GROUP>
			<KEYWORD>this</KEYWORD><OPERATOR>.</OPERATOR><INSTANCE_FIELD>instanceField</INSTANCE_FIELD> <OPERATOR>+=</OPERATOR> <TYPE>Type</TYPE><OPERATOR>.</OPERATOR><STATIC_FIELD>staticField</STATIC_FIELD>
			<TYPE>Type</TYPE><OPERATOR>.</OPERATOR><STATIC_METHOD>staticMethod</STATIC_METHOD><GROUP>(</GROUP><GROUP>)</GROUP>
		<GROUP>)</GROUP>
		""";
	}

	@Override
	public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
		return Map.ofEntries(
			Map.entry("JSON_HIDDEN",     Colors.JSON_HIDDEN    ),
			Map.entry("LINE_COMMENT",    Colors.LINE_COMMENT   ),
			Map.entry("BLOCK_COMMENT",   Colors.BLOCK_COMMENT  ),
			Map.entry("SCOPED_COMMENT",  Colors.SCOPED_COMMENT ),
			Map.entry("NUMBER",          Colors.NUMBER         ),
			Map.entry("STRING",          Colors.STRING         ),
			Map.entry("GROUP",           Colors.GROUP          ),
			Map.entry("OPERATOR",        Colors.OPERATOR       ),
			Map.entry("ERROR",           Colors.ERROR          ),
			Map.entry("LOCAL",           Colors.LOCAL          ),
			Map.entry("GLOBAL",          Colors.GLOBAL         ),
			Map.entry("PARAMETER",       Colors.PARAMETER      ),
			Map.entry("INSTANCE_FIELD",  Colors.INSTANCE_FIELD ),
			Map.entry("STATIC_FIELD",    Colors.STATIC_FIELD   ),
			Map.entry("FUNCTION",        Colors.FUNCTION       ),
			Map.entry("INSTANCE_METHOD", Colors.INSTANCE_METHOD),
			Map.entry("STATIC_METHOD",   Colors.STATIC_METHOD  ),
			Map.entry("KEYWORD",         Colors.KEYWORD        ),
			Map.entry("PROPERTY",        Colors.PROPERTY       ),
			Map.entry("TYPE",            Colors.TYPE           ),
			Map.entry("LABEL",           Colors.LABEL          )
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