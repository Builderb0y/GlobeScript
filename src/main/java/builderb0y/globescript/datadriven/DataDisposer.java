package builderb0y.globescript.datadriven;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

public class DataDisposer implements DynamicPluginListener {

	@Override
	public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			for (Module module : ModuleManager.getInstance(project).getModules()) {
				DataContext.invalidateInstance(module, false);
			}
		}
	}
}