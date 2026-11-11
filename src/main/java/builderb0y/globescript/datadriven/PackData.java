package builderb0y.globescript.datadriven;

import java.util.*;

import com.intellij.openapi.vfs.VirtualFile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class PackData {

	public final ProjectData projectData; public ProjectData projectData() { return this.projectData; }
	public final VirtualFile dataFolder; public VirtualFile dataFolder() { return this.dataFolder; }
	public final Set<PackData> dependencies; public Set<PackData> dependencies() { return this.dependencies; }
	public final CustomClassEnvironment customClasses;
	public final ColumnValueEnvironment columnValues;
	public final WorldTraitEnvironment worldTraits;

	public PackData(ProjectData data, VirtualFile dataFolder) {
		this.projectData = data;
		this.dataFolder = dataFolder;
		this.dependencies = new ObjectLinkedOpenHashSet<>(1);
		this.customClasses = new CustomClassEnvironment(this);
		this.columnValues = new ColumnValueEnvironment(this);
		this.worldTraits = new WorldTraitEnvironment(this);
	}

	public Set<PackData> getChain() {
		Set<PackData> set = new ObjectOpenHashSet<>();
		this.addChain(set);
		return set;
	}

	public void addChain(Set<PackData> set) {
		if (set.add(this)) {
			for (PackData dependency : this.dependencies) {
				dependency.addChain(set);
			}
		}
	}

	public static PackData find(VirtualFile file) {
		ProjectData data = ProjectData.find(file);
		return data != null ? data.getPackData(file) : null;
	}

	public UID identify(VirtualFile file) {
		return identify(Collections.singletonMap(this.dataFolder, this), file);
	}

	public static UID identify(Map<VirtualFile, PackData> dataFolder, VirtualFile file) {
		List<String> parts = new ArrayList<>();
		PackData pack = null;
		while (file != null && (pack = dataFolder.get(file)) == null) {
			parts.add(file.getNameWithoutExtension());
			file = file.getParent();
		}
		int index = parts.size();
		String elementNamespace = _get(parts, --index);
		if (elementNamespace == null) return null;
		String registryNamespace = _get(parts, --index);
		boolean tag = "tags".equals(registryNamespace);
		if (tag) registryNamespace = _get(parts, --index);
		if (registryNamespace == null) return null;
		String registryPath;
		if ("bigglobe".equals(registryNamespace)) {
			registryPath = _get(parts, --index);
			if (registryPath == null) return null;
		}
		else {
			registryPath = registryNamespace;
			registryNamespace = "minecraft";
		}
		if ("worldgen".equals(registryPath)) {
			String next = _get(parts, --index);
			if (next == null) return null;
			registryPath = registryPath + "/" + next;
		}
		if (index == 0) return null;
		String elementPath = String.join("/", parts.subList(0, index).reversed());
		return new UID(pack, new ID(registryNamespace, registryPath), new ID(elementNamespace, elementPath), tag);
	}

	public static String _get(List<String> parts, int index) {
		return index >= 0 && index < parts.size() ? parts.get(index) : null;
	}

	public void scan() {
		this.customClasses.scan();
		this.columnValues.scan();
		this.worldTraits.scan();
	}

	public boolean fileChanged(VirtualFile file) {
		return (
			this.customClasses.fileChanged(file) |
			this.columnValues.fileChanged(file) |
			this.worldTraits.fileChanged(file)
		);
	}

	public void setupEnvironment(EnvironmentModel environment, VirtualFile file, int columnValueFlags) {
		this.customClasses.setupEnvironment(environment, file);
		this.columnValues.setupEnvironment(environment, file, columnValueFlags);
		this.worldTraits.setupEnvironment(environment, columnValueFlags);
	}
}