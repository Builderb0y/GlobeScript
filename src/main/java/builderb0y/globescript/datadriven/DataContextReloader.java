package builderb0y.globescript.datadriven;

import java.util.List;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

public class DataContextReloader implements BulkFileListener {

	@Override
	public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
		for (VFileEvent event : events) {
			if (event.getPath().contains("/gs_env/")) {
				for (Project project : ProjectManager.getInstance().getOpenProjects()) {
					for (Module module : ModuleManager.getInstance(project).getModules()) {
						DataContext.invalidateInstance(module, true);
					}
				}
				return;
			}
		}
	}
}