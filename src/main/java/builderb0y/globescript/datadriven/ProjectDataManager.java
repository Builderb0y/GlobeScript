package builderb0y.globescript.datadriven;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ProjectDataManager {

	public static final Key<ProjectDataManager> KEY = Key.create("GlobeScript.ProjectDataManager");

	public final Map<VirtualFile, ProjectData> roots = new HashMap<>();

	public ProjectData findProjectData(VirtualFile file) {
		while (file != null) {
			ProjectData data = this.roots.get(file);
			if (data != null) return data;
			else file = file.getParent();
		}
		return null;
	}

	public boolean fileChanged(VirtualFile file) {
		ProjectData projectData = this.findProjectData(file);
		return projectData != null && projectData.fileChanged(file);
	}

	public static ProjectDataManager getInstance(Project project) {
		return project.getUserData(KEY);
	}

	public static ProjectDataManager getOrCreateInstance(Project project) {
		synchronized (KEY) {
			ProjectDataManager manager = project.getUserData(KEY);
			if (manager != null) return manager;
			manager = new ProjectDataManager();
			for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
				ProjectData projectData = new ProjectData(project, root);
				projectData.reload();
				manager.roots.put(root, projectData);
			}
			project.putUserData(KEY, manager);
			return manager;
		}
	}

	public static class Invalidator implements ModuleRootListener {

		@Override
		public void rootsChanged(@NotNull ModuleRootEvent event) {
			ProjectDataManager manager = event.getProject().getUserData(KEY);
			if (manager != null) {
				manager.roots.clear();
				for (VirtualFile root : ProjectRootManager.getInstance(event.getProject()).getContentRoots()) {
					ProjectData projectData = new ProjectData(event.getProject(), root);
					projectData.reload();
					manager.roots.put(root, projectData);
				}
			}
		}
	}
}