package builderb0y.globescript;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileSchemaProviderFactory implements JsonSchemaProviderFactory {

	@Override
	public @NotNull List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
		return (
			Arrays
			.stream(ModuleManager.getInstance(project).getModules())
			.<JsonSchemaFileProvider>flatMap((Module module) -> {
				return Stream.of(
					new CustomClassSchemaProvider(module),
					new DecisionTreeSchemaProvider(module)
				);
			})
			.toList()
		);
	}

	public static class CustomClassSchemaProvider implements JsonSchemaFileProvider {

		public static final Pattern CUSTOM_CLASS_PATTERN = Pattern.compile("/data/[a-z_.\\-]+/bigglobe/custom_class/");

		public final Module module;

		public CustomClassSchemaProvider(Module module) {
			this.module = module;
		}

		@Override
		public boolean isAvailable(@NotNull VirtualFile file) {
			return CUSTOM_CLASS_PATTERN.matcher(file.getPath()).find();
		}

		@Override
		public @NotNull @Nls String getName() {
			return "GlobeScript custom class";
		}

		@Override
		public @Nullable VirtualFile getSchemaFile() {
			for (VirtualFile root : ModuleRootManager.getInstance(this.module).getContentRoots()) {
				VirtualFile result = root.findFileByRelativePath("gs_env/file_schemas/custom_class.json");
				if (result != null) return result;
			}
			return null;
		}

		@Override
		public @NotNull SchemaType getSchemaType() {
			return SchemaType.schema;
		}
	}

	public static class DecisionTreeSchemaProvider implements JsonSchemaFileProvider {

		public static final Pattern DECISION_TREE_PATTERN = Pattern.compile("/data/[a-z_.\\-]+/bigglobe/worldgen/decision_tree");

		public final Module module;

		public DecisionTreeSchemaProvider(Module module) {
			this.module = module;
		}

		@Override
		public boolean isAvailable(@NotNull VirtualFile file) {
			return DECISION_TREE_PATTERN.matcher(file.getPath()).find();
		}

		@Override
		public @NotNull @Nls String getName() {
			return "GlobeScript decision tree";
		}

		@Override
		public @Nullable VirtualFile getSchemaFile() {
			for (VirtualFile root : ModuleRootManager.getInstance(this.module).getContentRoots()) {
				VirtualFile result = root.findFileByRelativePath("gs_env/file_schemas/decision_tree.json");
				if (result != null) return result;
			}
			return null;
		}

		@Override
		public @NotNull SchemaType getSchemaType() {
			return SchemaType.schema;
		}
	}
}