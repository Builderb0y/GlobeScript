package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.json.psi.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.*;
import builderb0y.globescript.ConstantValue.*;
import builderb0y.globescript.StructureParser.Structure;
import builderb0y.globescript.datadriven.PendingEnvironment.PendingTypeReference.Shorthand;
import builderb0y.globescript.keywords.Keywords;
import builderb0y.globescript.keywords.Keywords.KeywordFactory;
import builderb0y.globescript.keywords.Keywords.MemberKeywordFactory;
import builderb0y.globescript.datadriven.EnvironmentModel.*;

@SuppressWarnings("DataFlowIssue")
public class PendingEnvironment extends PendingElement implements PendingElement.Named {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		Named.INJECTOR,
		new FieldInjector<PendingEnvironment>("types", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.types = context.expectArray(value, PendingExposedType::new).toArray(PendingExposedType[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("includes", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.includes = context.expectArray(value, PendingEnvironmentReference::new).toArray(PendingEnvironmentReference[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("variables", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.variables = context.expectArray(value, PendingVariable::new).toArray(PendingVariable[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("instance_fields", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.instanceFields = context.expectArray(value, PendingInstanceField::new).toArray(PendingInstanceField[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("static_fields", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.staticFields = context.expectArray(value, PendingStaticField::new).toArray(PendingStaticField[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("functions", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.functions = context.expectArray(value, PendingFunction::new).toArray(PendingFunction[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("instance_methods", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.instanceMethods = context.expectArray(value, PendingInstanceMethod::new).toArray(PendingInstanceMethod[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("static_methods", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.staticMethods = context.expectArray(value, PendingStaticMethod::new).toArray(PendingStaticMethod[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("keywords", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.keywords = context.expectArray(value, PendingKeyword::new).toArray(PendingKeyword[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("instance_keywords", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.instanceKeywords = context.expectArray(value, PendingInstanceKeyword::new).toArray(PendingInstanceKeyword[]::new);
			}
		},
		new FieldInjector<PendingEnvironment>("casters", false) {

			@Override
			public void inject(PendingEnvironment self, PendingDataContext context, @Nullable PsiElement value) {
				self.casters = context.expectArray(value, PendingCaster::new).toArray(PendingCaster[]::new);
			}
		}
	);

	public String name;
		@Override public String name() { return this.name; }
		@Override public void name(String name) { this.name = name; }

	public PendingEnvironmentReference[] includes;

	public PendingExposedType[] types;

	public PendingVariable[] variables;
	public PendingInstanceField[] instanceFields;
	public PendingStaticField[] staticFields;

	public PendingFunction[] functions;
	public PendingInstanceMethod[] instanceMethods;
	public PendingStaticMethod[] staticMethods;

	public PendingKeyword[] keywords;
	public PendingInstanceKeyword[] instanceKeywords;

	public PendingCaster[] casters;

	public PendingEnvironment(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public static class PendingEnvironmentReference extends PendingElement implements Named {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			Named.INJECTOR
		);

		public String name;
			@Override public String name() { return this.name; }
			@Override public void name(String name) { this.name = name; }

		public PendingEnvironmentReference(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		@Override
		public void injectAll(PendingDataContext context, PsiElement element) {
			if (element instanceof JsonStringLiteral string) {
				this.name = string.getValue();
			}
			else {
				super.injectAll(context, element);
			}
		}

		public EnvironmentModel resolve(ConvertingDataContext context) {
			return context.getEnvironment(this.name, this.element);
		}
	}

	public static class PendingTypeReference extends PendingElement implements Named {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			Named.INJECTOR,
			new FieldInjector<PendingTypeReference>("generic", false) {

				@Override
				public void inject(PendingTypeReference self, PendingDataContext context, @Nullable PsiElement value) {
					self.generic = context.expectBoolean(value, false);
				}
			}
		);

		public String name;
			@Override public String name() { return this.name; }
			@Override public void name(String name) { this.name = name; }
		public boolean generic;

		public PendingTypeReference(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public PendingTypeReference(PendingDataContext context, PsiElement element, Shorthand shorthand) {
			super(context);
			this.element = element;
			this.name = shorthand.name;
			this.generic = shorthand.generic;
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public RawTypeModel resolveRaw(ConvertingDataContext context) {
			return context.getType(this.name, this.element);
		}

		public TokenInfo resolve(ConvertingDataContext context, int additionalFlags) {
			if (this.generic) additionalFlags |= TokenInfo.FLAG_GENERIC;
			return new TokenInfo(this.resolveRaw(context), additionalFlags);
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.PARSER;
		}

		public static record Shorthand(List<Token> tokens, String name, boolean generic) implements ListBackedStructure {

			public static final ShorthandParser<PendingTypeReference, Shorthand> PARSER = new ShorthandParser<>() {

				@Override
				public Shorthand parse(ExpressionReader reader) {
					Token first = reader.nextIdentifierAfterWhitespace();
					if ("generic".contentEquals(first.getIdentifierText())) {
						Token second = reader.nextIdentifierAfterWhitespace();
						if (second == null) throw new RuntimeException("Expected name after 'generic'");
						first.initIdentifier(Colors.KEYWORD, TokenInfo.NON_VALUE);
						second.initIdentifier(Colors.TYPE, TokenInfo.NON_VALUE);
						return new Shorthand(List.of(first, second), second.getIdentifierText().toString(), true);
					}
					else {
						first.initIdentifier(Colors.TYPE, TokenInfo.NON_VALUE);
						return new Shorthand(Collections.singletonList(first), first.getIdentifierText().toString(), false);
					}
				}

				@Override
				public void inject(PendingTypeReference self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.name = shorthand.name;
					self.generic = shorthand.generic;
				}
			};
		}
	}

	public static abstract class NamedTypedPendingElement extends PendingElement implements Named, Typed {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			Named.INJECTOR,
			Typed.INJECTOR
		);

		public String name;
			@Override public String name() { return this.name; }
			@Override public void name(String name) { this.name = name; }
		public PendingTypeReference type;
			@Override public PendingTypeReference type() { return this.type; }
			@Override public void type(PendingTypeReference type) { this.type = type; }

		public NamedTypedPendingElement(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public NamedTypedPendingElement(PendingDataContext context) {
			super(context);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}
	}

	public static class PendingExposedType extends NamedTypedPendingElement {

		public PendingExposedType(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public TypeData resolve(ConvertingDataContext context) {
			return new TypeData(this.name, Colors.TYPE, this.type.resolve(context, 0));
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.PARSER;
		}

		public static record Shorthand(PendingTypeReference.Shorthand type) implements Structure {

			public static final ShorthandParser<PendingExposedType, Shorthand> PARSER = new ShorthandParser<>() {

				@Override
				public Shorthand parse(ExpressionReader reader) {
					return new Shorthand(PendingTypeReference.Shorthand.PARSER.parse(reader));
				}

				@Override
				public void inject(PendingExposedType self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.name = shorthand.type.name;
					self.type = new PendingTypeReference(context, element, shorthand.type);
				}
			};

			@Override
			public void addTo(List<Token> list) {
				this.type.addTo(list);
			}

			@Override
			public Token group() {
				return this.type.group();
			}
		}
	}

	public static abstract class PendingMember extends NamedTypedPendingElement implements Colored {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			NamedTypedPendingElement.FIELDS,
			Colored.INJECTOR,
			new FieldInjector<PendingMember>("statement", false) {

				@Override
				public void inject(PendingMember self, PendingDataContext context, @Nullable PsiElement value) {
					self.statement = context.expectBoolean(value, self.defaultStatement());
				}
			},
			new FieldInjector<PendingMember>("jumps", false) {

				@Override
				public void inject(PendingMember self, PendingDataContext context, @Nullable PsiElement value) {
					self.jumps = context.expectBoolean(value, self.defaultJumps());
				}
			},
			new FieldInjector<PendingMember>("assignable", false) {

				@Override
				public void inject(PendingMember self, PendingDataContext context, @Nullable PsiElement value) {
					self.assignable = context.expectBoolean(value, self.defaultAssignable());
				}
			},
			new FieldInjector<PendingMember>("constant", false) {

				@Override
				public void inject(PendingMember self, PendingDataContext context, @Nullable PsiElement value) {
					self.constantElement = value;
				}
			}
		);

		public TextAttributesKey color;
			@Override public TextAttributesKey color() { return this.color; }
			@Override public void color(TextAttributesKey color) { this.color = color; }
		public boolean statement;
		public boolean assignable;
		public boolean jumps;
		public PsiElement constantElement;

		public PendingMember(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public TokenInfo createInfo(ConvertingDataContext context) {
			return this.constantElement != null ? new TokenInfo(this.parseConstant(context), this.typeInfoFlags()) : this.type.resolve(context, this.typeInfoFlags());
		}

		public ConstantValue parseConstant(ConvertingDataContext context) {
			ConstantValue result = null;
			try {
				RawTypeModel type;
				result = switch (this.type.name) {
					case "byte"   -> (type = context.getType("byte",   this.type.element)) != null ? new   ByteConstantValue(type, Byte   .parseByte  (this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "short"  -> (type = context.getType("short",  this.type.element)) != null ? new  ShortConstantValue(type, Short  .parseShort (this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "int"    -> (type = context.getType("int",    this.type.element)) != null ? new    IntConstantValue(type, Integer.parseInt   (this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "long"   -> (type = context.getType("long",   this.type.element)) != null ? new   LongConstantValue(type, Long   .parseLong  (this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "float"  -> (type = context.getType("float",  this.type.element)) != null ? new  FloatConstantValue(type, Float  .parseFloat (this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "double" -> (type = context.getType("double", this.type.element)) != null ? new DoubleConstantValue(type, Double .parseDouble(this.constantElement instanceof JsonStringLiteral string ? string.getValue() : this.constantElement.getText())) : null;
					case "char"   -> {
						type = context.getType("char", this.type.element);
						if (type != null) {
							yield switch (this.constantElement) {
								case JsonStringLiteral string -> {
									String value = string.getValue();
									yield value.length() == 1 ? new CharConstantValue(type, value.charAt(0)) : null;
								}
								case JsonNumberLiteral number -> {
									int value = Integer.parseInt(number.getText());
									yield value == (char)(value) ? new CharConstantValue(type, (char)(value)) : null;
								}
								default -> null;
							};
						}
						else {
							yield null;
						}
					}
					case "string"  -> this.constantElement instanceof JsonStringLiteral string && (type = context.getType("String", this.type.element)) != null ? new StringConstantValue(type, string.getValue()) : null;
					case "boolean" -> this.constantElement instanceof JsonBooleanLiteral bool && (type = context.getType("boolean", this.type.element)) != null ? new BooleanConstantValue(type, bool.getValue()) : null;
					//todo: check for type.isAssignableTo(Object).
					default -> this.constantElement instanceof JsonNullLiteral && (type = this.type.resolveRaw(context)) != null ? new NullConstantValue(type) : null;
				};
				if (result == null) {
					context.pending.addError(this.constantElement, "Can't parse as " + this.type.name);
				}
			}
			catch (Exception exception) {
				context.pending.addError(this.constantElement, "Can't parse as " + this.type.name + ": " + exception);
			}
			return result;
		}

		public int typeInfoFlags() {
			int flags = 0;
			if (this.jumps) flags |= TokenInfo.FLAG_JUMPS;
			if (this.statement) flags |= TokenInfo.FLAG_STATEMENT;
			if (this.assignable) flags |= TokenInfo.FLAG_ASSIGNABLE;
			if (this.type.generic) flags |= TokenInfo.FLAG_GENERIC;
			return flags;
		}

		public boolean defaultStatement() {
			return false;
		}

		public abstract boolean defaultAssignable();

		public boolean defaultJumps() {
			return false;
		}

		public static record MetaShorthand(
			List<Token> tokens,
			@Nullable TextAttributesKey color,
			@Nullable Boolean jumps,
			@Nullable Boolean statement,
			@Nullable Boolean assignable
		)
		implements ListBackedStructure {

			public static final ShorthandParser<PendingMember, MetaShorthand> PARSER = new ShorthandParser<PendingMember, MetaShorthand>() {

				@Override
				public MetaShorthand parse(ExpressionReader reader) {
					Token meta = reader.hasIdentifierAfterWhitespace("meta", Colors.KEYWORD);
					if (meta == null) return null;
					Token.Builder builder = Token.builder().with(meta.withInfo(TokenInfo.NON_VALUE));
					builder.with(reader.hasAfterWhitespace('(', Colors.GROUP).withInfo(TokenInfo.NON_VALUE));

					TextAttributesKey color = null;
					Boolean jumps = null;
					Boolean statement = null;
					Boolean assignable = null;
					if (!reader.hasAfterWhitespace(')')) {
						do {
							Token word = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
							builder.with(word);
							boolean needClose;
							if (reader.hasOperatorAfterWhitespace("=", Colors.OPERATOR, builder)) {
								needClose = false;
							}
							else if (reader.hasAfterWhitespace('(', Colors.GROUP, builder)) {
								needClose = true;
							}
							else {
								throw new RuntimeException("Expected '=' or '(' after '" + word.getIdentifierText() + "'");
							}
							String wordText = word.getIdentifierText().toString();
							switch (wordText) {
								case "color" -> {
									Token colorToken = reader.nextIdentifierAfterWhitespace().withInfo(TokenInfo.NON_VALUE);
									builder.with(colorToken);
									color = PendingDataContext.IDENTIFIER_COLORS.get(colorToken.getIdentifierText().toString());
									if (color == null) throw new RuntimeException("Expected color to be one of: " + PendingDataContext.IDENTIFIER_COLORS);
									colorToken.withColor(color);
								}
								case "jumps" -> {
									Token jumpsToken = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
									builder.with(jumpsToken);
									jumps = this.toBoolean(jumpsToken);
								}
								case "statement" -> {
									Token statementToken = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
									builder.with(statementToken);
									statement = this.toBoolean(statementToken);
								}
								case "assignable" -> {
									Token assignableToken = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
									builder.with(assignableToken);
									assignable = this.toBoolean(assignableToken);
								}
								default -> {
									throw new RuntimeException("Unknown meta tag '" + wordText + "', expected one of: { color, jumps, statement, assignable }");
								}
							}
							if (needClose) {
								builder.with(reader.hasAfterWhitespace(')', Colors.GROUP).withInfo(TokenInfo.NON_VALUE));
							}
						}
						while (reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, builder));
						builder.with(reader.hasAfterWhitespace(')', Colors.GROUP).withInfo(TokenInfo.NON_VALUE));
					}
					return new MetaShorthand(builder, color, jumps, statement, assignable);
				}

				@Override
				public void inject(PendingMember self, PendingDataContext context, JsonStringLiteral element, MetaShorthand shorthand) {
					if (shorthand.color != null) self.color = shorthand.color;
					if (shorthand.jumps != null) self.jumps = shorthand.jumps.booleanValue();
					if (shorthand.statement != null) self.statement = shorthand.statement.booleanValue();
					if (shorthand.assignable != null) self.assignable = shorthand.assignable.booleanValue();
				}

				public Boolean toBoolean(Token token) {
					String text = token.getIdentifierText().toString();
					return switch (text) {
						case "true" -> Boolean.TRUE;
						case "false" -> Boolean.FALSE;
						default -> throw new RuntimeException("Not a boolean: " + text);
					};
				}
			};
		}
	}

	public static class PendingVariable extends PendingMember {

		public PendingVariable(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public VariableData resolve(ConvertingDataContext context) {
			return new VariableData(this.name, this.color, this.createInfo(context));
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.GLOBAL;
		}

		@Override
		public boolean defaultAssignable() {
			return true;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.PARSER;
		}

		public static record Shorthand(List<Token> tokens, MetaShorthand meta, PendingTypeReference.Shorthand type, String name) implements ListBackedStructure {

			public static final ShorthandParser<PendingVariable, Shorthand> PARSER = new ShorthandParser<>() {

				@Override
				public Shorthand parse(ExpressionReader reader) {
					MetaShorthand meta = MetaShorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand type = PendingTypeReference.Shorthand.PARSER.parse(reader);
					Token name = reader.nextIdentifierAfterWhitespace(meta != null && meta.color != null ? meta.color : Colors.GLOBAL).withInfo(TokenInfo.NON_VALUE);
					return new Shorthand(Token.builder().with(meta).withAll(type).with(name), meta, type, name.getIdentifierText().toString());
				}

				@Override
				public void inject(PendingVariable self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.type = new PendingTypeReference(context, element, shorthand.type);
					self.name = shorthand.name;
					if (shorthand.meta != null) MetaShorthand.PARSER.inject(self, context, element, shorthand.meta);
				}
			};
		}
	}

	public static abstract class PendingField extends PendingMember implements Owned {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			PendingVariable.FIELDS.replace(Named.INJECTOR, Named.NULLABLE_INJECTOR),
			Owned.INJECTOR
		);

		public PendingTypeReference owner;
			@Override public PendingTypeReference owner() { return this.owner; }
			@Override public void owner(PendingTypeReference owner) { this.owner = owner; }

		public PendingField(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public FieldData resolve(ConvertingDataContext context) {
			return new FieldData(this.owner.resolveRaw(context), this.name, this.color, this.createInfo(context));
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		@Override
		public boolean defaultAssignable() {
			return true;
		}

		@Override
		public abstract ShorthandParser<?, ?> getShorthandParser();

		public static record Shorthand(List<Token> tokens, MetaShorthand meta, PendingTypeReference.Shorthand type, PendingTypeReference.Shorthand owner, String name) implements ListBackedStructure {

			public static final Parser
				INSTANCE_PARSER = new Parser(false),
				STATIC_PARSER   = new Parser(true);

			public static class Parser implements ShorthandParser<PendingField, Shorthand> {

				public final boolean isStatic;

				public Parser(boolean isStatic) {
					this.isStatic = isStatic;
				}

				@Override
				public Shorthand parse(ExpressionReader reader) {
					MetaShorthand meta = MetaShorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand type = PendingTypeReference.Shorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand owner = PendingTypeReference.Shorthand.PARSER.parse(reader);
					Token dot = reader.hasOperatorAfterWhitespace(".", Colors.OPERATOR).withInfo(TokenInfo.NON_VALUE);
					Token name = reader.nextIdentifierAfterWhitespace(meta != null && meta.color != null ? meta.color : this.isStatic ? Colors.STATIC_FIELD : Colors.INSTANCE_FIELD).withInfo(TokenInfo.NON_VALUE);
					return new Shorthand(Token.builder().with(meta).withAll(type).withAll(owner).with(dot).with(name), meta, type, owner, name.getIdentifierText().toString());
				}

				@Override
				public void inject(PendingField self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.type  = new PendingTypeReference(context, element, shorthand.type);
					self.owner = new PendingTypeReference(context, element, shorthand.owner);
					self.name  = shorthand.name;
					if (shorthand.meta != null) MetaShorthand.PARSER.inject(self, context, element, shorthand.meta);
				}
			}
		}
	}

	public static class PendingInstanceField extends PendingField {

		public PendingInstanceField(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.INSTANCE_FIELD;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.INSTANCE_PARSER;
		}
	}

	public static class PendingStaticField extends PendingField {

		public PendingStaticField(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.STATIC_FIELD;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.STATIC_PARSER;
		}
	}

	public static interface Parameterized {

		public static final FieldInjector<Parameterized> INJECTOR = new FieldInjector<>("parameters", true) {

			@Override
			public void inject(Parameterized self, PendingDataContext context, @Nullable PsiElement value) {
				self.parameters(context.expectArray(value, PendingParameter::new).toArray(PendingParameter[]::new));
			}
		};

		public abstract PendingParameter[] parameters();

		public abstract void parameters(PendingParameter[] parameters);
	}

	public static class PendingParameter extends NamedTypedPendingElement {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			NamedTypedPendingElement.FIELDS,
			new FieldInjector<PendingParameter>("repeatable", false) {

				@Override
				public void inject(PendingParameter self, PendingDataContext context, @Nullable PsiElement value) {
					self.repeatable = context.expectBoolean(value, false);
				}
			}
		);

		public static final PendingParameter[] EMPTY_ARRAY = {};

		public boolean repeatable;

		public PendingParameter(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public PendingParameter(PendingDataContext context, PsiElement element, SingleShorthand shorthand) {
			super(context);
			this.element = element;
			this.name = shorthand.name;
			this.repeatable = shorthand.repeatable;
			this.type = new PendingTypeReference(context, element, shorthand.type);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public static ParameterModel[] resolveAll(ConvertingDataContext context, PendingParameter... parameters) {
			int length = parameters.length;
			ParameterModel[] resolutions = new ParameterModel[length];
			for (int index = 0; index < length; index++) {
				resolutions[index] = parameters[index].resolve(context);
			}
			return resolutions;
		}

		public ParameterModel resolve(ConvertingDataContext context) {
			return new ParameterModel(this.name, this.type.resolveRaw(context), this.repeatable);
		}

		public static record SingleShorthand(PendingTypeReference.Shorthand type, String name, boolean repeatable) {}

		public static record MultiShorthand(List<Token> tokens, List<SingleShorthand> shorthands) implements ListBackedStructure {

			public static final ShorthandParser<Parameterized, MultiShorthand> PARSER = new ShorthandParser<>() {

				@Override
				public MultiShorthand parse(ExpressionReader reader) {
					Token.Builder builder = Token.builder();
					List<SingleShorthand> shorthands = new ArrayList<>(8);
					if (!reader.hasAfterWhitespace('(', Colors.GROUP, builder)) throw new RuntimeException("Missing '('");
					if (!reader.hasAfterWhitespace(')', Colors.GROUP, builder)) {
						do {
							Token.Builder innerBuilder = Token.builder();
							PendingTypeReference.Shorthand type = PendingTypeReference.Shorthand.PARSER.parse(reader);
							innerBuilder.withAll(type);
							Token star = reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
							if (star != null) {
								innerBuilder.with(star).with(reader.hasAfterWhitespace('(', Colors.GROUP).withInfo(TokenInfo.NON_VALUE));
								if (!reader.hasAfterWhitespace(')', Colors.GROUP, builder)) {
									do {
										Token name = reader.nextIdentifierAfterWhitespace(Colors.PARAMETER).withInfo(TokenInfo.NON_VALUE);
										innerBuilder.with(name);
										shorthands.add(new SingleShorthand(type, name.getIdentifierText().toString(), false));
									}
									while (reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, innerBuilder));
									if (!reader.hasAfterWhitespace(')', Colors.GROUP, innerBuilder)) throw new RuntimeException("Missing ')'");
								}
							}
							else {
								boolean repeatable = reader.hasOperatorAfterWhitespace("...", Colors.OPERATOR, innerBuilder);
								Token name = reader.nextIdentifierAfterWhitespace(Colors.PARAMETER).withInfo(TokenInfo.NON_VALUE);
								innerBuilder.with(name);
								shorthands.add(new SingleShorthand(type, name.getIdentifierText().toString(), repeatable));
							}
							builder.with(innerBuilder.build(reader.input, TokenInfo.NON_VALUE));
						}
						while (reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, builder));
						if (!reader.hasAfterWhitespace(')', Colors.GROUP, builder)) throw new RuntimeException("Missing ')'");
					}
					return new MultiShorthand(builder, shorthands);
				}

				@Override
				public void inject(Parameterized self, PendingDataContext context, JsonStringLiteral element, MultiShorthand shorthand) {
					int size = shorthand.shorthands.size();
					PendingParameter[] parameters = new PendingParameter[size];
					for (int index = 0; index < size; index++) {
						parameters[index] = new PendingParameter(context, element, shorthand.shorthands.get(index));
					}
					self.parameters(parameters);
				}
			};
		}
	}

	public static class PendingFunction extends PendingMember implements Parameterized {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			PendingMember.FIELDS,
			Parameterized.INJECTOR
		);

		public PendingParameter[] parameters;
			@Override public PendingParameter[] parameters() { return this.parameters; }
			@Override public void parameters(PendingParameter[] parameters) { this.parameters = parameters; }

		public PendingFunction(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public FunctionData resolve(ConvertingDataContext context) {
			return new FunctionData(this.name, this.color, this.createInfo(context), PendingParameter.resolveAll(context, this.parameters));
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.FUNCTION;
		}

		@Override
		public boolean defaultStatement() {
			return true;
		}

		@Override
		public boolean defaultAssignable() {
			return false;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.PARSER;
		}

		public static record Shorthand(List<Token> tokens, MetaShorthand meta, PendingTypeReference.Shorthand type, String name, PendingParameter.MultiShorthand parameters) implements ListBackedStructure {

			public static final ShorthandParser<PendingFunction, Shorthand> PARSER = new ShorthandParser<>() {

				@Override
				public Shorthand parse(ExpressionReader reader) {
					MetaShorthand meta = MetaShorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand type = PendingTypeReference.Shorthand.PARSER.parse(reader);
					Token name = reader.nextIdentifierAfterWhitespace(meta != null && meta.color != null ? meta.color : Colors.FUNCTION).withInfo(TokenInfo.NON_VALUE);
					PendingParameter.MultiShorthand parameters = PendingParameter.MultiShorthand.PARSER.parse(reader);
					return new Shorthand(Token.builder().with(meta).with(type).with(name).withAll(parameters), meta, type, name.getIdentifierText().toString(), parameters);
				}

				@Override
				public void inject(PendingFunction self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.type = new PendingTypeReference(context, element, shorthand.type);
					self.name = shorthand.name;
					PendingParameter.MultiShorthand.PARSER.inject(self, context, element, shorthand.parameters);
					if (shorthand.meta != null) MetaShorthand.PARSER.inject(self, context, element, shorthand.meta);
				}
			};
		}
	}

	public static abstract class PendingMethod extends PendingMember implements Owned, Parameterized {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			PendingMember.FIELDS,
			Parameterized.INJECTOR,
			Owned.INJECTOR
		);

		public PendingParameter[] parameters;
			@Override public PendingParameter[] parameters() { return this.parameters; }
			@Override public void parameters(PendingParameter[] parameters) { this.parameters = parameters; }
		public PendingTypeReference owner;
			@Override public PendingTypeReference owner() { return this.owner; }
			@Override public void owner(PendingTypeReference owner) { this.owner = owner; }

		public PendingMethod(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public MethodData resolve(ConvertingDataContext context) {
			return new MethodData(this.name, this.color, this.owner.resolveRaw(context), this.createInfo(context), PendingParameter.resolveAll(context, this.parameters));
		}

		@Override
		public boolean defaultAssignable() {
			return false;
		}

		@Override
		public abstract ShorthandParser<?, ?> getShorthandParser();

		public static record Shorthand(List<Token> tokens, MetaShorthand meta, PendingTypeReference.Shorthand type, PendingTypeReference.Shorthand owner, String name, PendingParameter.MultiShorthand parameters) implements ListBackedStructure {

			public static final Parser
				INSTANCE_PARSER = new Parser(false),
				STATIC_PARSER   = new Parser(true);

			public static class Parser implements ShorthandParser<PendingMethod, Shorthand> {

				public final boolean isStatic;

				public Parser(boolean isStatic) {
					this.isStatic = isStatic;
				}

				@Override
				public Shorthand parse(ExpressionReader reader) {
					MetaShorthand meta = MetaShorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand type = PendingTypeReference.Shorthand.PARSER.parse(reader);
					PendingTypeReference.Shorthand owner = PendingTypeReference.Shorthand.PARSER.parse(reader);
					Token dot = reader.hasOperatorAfterWhitespace(".", Colors.OPERATOR).withInfo(TokenInfo.NON_VALUE);
					Token name = reader.nextIdentifierAfterWhitespace(meta != null && meta.color != null ? meta.color : this.isStatic ? Colors.STATIC_METHOD : Colors.INSTANCE_METHOD);
					if (name != null) name.info = TokenInfo.NON_VALUE;
					PendingParameter.MultiShorthand parameters = PendingParameter.MultiShorthand.PARSER.parse(reader);
					return new Shorthand(Token.builder().with(meta).with(type).with(owner).with(dot).with(name).withAll(parameters), meta, type, owner, name != null ? name.getIdentifierText().toString() : "", parameters);
				}

				@Override
				public void inject(PendingMethod self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
					self.type = new PendingTypeReference(context, element, shorthand.type);
					self.owner = new PendingTypeReference(context, element, shorthand.owner);
					self.name = shorthand.name;
					PendingParameter.MultiShorthand.PARSER.inject(self, context, element, shorthand.parameters);
					if (shorthand.meta != null) MetaShorthand.PARSER.inject(self, context, element, shorthand.meta);
				}
			}
		}
	}

	public static class PendingInstanceMethod extends PendingMethod {

		public PendingInstanceMethod(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.INSTANCE_METHOD;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.INSTANCE_PARSER;
		}
	}

	public static class PendingStaticMethod extends PendingMethod {

		public PendingStaticMethod(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.STATIC_METHOD;
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.STATIC_PARSER;
		}
	}

	public static class PendingKeyword extends PendingElement implements Named, Colored {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			Named.INJECTOR,
			Colored.INJECTOR,
			new FieldInjector<PendingKeyword>("keyword", true) {

				@Override
				public void inject(PendingKeyword self, PendingDataContext context, @Nullable PsiElement value) {
					self.keyword = context.expectString(value, Keywords.KEYWORDS::get);
					if (self.keyword == null) context.addError(value, "Unknown keyword");
				}
			}
		);

		public String name;
			@Override public String name() { return this.name; }
			@Override public void name(String name) { this.name = name; }
		public TextAttributesKey color;
			@Override public TextAttributesKey color() { return this.color; }
			@Override public void color(TextAttributesKey color) { this.color = color; }
		public KeywordFactory keyword;

		public PendingKeyword(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public KeywordData resolve(ConvertingDataContext context) {
			return this.keyword.create(this.name, this.color);
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.KEYWORD;
		}
	}

	public static class PendingInstanceKeyword extends PendingElement implements Named, Owned, Colored {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			Named.INJECTOR,
			Owned.INJECTOR,
			Colored.INJECTOR,
			new FieldInjector<PendingInstanceKeyword>("keyword", true) {

				@Override
				public void inject(PendingInstanceKeyword self, PendingDataContext context, @Nullable PsiElement value) {
					self.keyword = context.expectString(value, Keywords.MEMBER_KEYWORDS::get);
					if (self.keyword == null) context.addError(value, "Unknown member keyword type");
				}
			}
		);

		public String name;
			@Override public String name() { return this.name; }
			@Override public void name(String name) { this.name = name; }
		public PendingTypeReference owner;
			@Override public PendingTypeReference owner() { return this.owner; }
			@Override public void owner(PendingTypeReference owner) { this.owner = owner; }
		public TextAttributesKey color;
			@Override public TextAttributesKey color() { return this.color; }
			@Override public void color(TextAttributesKey color) { this.color = color; }
		public MemberKeywordFactory keyword;

		public PendingInstanceKeyword(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public MemberKeywordData resolve(ConvertingDataContext context) {
			return this.keyword.create(this.name, this.color, this.owner.resolveRaw(context));
		}

		@Override
		public TextAttributesKey defaultColor() {
			return Colors.KEYWORD;
		}
	}

	public static class PendingCaster extends PendingElement {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			new FieldInjector<PendingCaster>("from", true) {

				@Override
				public void inject(PendingCaster self, PendingDataContext context, @Nullable PsiElement value) {
					self.from = value == null ? null : new PendingTypeReference(context, value);
				}
			},
			new FieldInjector<PendingCaster>("to", true) {

				@Override
				public void inject(PendingCaster self, PendingDataContext context, @Nullable PsiElement value) {
					self.to = value == null ? null : new PendingTypeReference(context, value);
				}
			},
			new FieldInjector<PendingCaster>("plicity", true) {

				@Override
				public void inject(PendingCaster self, PendingDataContext context, @Nullable PsiElement value) {
					self.plicity = context.expectString(value, (String name) -> switch (name) {
						case "implicit" -> Plicity.IMPLICIT;
						case "explicit" -> Plicity.EXPLICIT;
						default -> {
							context.addError(value, "Expected 'implicit' or 'explicit'");
							yield null;
						}
					});
				}
			}
		);

		public PendingTypeReference from, to;
		public Plicity plicity;

		public PendingCaster(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public CastData resolve(ConvertingDataContext context) {
			return new CastData(this.from.resolveRaw(context), this.to.resolveRaw(context), this.plicity);
		}

		@Override
		public ShorthandParser<?, ?> getShorthandParser() {
			return Shorthand.PARSER;
		}

		public static record Shorthand(List<Token> tokens, Plicity plicity, PendingTypeReference.Shorthand from, PendingTypeReference.Shorthand to) implements ListBackedStructure {

			public static final ShorthandParser<PendingCaster, Shorthand> PARSER = new ShorthandParser<>() {

				@Override
				public Shorthand parse(ExpressionReader reader) {
					Token plicityToken = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
					Plicity plicity = switch (plicityToken.getIdentifierText().toString()) {
						case "implicit" -> Plicity.IMPLICIT;
						case "explicit" -> Plicity.EXPLICIT;
						default -> throw new RuntimeException("Expected 'implicit' or 'explicit' at start");
					};
					PendingTypeReference.Shorthand from = PendingTypeReference.Shorthand.PARSER.parse(reader);
					Token arrow = reader.hasOperatorAfterWhitespace("->", Colors.OPERATOR).withInfo(TokenInfo.NON_VALUE);
					PendingTypeReference.Shorthand to = PendingTypeReference.Shorthand.PARSER.parse(reader);
					return new Shorthand(Token.builder().with(plicityToken).withAll(from).with(arrow).withAll(to), plicity, from, to);
				}

				@Override
				public void inject(PendingCaster self, PendingDataContext context, JsonStringLiteral element, Shorthand structure) {
					self.plicity = structure.plicity;
					self.from = new PendingTypeReference(context, element, structure.from);
					self.to = new PendingTypeReference(context, element, structure.to);
				}
			};
		}
	}

	public static interface ListBackedStructure extends Structure {

		public abstract List<Token> tokens();

		@Override
		public default void addTo(List<Token> list) {
			list.addAll(this.tokens());
		}

		@Override
		public default Token group() {
			return new Token(this.tokens().getFirst().getEntireText(), TokenInfo.NON_VALUE, this.tokens());
		}
	}
}