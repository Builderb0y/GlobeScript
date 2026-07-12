package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
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
	public List<SchemaModel> schemas = new ObjectArrayList<>();
	public Map<Pattern, List<ReferenceModel>> references = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {

		@Override
		public int hashCode(Pattern o) {
			return o == null ? 0 : o.pattern().hashCode();
		}

		@Override
		public boolean equals(Pattern a, Pattern b) {
			if (a == b) return true;
			if (a == null || b == null) return false;
			return a.pattern().equals(b.pattern());
		}
	});
	public List<RequiredTagModel> requiredTags = new ObjectArrayList<>();
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
		Set<PsiFile> files = this.errors.keySet().stream().map(PsiElement::getContainingFile).collect(Collectors.toSet());
		PsiFile firstFile = this.findUnopenFile(files);
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
		);
		return true;
	}

	public VirtualFile envFolder() {
		return this.projectData.contentRoot.findChild("gs_env");
	}

	public void reload() {
		this.errors.clear();
		this.types.clear();
		this.environments.clear();
		this.schemas.clear();
		this.references.clear();
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
				if (resolution != null) this.types.put(type.name, resolution);
			}
			if (this.checkErrors(pending)) break done;
			for (PendingEnvironment environment : pending.environments.values()) {
				EnvironmentConfigurator resolution = converting.getEnvironment(environment.name, environment.element);
				if (resolution != null) this.environments.put(environment.name, resolution);
			}
			if (this.checkErrors(pending)) break done;
			for (PendingSchema schema : pending.schemas) {
				this.schemas.add(schema.resolve(converting));
			}
			if (this.checkErrors(pending)) break done;
			for (PendingReference reference : pending.references) {
				this.references.computeIfAbsent(reference.filePath, (Pattern $) -> new ArrayList<>()).add(reference.resolve());
			}
			for (PendingRequiredTag requiredTag : pending.requiredTags) {
				this.requiredTags.add(requiredTag.resolve());
			}
			this.standardTypes = this.new StandardTypes();
		}
		catch (Exception exception) {
			this.types.clear();
			this.environments.clear();
			this.schemas.clear();
			this.references.clear();
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

	public class StandardTypes {

		public final RawTypeModel
			root                   = this.get("root"),
			value                  = this.get("value"),
			object                 = this.get("Object"),
			string                 = this.get("String"),
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
			iterable               = this.get("Iterable"),
			iterator               = this.get("Iterator"),
			list                   = this.get("List"),
			map                    = this.get("Map"),
			columnStorage          = this.get("column_storage"),
			columnLookup           = this.get("column_lookup");

		public RawTypeModel get(String name) {
			RawTypeModel model = GsEnv.this.types.get(name);
			if (model != null) return model;
			else throw new IllegalStateException("Missing essential type " + name);
		}
	}
}