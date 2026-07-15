package builderb0y.globescript.datadriven;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingSchema.AnyWhen;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class PendingRequiredTag extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingRequiredTag>("registry", true) {

			@Override
			public void inject(PendingRequiredTag self, PendingDataContext context, @Nullable PsiElement value) {
				if ((self.registry = context.expectString(value, ID::parseRequireNamespace)) == null) {
					context.addError(value, "Could not parse as namespace:path pair");
				}
			}
		},
		new FieldInjector<PendingRequiredTag>("when", false) {

			@Override
			public void inject(PendingRequiredTag self, PendingDataContext context, @Nullable PsiElement value) {
				self.when = value == null ? AnyWhen.INSTANCE : When.parse(context, value);
			}
		}
	);

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public ID registry;
	public When when;

	public PendingRequiredTag(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	public RequiredTagModel resolve() {
		return new RequiredTagModel(this.registry, this.when);
	}
}