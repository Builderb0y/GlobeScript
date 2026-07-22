package builderb0y.globescript.datadriven;

import java.util.function.BiConsumer;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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

	public static ID parseRequireNamespace(String combined) {
		int colon = combined.indexOf(':');
		return (
			colon >= 0
			? new ID(combined.substring(0, colon), combined.substring(colon + 1))
			: null
		);
	}

	public static ID parseSkipTag(String combined, String defaultNamespace) {
		boolean isTag = combined.startsWith("#");
		int colon = combined.indexOf(':');
		String namespace, path;
		if (colon >= 0) {
			namespace = combined.substring(isTag ? 1 : 0, colon);
			path = combined.substring(colon + 1);
		}
		else {
			namespace = defaultNamespace;
			path = combined.substring(isTag ? 1 : 0);
		}
		return new ID(namespace, path);
	}

	public static ID parseBGSkipTag(String combined) {
		return parseSkipTag(combined, "bigglobe");
	}

	public static ID parseMCSkipTag(String combined) {
		return parseSkipTag(combined, "minecraft");
	}

	public void walkJsonRegistry(VirtualFile dataFolder, boolean tag, BiConsumer<VirtualFile, ID> action) {
		for (VirtualFile elementNamespace : VfsUtil.getChildren(dataFolder)) {
			String namespace = elementNamespace.getName();
			VirtualFile next;
			if (tag) {
				next = elementNamespace.findChild("tags");
				if (next == null) continue;
			}
			else {
				next = elementNamespace;
			}
			if (!this.namespace().equals("minecraft")) {
				next = next.findChild(this.namespace());
				if (next == null) continue;
			}
			next = next.findFileByRelativePath(this.path());
			if (next == null) continue;
			VirtualFile registryRoot = next;
			VfsUtilCore.iterateChildrenRecursively(
				registryRoot,
				(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
				(VirtualFile file) -> {
					if (!file.isDirectory()) {
						String relativePath = VfsUtilCore.getRelativePath(file, registryRoot);
						relativePath = relativePath.substring(0, relativePath.length() - ".json".length());
						action.accept(file, new ID(namespace, relativePath));
					}
					return true;
				}
			);
		}
	}

	public boolean registryEquals(UID uid) {
		return this.equals(uid.registry());
	}

	public boolean contains(VirtualFile dataFolder, VirtualFile file) {
		UID uid = PackData.identify(dataFolder, file);
		return uid != null && this.registryEquals(uid);
	}

	public boolean contains(VirtualFile dataFolder, PsiFile psiFile) {
		return this.contains(dataFolder, psiFile.getVirtualFile());
	}

	public boolean contains(VirtualFile dataFolder, PsiElement element) {
		return this.contains(dataFolder, element.getContainingFile());
	}

	public String toTagString() {
		return "#" + this.namespace + ":" + this.path;
	}

	@Override
	public @NotNull String toString() {
		return this.namespace + ":" + this.path;
	}
}