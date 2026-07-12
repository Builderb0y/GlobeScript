package builderb0y.globescript.datadriven;

import org.jetbrains.annotations.NotNull;

public record ID(String namespace, String path) {

	public static ID parse(String combined, String defaultNamespace) {
		int colon = combined.indexOf(':');
		return (
			colon >= 0
			? new ID(combined.substring(0, colon), combined.substring(colon + 1))
			: new ID(defaultNamespace, combined)
		);
	}

	public static ID parseMC(String combined) {
		return parse(combined, "minecraft");
	}

	public static ID parseBG(String combined) {
		return parse(combined, "bigglobe");
	}

	@Override
	public @NotNull String toString() {
		return this.namespace + ":" + this.path;
	}
}