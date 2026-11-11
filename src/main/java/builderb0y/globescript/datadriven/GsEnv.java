package builderb0y.globescript.datadriven;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.DisplayErrorAction;
import builderb0y.globescript.Instances;
import builderb0y.globescript.PsiErrorDisplay;

public class GsEnv {

	public final ProjectData projectData;
	public Map<PsiElement, List<PsiErrorDisplay>> errors = new Reference2ObjectOpenHashMap<>();
	public Map<String, RawTypeModel> types = new Object2ObjectOpenHashMap<>();
	public Map<String, EnvironmentConfigurator> environments = new Object2ObjectOpenHashMap<>();
	public Map<ID, List<SchemaModel>> schemas = new Object2ObjectOpenHashMap<>();
	public Map<ID, List<ReferenceModel>> references = new Object2ObjectOpenHashMap<>();
	public Map<ID, List<RequiredTagModel>> requiredTags = new Object2ObjectOpenHashMap<>();
	public Map<String, PackData> packsByName = new Object2ObjectOpenHashMap<>();
	public Map<VirtualFile, PackData> packsByFolder = new Object2ObjectOpenHashMap<>();
	public StandardTypes standardTypes;

	public GsEnv(ProjectData projectData) {
		this.projectData = projectData;
	}

	public static GsEnv find(VirtualFile file) {
		ProjectData projectData = ProjectData.find(file);
		return projectData != null ? projectData.environment() : null;
	}

	public boolean isValid() {
		return this.errors.isEmpty();
	}

	public PsiFile findUnopenFile(Set<PsiFile> files) {
		FileEditorManager manager = FileEditorManager.getInstance(this.projectData.project);
		FileEditor editor = manager.getFocusedEditor();
		VirtualFile current = editor != null ? editor.getFile() : null;
		for (PsiFile file : files) {
			if (!file.getVirtualFile().equals(current)) {
				return file;
			}
		}
		return null;
	}

	public boolean checkErrors(PendingDataContext pending) {
		if (pending.errors.isEmpty()) return false;
		this.errors.putAll(pending.errors);
		Map<PsiFile, List<PsiErrorDisplay>> files = (
			this
			.errors
			.entrySet()
			.stream()
			.collect(
				Collectors.groupingBy(
					(Map.Entry<PsiElement, List<PsiErrorDisplay>> entry) -> entry.getKey().getContainingFile(),
					Collectors.flatMapping(
						(Map.Entry<PsiElement, List<PsiErrorDisplay>> entry) -> entry.getValue().stream(),
						Collectors.toList()
					)
				)
			)
		);
		PsiFile firstFile = this.findUnopenFile(files.keySet());
		if (firstFile != null) Notifications.Bus.notify(
			new Notification(
				Instances.ERROR_NOTIFICATION,
				"Error(s) in gs_env:",
				files.size() > 1
				? firstFile.getName() + " and " + (files.size() - 1) + " other(s)"
				: firstFile.getName(),
				NotificationType.WARNING
			)
			.addAction(new AnAction() {

				{
					this.getTemplatePresentation().setText("Open File");
				}

				@Override
				public void actionPerformed(@NotNull AnActionEvent event) {
					FileEditorManager.getInstance(firstFile.getProject()).openFile(firstFile.getVirtualFile());
				}
			})
			.addAction(new DisplayErrorAction(files.get(firstFile).getFirst().text))
		);
		return true;
	}

	public VirtualFile envFolder() {
		return this.projectData.contentRoot.findChild("gs_env");
	}

	public VirtualFile hardCodedFolder() {
		return this.projectData.contentRoot.findFileByRelativePath("gs_env/hard_coded");
	}

	public void reload() {
		this.errors.clear();
		this.types.clear();
		this.environments.clear();
		this.schemas.clear();
		this.references.clear();
		this.packsByName.clear();
		this.packsByFolder.clear();
		this.standardTypes = null;
		VirtualFile envFolder = this.envFolder();
		if (envFolder == null) return;
		PendingDataContext pending = new PendingDataContext(this.projectData, envFolder);
		pending.scan();
		done:
		try {
			if (this.checkErrors(pending)) break done;
			ConvertingDataContext converting = new ConvertingDataContext(pending);
			for (PendingType type : pending.types.values()) {
				RawTypeModel resolution = converting.getType(type.name, type.element);
				if (resolution != null && this.types.put(type.name, resolution) != null) {
					throw new RuntimeException("Duplicate type name: " + type.name);
				}
			}
			if (this.checkErrors(pending)) break done;
			for (PendingEnvironment environment : pending.environments.values()) {
				EnvironmentConfigurator resolution = converting.getEnvironment(environment.name, environment.element);
				if (resolution != null && this.environments.put(environment.name, resolution) != null) {
					throw new RuntimeException("Duplicate environment name: " + environment.name);
				}
			}
			if (this.checkErrors(pending)) break done;
			for (PendingSchema schema : pending.schemas) {
				this.schemas.computeIfAbsent(schema.registry, $ -> new ObjectArrayList<>()).add(schema.resolve(converting));
			}
			if (this.checkErrors(pending)) break done;
			for (PendingReference reference : pending.references) {
				this.references.computeIfAbsent(reference.reference.registry, $ -> new ObjectArrayList<>()).add(reference.resolve());
			}
			if (this.checkErrors(pending)) break done;
			for (PendingRequiredTag requiredTag : pending.requiredTags) {
				this.requiredTags.computeIfAbsent(requiredTag.registry, $ -> new ObjectArrayList<>()).add(requiredTag.resolve());
			}
			if (this.checkErrors(pending)) break done;
			for (PendingPack pack : pending.packs) {
				PackData packData = new PackData(this.projectData, pack.dataFolder);
				if (this.packsByName.put(pack.name, packData) != null) {
					throw new RuntimeException("Duplicate pack name: " + pack.name);
				}
				if (this.packsByFolder.put(pack.dataFolder, packData) != null) {
					throw new RuntimeException("Duplicate pack folder: " + pack.dataFolder.getPath());
				}
			}
			for (PendingPack pendingPack : pending.packs) {
				PackData root = this.packsByName.get(pendingPack.name);
				Arrays.stream(pendingPack.dependencies).map(this.packsByName::get).forEach((PackData pack) -> pack.dependencies.add(root));
				root.scan();
			}
			if (this.checkErrors(pending)) break done;
			this.standardTypes = this.new StandardTypesImpl();
		}
		catch (Exception exception) {
			this.types.clear();
			this.environments.clear();
			this.schemas.clear();
			this.references.clear();
			this.packsByName.clear();
			this.packsByFolder.clear();
			Notifications.Bus.notify(
				new Notification(
					Instances.ERROR_NOTIFICATION,
					"Uncaught exception in gs_env:",
					exception.toString(),
					NotificationType.WARNING
				)
				.addAction(new DisplayErrorAction(exception))
			);
		}
	}

	public static abstract class StandardTypes {

		public final RawTypeModel
			root                   = this.get("root"),
			value                  = this.get("value"),
			object                 = this.get("object"),
			string                 = this.get("string"),
			primitiveBitwise       = this.get("primitive_bitwise"),
			primitiveNumber        = this.get("primitive_number"),
			primitiveInteger       = this.get("primitive_integer"),
			primitiveFloatingPoint = this.get("primitive_floating_point"),
			primitiveComparable    = this.get("primitive_comparable"),
			byte_                  = this.get("byte"),
			short_                 = this.get("short"),
			int_                   = this.get("int"),
			long_                  = this.get("long"),
			float_                 = this.get("float"),
			double_                = this.get("double"),
			char_                  = this.get("char"),
			boolean_               = this.get("boolean"),
			void_                  = this.get("void"),
			iterable               = this.get("iterable"),
			iterator               = this.get("iterator"),
			list                   = this.get("list"),
			set                    = this.get("set"),
			map                    = this.get("map"),
			columnStorage          = this.get("column_storage"),
			columnLookup           = this.get("column_lookup"),
			worldTraits            = this.get("world_traits"),
			columnWorldTraits      = this.get("column_world_traits"),
			lookupWorldTraits      = this.get("lookup_world_traits");

		public abstract RawTypeModel get(String name);
	}

	public class StandardTypesImpl extends StandardTypes {

		@Override
		public RawTypeModel get(String name) {
			RawTypeModel model = GsEnv.this.types.get(name);
			if (model != null) return model;
			else throw new IllegalStateException("Missing essential type " + name);
		}
	}
}