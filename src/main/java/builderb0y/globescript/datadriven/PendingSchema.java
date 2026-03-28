package builderb0y.globescript.datadriven;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.intellij.json.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingEnvironment.PendingEnvironmentReference;

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
				self.jsonPath = pattern(context, value);
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

	public Pattern filePath, jsonPath;
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

	public static interface When {

		public abstract boolean test(PsiElement element);

		public static When parse(PendingDataContext context, @Nullable PsiElement element) {
			return switch (element) {
				case JsonStringLiteral string -> {
					yield new StringWhen(string.getValue());
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
						.map((JsonValue value) -> parse(context, element))
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

	public static class StringWhen implements When {

		public final String value;

		public StringWhen(String value) {
			this.value = value;
		}

		@Override
		public boolean test(PsiElement element) {
			return element instanceof JsonStringLiteral literal && literal.getValue().equals(this.value);
		}

		@Override
		public String toString() {
			return "StringWhen: " + this.value;
		}
	}

	public static class CompoundWhen implements When {

		public final Map<String, When> conditions;

		public CompoundWhen(Map<String, When> conditions) {
			this.conditions = conditions;
		}

		@Override
		public boolean test(PsiElement element) {
			if (element instanceof JsonObject object) {
				for (Map.Entry<String, When> entry : this.conditions.entrySet()) {
					JsonProperty property = object.findProperty(entry.getKey());
					if (property == null || !entry.getValue().test(property.getValue())) return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "CompoundWhen: " + this.conditions;
		}
	}

	public static class OrWhen implements When {

		public When[] conditions;

		public OrWhen(When... conditions) {
			this.conditions = conditions;
		}

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
}