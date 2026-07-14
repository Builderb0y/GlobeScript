package builderb0y.globescript.datadriven;

import com.intellij.psi.PsiElement;

public abstract class EnvironmentConfigurator {

	public final String name;

	public EnvironmentConfigurator(String name) {
		this.name = name;
	}

	public abstract void configure(PsiElement source, EnvironmentModel environment);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.name;
	}
}