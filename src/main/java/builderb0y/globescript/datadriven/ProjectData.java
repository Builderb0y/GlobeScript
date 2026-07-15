package builderb0y.globescript.datadriven;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import builderb0y.globescript.ScriptEnvironment;

public class ProjectData {

	public final Project project;
	public final VirtualFile contentRoot;
	public GsEnv env;
	public Map<VirtualFile /* data folder */, PackData> packs = new HashMap<>();

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

	public PackData getPackData(VirtualFile file) {
		while (file != null) {
			PackData pack = this.packs.get(file);
			if (pack != null) return pack;
			else file = file.getParent();
		}
		return null;
	}

	public PackData getProvidedDataPack() {
		VirtualFile provided = this.contentRoot.findFileByRelativePath("gs_env/provided/data");
		return provided != null ? this.packs.get(provided) : null;
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
					PackData pack = this.packs.get(parent);
					if (pack != null) {
						changed |= pack.fileChanged(file);
						break loop;
					}
				}
				case "gs_env" -> {
					if (this.env != null && file.equals(this.env.envFolder())) {
						this.env = null;
						changed = true;
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
		this.packs.clear();
		VirtualFile src = this.contentRoot.findChild("src");
		if (src != null) {
			for (VirtualFile dataPack : src.getChildren()) {
				VirtualFile dataFolder = dataPack.findFileByRelativePath("resources/data");
				if (dataFolder != null) {
					PackData pack = new PackData(this, dataFolder);
					pack.scan();
					this.packs.put(dataFolder, pack);
				}
			}
			VirtualFile provided = this.contentRoot.findFileByRelativePath("gs_env/provided/data");
			if (provided != null) {
				PackData pack = new PackData(this, provided);
				pack.scan();
				this.packs.put(provided, pack);
			}
		}
	}
}