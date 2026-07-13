package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.json.psi.JsonNullLiteral;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.Colors;
import builderb0y.globescript.ExpressionReader;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.PendingEnvironment.ListBackedStructure;

import static builderb0y.globescript.datadriven.TypeModifiersModel.BY_NAME;

public class PendingType extends PendingElement implements PendingElement.Named {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		Named.INJECTOR,
		new FieldInjector<PendingType>("modifiers", false) {

			@Override
			public void inject(PendingType self, PendingDataContext context, @Nullable PsiElement value) {
				self.modifiers = (
					context
					.expectArray(value, JsonStringLiteral.class, (JsonStringLiteral literal) -> {
						int flag = BY_NAME.getInt(literal.getValue());
						if (flag == 0) context.addError(literal, "Expected one of: " + BY_NAME.keySet());
						return flag;
					})
					.mapToInt(Integer::intValue)
					.reduce(0, (int a, int b) -> a | b)
				);
			}
		},
		new FieldInjector<PendingType>("extends", true) {

			@Override
			public void inject(PendingType self, PendingDataContext context, @Nullable PsiElement value) {
				switch (value) {
					case JsonStringLiteral string -> self.superClass = new PendingSuperType(context, string);
					case JsonNullLiteral null_ -> self.superClass = null;
					case null, default -> context.addError(value, "Expected JsonStringLiteral or JsonNullLiteral");
				}
			}
		},
		new FieldInjector<PendingType>("implements", false) {

			@Override
			public void inject(PendingType self, PendingDataContext context, @Nullable PsiElement value) {
				self.superInterfaces = context.expectArray(value, JsonStringLiteral.class, (JsonStringLiteral literal) -> new PendingSuperType(context, literal)).toArray(PendingSuperType[]::new);
			}
		},
		new FieldInjector<PendingType>("enum_constants", false) {

			@Override
			public void inject(PendingType self, PendingDataContext context, @Nullable PsiElement value) {
				self.enumConstants = context.expectArray(value, PendingDataContext::expectString).collect(Collectors.toSet());
			}
		}
	);

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public int modifiers;
	public String name;
		@Override public String name() { return this.name; }
		@Override public void name(String name) { this.name = name; }
	public PendingSuperType superClass;
	public PendingSuperType[] superInterfaces;
	public Set<String> enumConstants;

	public PendingType(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	@Override
	public ShorthandParser<?, ?> getShorthandParser() {
		return Shorthand.PARSER;
	}

	public static record Shorthand(List<Token> tokens, int modifiers, String name, String superClass, List<String> superInterfaces, Set<String> enumConstants) implements ListBackedStructure {

		public static final ShorthandParser<PendingType, Shorthand> PARSER = new ShorthandParser<>() {

			@Override
			public Shorthand parse(ExpressionReader reader) {
				int modifiers = 0;
				Token name;
				String superClass;
				List<String> superInterfaces = new ArrayList<>(2);
				Token.Builder builder = Token.builder();
				loop:
				while (true) {
					Token word = reader.nextIdentifierAfterWhitespace(Colors.KEYWORD).withInfo(TokenInfo.NON_VALUE);
					builder.with(word);
					switch (word.getIdentifierText().toString()) {
						case "final"     -> { modifiers |= TypeModifiersModel.FINAL;    }
						case "abstract"  -> { modifiers |= TypeModifiersModel.ABSTRACT; }
						case "interface" -> { modifiers |= TypeModifiersModel.INTERFACE_IMPLIES; break loop; }
						case "record"    -> { modifiers |= TypeModifiersModel.RECORD_IMPLIES;    break loop; }
						case "enum"      -> { modifiers |= TypeModifiersModel.ENUM;              break loop; }
						case "class"     -> { break loop; }
						default          -> throw new RuntimeException("Unexpected word '" + word + "'");
					}
				}
				name = reader.nextIdentifierAfterWhitespace(Colors.TYPE).withInfo(TokenInfo.NON_VALUE);
				builder.with(name);
				Token extends_ = reader.hasIdentifierAfterWhitespace("extends", Colors.KEYWORD);
				if (extends_ != null) {
					builder.with(extends_.withInfo(TokenInfo.NON_VALUE));
					Token word = reader.nextIdentifierAfterWhitespace(Colors.TYPE).withInfo(TokenInfo.NON_VALUE);
					builder.with(word);
					superClass = word.getIdentifierText().toString();
				}
				else switch (modifiers & (TypeModifiersModel.INTERFACE_ONLY | TypeModifiersModel.RECORD_ONLY | TypeModifiersModel.ENUM)) {
					case TypeModifiersModel.INTERFACE_ONLY -> superClass = "object";
					case TypeModifiersModel.RECORD_ONLY -> superClass = "record";
					case TypeModifiersModel.ENUM -> superClass = "enum";
					case 0 -> {
						throw new RuntimeException("Must specify extends");
					}
					default -> {
						throw new RuntimeException("Unexpected modifiers: 2x" + Integer.toBinaryString(modifiers));
					}
				}
				Token implements_ = reader.hasIdentifierAfterWhitespace("implements", Colors.KEYWORD);
				if (implements_ != null) {
					builder.with(implements_.withInfo(TokenInfo.NON_VALUE));
					do {
						Token word = reader.nextIdentifierAfterWhitespace(Colors.TYPE).withInfo(TokenInfo.NON_VALUE);
						builder.with(word);
						superInterfaces.add(word.getIdentifierText().toString());
					}
					while (reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, builder));
				}
				Set<String> enumConstants = null;
				if ((modifiers & TypeModifiersModel.ENUM) != 0 && reader.hasAfterWhitespace('(', Colors.GROUP, builder)) {
					enumConstants = new HashSet<>();
					do {
						Token word = reader.nextIdentifierAfterWhitespace(Colors.STATIC_FIELD).withInfo(TokenInfo.NON_VALUE);
						builder.with(word);
						if (!enumConstants.add(word.getIdentifierText().toString())) {
							throw new RuntimeException("Duplicate enum constant: " + word.getIdentifierText());
						}
					}
					while (reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, builder));
					builder.with(reader.hasAfterWhitespace(')', Colors.GROUP));
				}
				return new Shorthand(builder, modifiers, name.getIdentifierText().toString(), superClass, superInterfaces, enumConstants);
			}

			@Override
			public void inject(PendingType self, PendingDataContext context, JsonStringLiteral element, Shorthand shorthand) {
				self.modifiers = shorthand.modifiers;
				self.name = shorthand.name;
				self.superClass = new PendingSuperType(element, shorthand.superClass);
				self.superInterfaces = shorthand.superInterfaces.stream().map((String name) -> new PendingSuperType(element, name)).toArray(PendingSuperType[]::new);
				self.enumConstants = shorthand.enumConstants;
			}
		};
	}
}