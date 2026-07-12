package builderb0y.globescript.datadriven;

import com.intellij.openapi.vfs.VirtualFile;

public class PackData {

	public final ProjectData projectData;
	public final VirtualFile dataFolder;
	public final CustomClassEnvironment customClasses;
	public final ColumnValueEnvironment columnValues;

	public PackData(ProjectData data, VirtualFile dataFolder) {
		this.projectData = data;
		this.dataFolder = dataFolder;
		this.customClasses = new CustomClassEnvironment(this);
		this.columnValues = new ColumnValueEnvironment(this);
	}

	public void scan() {
		this.customClasses.scan();
		this.columnValues.scan();
	}

	public boolean fileChanged(VirtualFile file) {
		return this.customClasses.fileChanged(file) | this.columnValues.fileChanged(file);
	}

	public void setupEnvironment(EnvironmentModel environment, VirtualFile file, int columnValueFlags) {
		this.customClasses.setupEnvironment(environment);
		this.columnValues.setupEnvironment(environment, file, columnValueFlags);
	}
}