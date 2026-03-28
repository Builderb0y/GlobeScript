package builderb0y.globescript.datadriven;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ObjectArrays;
import com.intellij.json.psi.JsonNullLiteral;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ExpressionReader;
import builderb0y.globescript.GlobeScriptAnnotator;
import builderb0y.globescript.JsonExpressionReader;
import builderb0y.globescript.StructureParser.Structure;
import builderb0y.globescript.datadriven.PendingEnvironment.PendingTypeReference;

public abstract class PendingElement {

	public PsiElement element;

	public PendingElement() {}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PendingElement(PendingDataContext context) {
		for (FieldInjector injector : this.getFields().array) {
			injector.inject(this, context, null); //initialize default values for fields.
		}
	}

	public PendingElement(PendingDataContext context, PsiElement element) {
		this.element = element;
		this.injectAll(context, element);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void injectAll(PendingDataContext context, PsiElement element) {
		if (element instanceof JsonStringLiteral literal) {
			ShorthandParser shorthandParser = this.getShorthandParser();
			if (shorthandParser != null) {
				for (FieldInjector injector : this.getFields().array) {
					injector.inject(this, context, null); //initialize default values for fields.
				}
				Structure shorthandStructure = shorthandParser.tryParse(context, literal);
				if (shorthandStructure != null) shorthandParser.inject(this, context, literal, shorthandStructure);
			}
			else {
				context.addError(element, "String-based shorthand not supported for this element");
			}
		}
		else if (element instanceof JsonObject object) {
			FieldInjectorMap fields = this.getFields();
			Set<String> seen = new ObjectOpenHashSet<>(fields.array.length);
			for (JsonProperty property : object.getPropertyList()) {
				FieldInjector field = fields.map.get(property.getName());
				if (field != null) {
					field.inject(this, context, property.getValue());
					seen.add(field.name);
				}
				else {
					context.addError(property.getNameElement(), "Expected one of: " + fields.map.keySet());
				}
			}
			for (FieldInjector field : fields.array) {
				if (!seen.contains(field.name)) {
					if (field.required) {
						context.addError(object, "Missing " + field.name);
					}
					field.inject(this, context, null);
				}
			}
		}
		else {
			context.addError(element, this.getShorthandParser() != null ? "Expected JsonObject or JsonStringLiteral" : "Expected JsonObject");
		}
	}

	public FieldInjectorMap getFields() {
		return FieldInjectorMap.EMPTY;
	}

	public ShorthandParser<?, ?> getShorthandParser() {
		return null;
	}

	public static interface ShorthandParser<T_Self, T_Structure extends Structure> {

		public abstract T_Structure parse(ExpressionReader reader);

		public default T_Structure parseAndCheckEOF(ExpressionReader reader) {
			T_Structure structure = this.parse(reader);
			if (reader.canReadAfterWhitespace()) {
				throw new RuntimeException("Unexpected trailing characters: " + reader.input.subSequence(reader.cursor, reader.bufferEnd));
			}
			return structure;
		}

		public default T_Structure tryParse(PendingDataContext context, PsiElement element, ExpressionReader reader) {
			try {
				return this.parseAndCheckEOF(reader);
			}
			catch (RuntimeException exception) {
				context.addError(element, exception);
				return null;
			}
		}

		public default T_Structure tryParse(PendingDataContext context, JsonStringLiteral element) {
			String text = element.getContainingFile().getText();
			TextRange range = element.getTextRange();
			return this.tryParse(
				context,
				element,
				new JsonExpressionReader(
					text,
					GlobeScriptAnnotator.maybeIncrement(text, range.getStartOffset()),
					GlobeScriptAnnotator.maybeDecrement(text, range.getEndOffset())
				)
			);
		}

		public abstract void inject(T_Self self, PendingDataContext context, JsonStringLiteral element, T_Structure shorthand);
	}

	public static interface Named {

		public static final FieldInjector<Named>
			INJECTOR = new FieldInjector<>("name", true) {

				@Override
				public void inject(Named self, PendingDataContext context, @Nullable PsiElement value) {
					self.name(context.expectString(value));
				}
			},
			NULLABLE_INJECTOR = new FieldInjector<>("name", true) {

				@Override
				public void inject(Named self, PendingDataContext context, @Nullable PsiElement value) {
					self.name(value instanceof JsonNullLiteral ? null : context.expectString(value));
				}
			};

		public abstract String name();

		public abstract void name(String name);
	}

	public static interface Typed {

		public static final FieldInjector<Typed> INJECTOR = new FieldInjector<>("type", true) {

			@Override
			public void inject(Typed self, PendingDataContext context, @Nullable PsiElement value) {
				self.type(value == null ? null : new PendingTypeReference(context, value));
			}
		};

		public abstract PendingTypeReference type();

		public abstract void type(PendingTypeReference type);
	}

	public static interface Colored {

		public static final FieldInjector<Colored> INJECTOR = new FieldInjector<>("color", false) {

			@Override
			public void inject(Colored self, PendingDataContext context, @Nullable PsiElement value) {
				self.color(context.expectColor(value, self.defaultColor()));
			}
		};

		public abstract TextAttributesKey color();

		public abstract void color(TextAttributesKey color);

		public abstract TextAttributesKey defaultColor();
	}

	public static interface Owned {

		public static final FieldInjector<Owned> INJECTOR = new FieldInjector<>("owner", true) {

			@Override
			public void inject(Owned self, PendingDataContext context, @Nullable PsiElement value) {
				self.owner(value == null ? null : new PendingTypeReference(context, value));
			}
		};

		public abstract PendingTypeReference owner();

		public abstract void owner(PendingTypeReference owner);
	}

	public static class FieldInjectorMap {

		public static final FieldInjectorMap EMPTY = new FieldInjectorMap();

		public final FieldInjector<?>[] array;
		public final Map<String, FieldInjector<?>> map;

		public FieldInjectorMap() {
			this.array = new FieldInjector[0];
			this.map = Collections.emptyMap();
		}

		@SuppressWarnings("unchecked")
		public FieldInjectorMap(FieldInjector<?>... injectors) {
			this.array = injectors;
			this.map = Map.ofEntries(Arrays.stream(injectors).map((FieldInjector<?> injector) -> Map.entry(injector.name, injector)).toArray(Map.Entry[]::new));
		}

		public FieldInjectorMap(FieldInjectorMap parent, FieldInjector<?>... children) {
			this(ObjectArrays.concat(parent.array, children, FieldInjector.class));
		}

		public FieldInjectorMap replace(FieldInjector<?> from, FieldInjector<?> to) {
			for (int index = 0; index < this.array.length; index++) {
				if (this.array[index] == from) {
					FieldInjector<?>[] newArray = this.array.clone();
					newArray[index] = to;
					return new FieldInjectorMap(newArray);
				}
			}
			throw new IllegalArgumentException(from.name + " not present in FieldInjectorMap");
		}
	}

	public static abstract class FieldInjector<T_Pending> {

		public final String name;
		public final boolean required;

		public FieldInjector(String name, boolean required) {
			this.name = name;
			this.required = required;
		}

		public abstract void inject(T_Pending self, PendingDataContext context, @Nullable PsiElement value);
	}
}