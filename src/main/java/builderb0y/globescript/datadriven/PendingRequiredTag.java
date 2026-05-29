package builderb0y.globescript.datadriven;

import java.util.regex.Pattern;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingSchema.AnyWhen;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class PendingRequiredTag extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingRequiredTag>("file_path", true) {

			@Override
			public void inject(PendingRequiredTag self, PendingDataContext context, @Nullable PsiElement value) {
				self.filePath = PendingSchema.pattern(context, value);
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

	public Pattern filePath;
	public When when;

	public PendingRequiredTag(PendingDataContext context, PsiElement element) {
		super(context, element);
	}

	public RequiredTagModel resolve() {
		return new RequiredTagModel(this.filePath, this.when);
	}
}