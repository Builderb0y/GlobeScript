package builderb0y.globescript.datadriven;

import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;
import builderb0y.globescript.datadriven.ReferenceModel.TargetModel;

public class PendingReference extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingReference>("reference", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.reference = new PendingTarget(context, value);
			}
		},
		new FieldInjector<PendingReference>("declaration", true) {

			@Override
			public void inject(PendingReference self, PendingDataContext context, @Nullable PsiElement value) {
				self.declaration = new PendingTarget(context, value);
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

	public PendingTarget reference, declaration;
	public String defaultNamespace;
	public Type type;

	public PendingReference(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public ReferenceModel resolve() {
		return new ReferenceModel(this.reference.resolve(), this.declaration.resolve(), this.defaultNamespace, this.type);
	}

	public static class PendingTarget extends PendingElement {

		public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
			new FieldInjector<PendingTarget>("registry", true) {

				@Override
				public void inject(PendingTarget self, PendingDataContext context, @Nullable PsiElement value) {
					if ((self.registry = context.expectString(value, ID::parseRequireNamespace)) == null) {
						context.addError(value, "Can't parse as namespace:path");
					}
				}
			},
			new FieldInjector<PendingTarget>("json_path", false) {

				@Override
				public void inject(PendingTarget self, PendingDataContext context, @Nullable PsiElement value) {
					self.jsonPath = JsonPath.parse(context, value);
				}
			},
			new FieldInjector<PendingTarget>("conditions", false) {

				@Override
				public void inject(PendingTarget self, PendingDataContext context, @Nullable PsiElement value) {
					self.conditions = When.parse(context, value);
				}
			}
		);

		@Override
		public FieldInjectorMap getFields() {
			return FIELDS;
		}

		public ID registry;
		public JsonPath jsonPath;
		public When conditions;

		public PendingTarget(PendingDataContext context, PsiElement element) {
			super(context, element);
		}

		public TargetModel resolve() {
			return new TargetModel(this.registry, this.jsonPath, this.conditions);
		}
	}

	public static enum Type {
		ELEMENT,
		TAG,
		EITHER;

		public boolean elementsAllowed() {
			return this != TAG;
		}

		public boolean tagsAllowed() {
			return this != ELEMENT;
		}
	}
}