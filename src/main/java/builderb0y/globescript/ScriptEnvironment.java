package builderb0y.globescript;

import com.intellij.openapi.editor.colors.TextAttributesKey;

public class ScriptEnvironment {

	public static final ScriptEnvironment BUILTIN = (
		new ScriptEnvironment()
		.addType("boolean")
		.addType("byte")
		.addType("short")
		.addType("int")
		.addType("long")
		.addType("float")
		.addType("double")
		.addType("char")
		.addType("void")

		.addType("Boolean")
		.addType("Byte")
		.addType("Short")
		.addType("Integer")
		.addType("Long")
		.addType("Float")
		.addType("Double")
		.addType("Character")
		.addType("Void")
		.addType("Number")

		.addType("Object")
		.addType("Comparable")
		.addType("String")
		//.addType("Throwable")
		.addType("Class")

		.addType("MinecraftVersion")

		.addGlobal("true")
		.addGlobal("false")
		.addGlobal("yes")
		.addGlobal("no")
		.addGlobal("null")

		.addKeyword("if",     Keywords::parseIf)
		.addKeyword("unless", Keywords::parseIf)
		.addKeyword("while",  Keywords::parseWhile)
		.addKeyword("until",  Keywords::parseWhile)
		.addKeyword("repeat", Keywords::parseWhile)
		.addKeyword("do",     Keywords::parseDo)
		.addKeyword("class",  Keywords::parseClass)
	);

	public StackMap<CharSequence, IdentifierHandler> identifierTypes = StackMap.withFallback(Util.CHAR_SEQUENCE_STRATEGY, IdentifierTypes.UNKNOWN);

	public ScriptEnvironment addLocal(String name) {
		this.identifierTypes.put(name, IdentifierTypes.LOCAL);
		return this;
	}

	public ScriptEnvironment addGlobal(String name) {
		this.identifierTypes.put(name, IdentifierTypes.GLOBAL);
		return this;
	}

	public ScriptEnvironment addParameter(String name) {
		this.identifierTypes.put(name, IdentifierTypes.PARAMETER);
		return this;
	}

	public ScriptEnvironment addField(String name) {
		this.identifierTypes.put(name, IdentifierTypes.FIELD);
		return this;
	}

	public ScriptEnvironment addType(String name) {
		this.identifierTypes.put(name, IdentifierTypes.TYPE);
		return this;
	}

	public ScriptEnvironment addKeyword(String name, Keyword keyword) {
		this.identifierTypes.put(name, keyword);
		return this;
	}

	public ScriptEnvironment addEnvironment(ScriptEnvironment that) {
		this.identifierTypes.putAll(that.identifierTypes);
		return this;
	}

	public void push() {
		this.identifierTypes.push();
	}

	public void pop() {
		this.identifierTypes.pop();
	}

	public static interface IdentifierHandler {

		public abstract TextAttributesKey color();

		public abstract Token handle(ExpressionParser parser, Token word, boolean member);
	}

	public static enum IdentifierTypes implements IdentifierHandler {
		LOCAL(Colors.LOCAL, false),
		GLOBAL(Colors.GLOBAL, false),
		PARAMETER(Colors.PARAMETER, false),
		FIELD(Colors.FIELD, true),
		TYPE(Colors.TYPE, false) {

			@Override
			public Token handle(ExpressionParser parser, Token word, boolean member) {
				return parser.handleTypeIdentifier(word);
			}
		},
		UNKNOWN(Colors.ERROR, false);

		public final TextAttributesKey color;
		public final boolean member;

		IdentifierTypes(TextAttributesKey color, boolean member) {
			this.color = color;
			this.member = member;
		}

		@Override
		public TextAttributesKey color() {
			return this.color;
		}

		@Override
		public Token handle(ExpressionParser parser, Token word, boolean member) {
			word.color = (this.member == member) ? this.color : Colors.ERROR;
			return word;
		}
	}

	public static interface Keyword extends IdentifierHandler {

		@Override
		public default TextAttributesKey color() {
			return Colors.KEYWORD;
		}
	}
}