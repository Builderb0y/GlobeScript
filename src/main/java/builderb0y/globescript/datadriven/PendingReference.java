package builderb0y.globescript.datadriven;

import java.util.Locale;
import java.util.regex.Pattern;

import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class PendingReference extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingReference>("file_path", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.filePath = PendingSchema.pattern(context, value);
			}
		},
		new FieldInjector<PendingReference>("json_path", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.jsonPath = JsonPath.parse(context, value);
			}
		},
		new FieldInjector<PendingReference>("when", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.when = When.parse(context, value);
			}
		},
		new FieldInjector<PendingReference>("registry", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.registry = context.expectString(value);
			}
		},
		new FieldInjector<PendingReference>("default_namespace", false) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.defaultNamespace = value == null ? "minecraft" : context.expectString(value);
			}
		},
		new FieldInjector<PendingReference>("reference_type", false) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.type = value instanceof JsonStringLiteral literal ? switch (literal.getValue()) {
					case "element" -> Type.ELEMENT;
					case "tag" -> Type.TAG;
					default -> {
						context.addError(value, "Expected 'tag' or 'element'");
						yield Type.EITHER;
					}
				} : Type.EITHER;
			}
		}
	);

	public String registry, defaultNamespace;
	public Pattern filePath;
	public JsonPath jsonPath;
	public When when;
	public Type type;

	public PendingReference(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public ReferenceModel resolve() {
		return new ReferenceModel(this.registry, this.defaultNamespace, this.jsonPath, this.when, this.type);
	}

	public static enum Type {
		ELEMENT,
		TAG,
		EITHER;
	}
}