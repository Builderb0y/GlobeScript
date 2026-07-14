package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.vfs.VirtualFile;

public class PackData {

	public final ProjectData projectData;
	public final VirtualFile dataFolder;
	public final CustomClassEnvironment customClasses;
	public final ColumnValueEnvironment columnValues;
	public final WorldTraitEnvironment worldTraits;

	public PackData(ProjectData data, VirtualFile dataFolder) {
		this.projectData = data;
		this.dataFolder = dataFolder;
		this.customClasses = new CustomClassEnvironment(this);
		this.columnValues = new ColumnValueEnvironment(this);
		this.worldTraits = new WorldTraitEnvironment(this);
	}

	public static PackData find(VirtualFile file) {
		ProjectData data = ProjectData.find(file);
		return data != null ? data.getPackData(file) : null;
	}

	public UID identify(VirtualFile file) {
		return identify(this.dataFolder, file);
	}

	public static UID identify(VirtualFile dataFolder, VirtualFile file) {
		List<String> parts = new ArrayList<>();
		while (file != null && !file.equals(dataFolder)) {
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
		if (index == 0) return null;
		String elementPath = String.join("/", parts.subList(0, index).reversed());
		return new UID(new ID(registryNamespace, registryPath), new ID(elementNamespace, elementPath), tag);
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
		this.customClasses.setupEnvironment(environment);
		this.columnValues.setupEnvironment(environment, file, columnValueFlags);
		this.worldTraits.setupEnvironment(environment, columnValueFlags);
	}
}