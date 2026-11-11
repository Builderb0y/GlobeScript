package builderb0y.globescript.datadriven;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import builderb0y.globescript.ScriptEnvironment;

public class ProjectData {

	public final Project project;
	public final VirtualFile contentRoot;
	public GsEnv env;

	public ProjectData(Project project, VirtualFile contentRoot) {
		this.project = project;
		this.contentRoot = contentRoot;
	}

	public static ProjectData find(VirtualFile file) {
		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			ProjectData projectData = ProjectDataManager.getOrCreateInstance(project).findProjectData(file);
			if (projectData != null) return projectData;
		}
		return null;
	}

	public UID identify(VirtualFile file) {
		return PackData.identify(this.environment().packsByFolder, file);
	}

	public PackData getPackData(VirtualFile file) {
		GsEnv environment = this.environment();
		while (file != null) {
			PackData pack = environment.packsByFolder.get(file);
			if (pack != null) return pack;
			else file = file.getParent();
		}
		return null;
	}

	public ScriptEnvironment combineAllEnvironments(PsiElement source) {
		GsEnv gsEnv = this.environment();
		ScriptEnvironment environment = new ScriptEnvironment(
			gsEnv.standardTypes,
			source,
			gsEnv.environments.values().toArray(
				new EnvironmentConfigurator[gsEnv.environments.size()]
			)
		);
		VirtualFile file = source.getContainingFile().getVirtualFile();
		PackData pack = this.getPackData(file);
		if (pack != null) pack.setupEnvironment(environment, file, ColumnValueEnvironment.FLAG_XYZ_PROVIDED);
		return environment;
	}

	public boolean fileChanged(VirtualFile file) {
		boolean changed = false;
		loop:
		for (VirtualFile parent = file; parent != null; parent = parent.getParent()) {
			switch (parent.getName()) {
				case "data" -> {
					if (this.env != null) {
						PackData pack = this.env.packsByFolder.get(parent);
						if (pack != null) {
							changed |= pack.fileChanged(file);
							break loop;
						}
					}
				}
				case "gs_env" -> {
					if (this.env != null && parent.equals(this.env.envFolder())) {
						this.env = null;
						changed = true;
						break loop;
					}
				}
			}
		}
		return changed;
	}

	public GsEnv environment() {
		GsEnv env = this.env;
		if (env == null) {
			env = new GsEnv(this);
			env.reload();
			this.env = env;
		}
		return env;
	}

	public void reload() {
		this.env = null;
	}
}