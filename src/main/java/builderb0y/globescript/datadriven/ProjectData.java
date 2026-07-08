package builderb0y.globescript.datadriven;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;

import builderb0y.globescript.ScriptEnvironment;

public class ProjectData {

	public final Project project;
	public final VirtualFile contentRoot;
	public GsEnv env;
	public Map<VirtualFile /* data folder */, CustomClassEnvironment> customClasses = new HashMap<>();

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

	public CustomClassEnvironment getCustomClasses(VirtualFile file) {
		while (file != null) {
			CustomClassEnvironment customClasses = this.customClasses.get(file);
			if (customClasses != null) return customClasses;
			else file = file.getParent();
		}
		return null;
	}

	public ScriptEnvironment combineAllEnvironments(VirtualFile file) {
		GsEnv gsEnv = this.environment();
		ScriptEnvironment environment = new ScriptEnvironment(
			gsEnv.standardTypes,
			file,
			gsEnv.environments.values().toArray(
				new EnvironmentConfigurator[gsEnv.environments.size()]
			)
		);
		CustomClassEnvironment classes = this.getCustomClasses(file);
		if (classes != null) classes.setupEnvironment(environment);
		return environment;
	}

	public boolean fileChanged(VirtualFile file) {
		VirtualFile parent = file;
		boolean changed = false;
		while (parent != null) {
			switch (parent.getName()) {
				case "data" -> {
					CustomClassEnvironment customClasses = this.customClasses.get(parent);
					if (customClasses != null) changed |= customClasses.fileChanged(file);
				}
				case "gs_env" -> {
					this.env = null;
					changed = true;
				}
			}
			parent = parent.getParent();
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
		this.customClasses.clear();
		VirtualFile src = this.contentRoot.findChild("src");
		if (src != null) {
			for (VirtualFile dataPack : src.getChildren()) {
				VirtualFile dataFolder = dataPack.findChild("data");
				if (dataFolder != null) {
					CustomClassEnvironment customClasses = new CustomClassEnvironment(this, dataFolder);
					customClasses.scan();
					this.customClasses.put(dataFolder, customClasses);
				}
			}
		}
	}
}