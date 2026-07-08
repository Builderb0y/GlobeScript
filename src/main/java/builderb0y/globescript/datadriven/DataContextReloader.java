package builderb0y.globescript.datadriven;

import java.util.List;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

public class DataContextReloader implements BulkFileListener {

	@Override
	public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
		Project[] projects = ProjectManager.getInstance().getOpenProjects();
		for (Project project : projects) {
			boolean restart = false;
			ProjectDataManager manager = ProjectDataManager.getInstance(project);
			if (manager != null) {
				for (VFileEvent event : events) {
					restart |= manager.fileChanged(event.getFile());
				}
			}
			if (restart) DaemonCodeAnalyzer.getInstance(project).restart();
		}
	}
}