package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public record UID(ID registry, ID element, boolean tag) {

	public VirtualFile findJson(VirtualFile dataFolder) {
		VirtualFile file = dataFolder.findChild(this.element.namespace());
		if (file == null) return null;
		if (this.tag) {
			file = file.findChild("tags");
			if (file == null) return null;
		}
		if (!this.registry.namespace().equals("minecraft")) {
			file = file.findChild(this.registry.namespace());
			if (file == null) return null;
		}
		file = file.findFileByRelativePath(this.registry.path());
		if (file == null) return null;
		file = file.findFileByRelativePath(this.element.path() + ".json");
		return file;
	}

	public List<String> jsonFilePathParts() {
		List<String> list = new ArrayList<>();
		list.add(this.element.namespace());
		if (this.tag) list.add("tags");
		if (!this.registry.namespace().equals("minecraft")) list.add(this.registry.namespace());
		list.addAll(Arrays.asList(this.registry.path().split("/")));
		list.addAll(Arrays.asList(this.element.path().split("/")));
		return list;
	}

	public String formatElement() {
		return this.tag ? "#" + this.element : this.element.toString();
	}

	@Override
	public @NotNull String toString() {
		return (this.tag ? "#" : "") + this.registry + ">" + this.element;
	}
}