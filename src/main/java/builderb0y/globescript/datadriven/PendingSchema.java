package builderb0y.globescript.datadriven;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.google.common.base.Predicates;
import com.intellij.json.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingEnvironment.PendingEnvironmentReference;
import builderb0y.globescript.util.Util;

public class PendingSchema extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingSchema>("file_path", true) {

			@Override
			public void inject(PendingSchema self, PendingDataContext context, @Nullable PsiElement value) {
				self.filePath = pattern(context, value);
			}
		},
		new FieldInjector<PendingSchema>("json_path", true) {

			@Override
			public void inject(PendingSchema self, PendingDataContext context, @Nullable PsiElement value) {
				self.jsonPath = JsonPath.parse(context, value);
			}
		},
		new FieldInjector<PendingSchema>("when", false) {

			@Override
			public void inject(PendingSchema self, PendingDataContext context, @Nullable PsiElement value) {
				self.when = When.parse(context, value);
			}
		},
		new FieldInjector<PendingSchema>("environments", true) {

			@Override
			public void inject(PendingSchema self, PendingDataContext context, @Nullable PsiElement value) {
				self.environments = context.expectArray(value, PendingEnvironmentReference::new).toArray(PendingEnvironmentReference[]::new);
			}
		}
	);

	public Pattern filePath;
	public JsonPath jsonPath;
	public When when;
	public PendingEnvironmentReference[] environments;

	public PendingSchema(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public SchemaModel resolve(ConvertingDataContext context) {
		return new SchemaModel(this.filePath, this.jsonPath, this.when, context.getEnvironments(this.environments));
	}

	public static Pattern pattern(PendingDataContext context, PsiElement element) {
		if (element instanceof JsonStringLiteral string) try {
			return Pattern.compile(string.getValue());
		}
		catch (PatternSyntaxException exception) {
			context.addError(element, exception);
		}
		else {
			context.addError(element, "Expected JsonStringLiteral");
		}
		return null;
	}

	public record JsonPath(Predicate<String>[] parts) {

		public static JsonPath parse(PendingDataContext context, @Nullable PsiElement element) {
			if (element instanceof JsonStringLiteral string) {
				String[] parts = string.getValue().split(">");
				@SuppressWarnings("unchecked")
				Predicate<String>[] predicates = new Predicate[parts.length];
				for (int index = 0; index < parts.length; index++) {
					String[] alt = parts[index].split("\\|");
					if (alt.length == 1) {
						if (alt[0].equals("*")) {
							predicates[index] = Predicates.alwaysTrue();
						}
						else {
							predicates[index] = alt[0]::equals;
						}
					}
					else {
						predicates[index] = (String test) -> {
							for (String compare : alt) {
								if (compare.equals(test)) return true;
							}
							return false;
						};
					}
				}
				return new JsonPath(predicates);
			}
			else {
				context.addError(element, "Expected string.");
				return null;
			}
		}

		public @Nullable PsiElement getRootFor(PsiElement self) {
			for (int index = this.parts.length; --index >= 0;) {
				PsiElement parent = self.getParent();
				if (parent instanceof JsonProperty property) {
					if (this.parts[index].test(property.getName())) {
						self = parent.getParent();
					}
					else {
						return null;
					}
				}
				else if (parent instanceof JsonArray array) {
					if (this.parts[index].test(Integer.toString(array.getValueList().indexOf(self)))) {
						self = parent;
					}
					else {
						return null;
					}
				}
				else {
					return null;
				}
			}
			return self;
		}
	}

	public static interface When {

		public abstract boolean test(PsiElement element);

		public static When parse(PendingDataContext context, @Nullable PsiElement element) {
			return switch (element) {
				case JsonStringLiteral string -> {
					yield new StringWhen(string.getValue());
				}
				case JsonBooleanLiteral bool -> {
					yield BooleanWhen.get(bool.getValue());
				}
				case JsonObject object -> {
					record Entry(String key, When value) {}
					yield new CompoundWhen(
						object
						.getPropertyList()
						.stream()
						.map((JsonProperty property) -> new Entry(
							property.getName(),
							parse(context, property.getValue())
						))
						.filter((Entry entry) -> entry.value != null)
						.collect(Collectors.toMap(Entry::key, Entry::value))
					);
				}
				case JsonArray array -> {
					yield new OrWhen(
						array
						.getValueList()
						.stream()
						.map((JsonValue value) -> parse(context, value))
						.toArray(When[]::new)
					);
				}
				case null, default -> {
					context.addError(element, "Expected JsonStringLiteral, JsonObject, or JsonArray");
					yield null;
				}
			};
		}
	}

	public static record StringWhen(String value) implements When {

		@Override
		public boolean test(PsiElement element) {
			return element instanceof JsonStringLiteral literal && literal.getValue().equals(this.value);
		}
	}

	public static record BooleanWhen(boolean value) implements When {

		public static final BooleanWhen
			TRUE  = new BooleanWhen(true),
			FALSE = new BooleanWhen(false);

		public static BooleanWhen get(boolean value) {
			return value ? TRUE : FALSE;
		}

		@Override
		public boolean test(PsiElement element) {
			return element instanceof JsonBooleanLiteral literal && literal.getValue() == this.value;
		}
	}

	public static record CompoundWhen(Map<String, When> conditions) implements When {

		@Override
		public boolean test(PsiElement element) {
			if (element instanceof JsonObject object) {
				for (Map.Entry<String, When> entry : this.conditions.entrySet()) {
					JsonValue property = Util.findProperty(object, entry.getKey());
					if (property == null || !entry.getValue().test(property)) return false;
				}
				return true;
			}
			return false;
		}
	}

	public static record OrWhen(When... conditions) implements When {

		@Override
		public boolean test(PsiElement element) {
			for (When condition : this.conditions) {
				if (condition.test(element)) return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "OrWhen: " + Arrays.toString(this.conditions);
		}
	}

	public static enum AnyWhen implements When {
		INSTANCE;

		@Override
		public boolean test(PsiElement element) {
			return true;
		}
	}
}