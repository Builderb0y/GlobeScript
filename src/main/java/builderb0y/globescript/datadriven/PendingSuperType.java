package builderb0y.globescript.datadriven;

import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.psi.PsiElement;

public class PendingSuperType extends PendingElement implements PendingElement.Named {

	public static final PendingSuperType[] EMPTY_ARRAY = {};

	public String name;
		@Override public String name() { return this.name; }
		@Override public void name(String name) { this.name = name; }

	public PendingSuperType(PendingDataContext context, JsonStringLiteral element) {
		super(context, element);
	}

	public PendingSuperType(PsiElement element, String name) {
		this.element = element;
		this.name = name;
	}

	@Override
	public void injectAll(PendingDataContext context, PsiElement element) {
		this.name = ((JsonStringLiteral)(element)).getValue();
	}
}