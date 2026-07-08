package builderb0y.globescript.datadriven;

import com.intellij.openapi.vfs.VirtualFile;

public abstract class EnvironmentConfigurator {

	public final String name;

	public EnvironmentConfigurator(String name) {
		this.name = name;
	}

	public abstract void configure(VirtualFile source, EnvironmentModel environment);

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.name;
	}
}