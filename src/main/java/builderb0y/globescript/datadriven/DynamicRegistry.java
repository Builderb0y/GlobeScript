package builderb0y.globescript.datadriven;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import builderb0y.globescript.datadriven.DynamicRegistry.DynamicRegistryElement;

public abstract class DynamicRegistry<T extends DynamicRegistryElement> {

	public final PackData packData;
	public final Map<ID, T> elements;
	public final String[] registryPath;

	public DynamicRegistry(PackData packData, String... path) {
		this.packData = packData;
		this.registryPath = path;
		this.elements = new HashMap<>();
	}

	public static VirtualFile walk(VirtualFile root, String... parts) {
		for (String part : parts) {
			root = root.findChild(part);
			if (root == null) return null;
		}
		return root;
	}

	public void scan() {
		VirtualFile[] namespaces = this.packData.dataFolder.getChildren();
		if (namespaces != null) {
			for (VirtualFile namespace : namespaces) {
				VirtualFile registry = walk(namespace, this.registryPath);
				if (registry != null) {
					VfsUtilCore.iterateChildrenRecursively(
						registry,
						(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
						(VirtualFile fileOrDir) -> {
							if (!fileOrDir.isDirectory()) {
								String path = VfsUtilCore.getRelativePath(fileOrDir, registry);
								T element = this.compute(fileOrDir);
								if (element != null) {
									path = path.substring(0, path.length() - ".json".length());
									this.elements.put(new ID(namespace.getName(), path), element);
								}
							}
							return true;
						}
					);
				}
			}
		}
	}

	public ID idOf(VirtualFile file) {
		String path = VfsUtilCore.getRelativePath(file, this.packData.dataFolder);
		int firstSlash = path.indexOf('/'), slash = firstSlash;
		for (String part : this.registryPath) {
			if (path.regionMatches(slash + 1, part, 0, part.length())) {
				slash += part.length() + 1;
				if (path.charAt(slash) != '/') return null;
			}
			else {
				return null;
			}
		}
		String namespace = path.substring(0, firstSlash);
		path = path.substring(slash + 1, path.length() - ".json".length());
		return new ID(namespace, path);
	}

	public boolean fileChanged(VirtualFile file) {
		ID id = this.idOf(file);
		if (id != null) {
			T element = this.compute(file);
			if (element != null) this.elements.put(id, element);
			else this.elements.remove(id);
			this.elements.values().forEach(T::clearCaches);
			return true;
		}
		return false;
	}

	public abstract T compute(VirtualFile file);

	public static class DynamicRegistryElement {

		public void clearCaches() {}
	}
}