package builderb0y.globescript.datadriven;

import org.jetbrains.annotations.NotNull;

public record UID(ID registry, ID element, boolean tag) {

	public String formatElement() {
		return this.tag ? "#" + this.element : this.element.toString();
	}

	@Override
	public @NotNull String toString() {
		return (this.tag ? "#" : "") + this.registry + ">" + this.element;
	}
}