package builderb0y.globescript.datadriven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.intellij.json.psi.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.Colors;
import builderb0y.globescript.PsiErrorDisplay;

public class PendingDataContext {

	public static final Map<String, TextAttributesKey> IDENTIFIER_COLORS = Map.of(
		"local",           Colors.LOCAL,
		"global",          Colors.GLOBAL,
		"parameter",       Colors.PARAMETER,
		"instance_field",  Colors.INSTANCE_FIELD,
		"static_field",    Colors.STATIC_FIELD,
		"function",        Colors.FUNCTION,
		"instance_method", Colors.INSTANCE_METHOD,
		"static_method",   Colors.INSTANCE_METHOD,
		"keyword",         Colors.KEYWORD,
		"type",            Colors.TYPE
	);

	public final ProjectData projectData;
	public final VirtualFile envFolder;
	public Map<PsiElement, List<PsiErrorDisplay>> errors = new Reference2ObjectOpenHashMap<>();
	public Map<String, PendingType> types = new Object2ObjectOpenHashMap<>();
	public Map<String, PendingEnvironment> environments = new Object2ObjectOpenHashMap<>();
	public List<PendingSchema> schemas = new ArrayList<>();
	public List<PendingReference> references = new ArrayList<>();
	public List<PendingRequiredTag> requiredTags = new ArrayList<>();

	public PendingDataContext(ProjectData projectData, VirtualFile envFolder) {
		this.projectData = projectData;
		this.envFolder = envFolder;
	}

	public void scan() {
		VirtualFile hardCoded = this.envFolder.findChild("hard_coded");
		if (hardCoded != null) VfsUtilCore.iterateChildrenRecursively(
			hardCoded,
			(VirtualFile file) -> {
				if (file.isDirectory()) return true;
				String extension = file.getExtension();
				return "json5".equals(extension) || "json".equals(extension);
			},
			this::scan
		);
	}

	public boolean scan(VirtualFile file) {
		if (!file.isDirectory()) {
			PsiFile psiFile = PsiManager.getInstance(this.projectData.project).findFile(file);
			if (psiFile != null) try {
				for (PsiElement root : psiFile.getChildren()) {
					if (root instanceof JsonObject object) {
						for (JsonProperty property : object.getPropertyList()) {
							JsonValue value = property.getValue();
							switch (property.getName()) {
								case "types" -> this.expectArray(value, PendingType::new).forEach((PendingType type) -> {
									PendingType old = this.types.putIfAbsent(type.name, type);
									if (old != null) this.addError(type.element, "Type '" + type.name + "' is already defined.");
								});
								case "environments" -> this.expectArray(value, PendingEnvironment::new).forEach((PendingEnvironment environment) -> {
									PendingEnvironment old = this.environments.putIfAbsent(environment.name, environment);
									if (old != null) this.addError(environment.element, "Environment '" + environment.name + "' is already defined.");
								});
								case "script_schemas" -> this.expectArray(value, PendingSchema::new).forEach(this.schemas::add);
								case "references" -> this.expectArray(value, PendingReference::new).forEach(this.references::add);
								case "required_tags" -> this.expectArray(value, PendingRequiredTag::new).forEach(this.requiredTags::add);
								default -> this.addError(property.getNameElement(), "Expected one of: { types, environments, schemas }");
							}
						}
					}
				}
			}
			catch (Exception exception) {
				this.addError(psiFile, new PsiErrorDisplay(0, psiFile.getTextLength(), exception));
			}
		}
		return true;
	}

	public void addError(PsiElement element, String error) {
		if (element != null) {
			this.addError(element, new PsiErrorDisplay(0, element.getTextLength(), error));
		}
	}

	public void addError(PsiElement element, Throwable error) {
		if (element != null) {
			this.addError(element, new PsiErrorDisplay(0, element.getTextLength(), error));
		}
	}

	public void addError(PsiElement element, PsiErrorDisplay error) {
		if (element != null) {
			this.errors.computeIfAbsent(element, (PsiElement $) -> new ArrayList<>(1)).add(error);
		}
	}

	@SuppressWarnings("unchecked")
	public <T_Psi, T_Result> @Nullable T_Result expectType(PsiElement element, Class<T_Psi> psiClass, Function<? super T_Psi, ? extends T_Result> computer) {
		if (psiClass.isInstance(element)) {
			return computer.apply((T_Psi)(element));
		}
		else {
			this.addError(element, "Expected " + psiClass.getSimpleName());
			return null;
		}
	}

	public <T_Result> @Nullable T_Result expectString(PsiElement element, Function<? super String, ? extends T_Result> computer) {
		return this.expectType(element, JsonStringLiteral.class, computer.compose(JsonStringLiteral::getValue));
	}

	public @Nullable String expectString(PsiElement element) {
		return this.expectType(element, JsonStringLiteral.class, JsonStringLiteral::getValue);
	}

	public boolean expectBoolean(PsiElement element, boolean fallback) {
		Boolean bool = this.expectType(element, JsonBooleanLiteral.class, JsonBooleanLiteral::getValue);
		return bool != null ? bool.booleanValue() : fallback;
	}

	public Number expectNumber(PsiElement element, Number fallback) {
		Number number = this.expectType(element, JsonNumberLiteral.class, JsonNumberLiteral::getValue);
		return number != null ? number : fallback;
	}

	public TextAttributesKey expectColor(PsiElement element, TextAttributesKey fallback) {
		String colorName = this.expectString(element);
		TextAttributesKey color = IDENTIFIER_COLORS.get(colorName != null ? colorName : "");
		if (color != null) return color;
		this.addError(element, "Expected one of: " + IDENTIFIER_COLORS.keySet());
		return fallback;
	}

	public <T_Psi, T_Element> Stream<T_Element> expectArray(PsiElement element, Class<T_Psi> psiClass, Function<? super T_Psi, ? extends T_Element> computer) {
		if (element instanceof JsonArray array) {
			return array.getValueList().stream().map((JsonValue value) -> {
				return this.expectType(value, psiClass, computer);
			});
		}
		else {
			this.addError(element, "Expected array");
			return Stream.empty();
		}
	}

	public <T_Element> Stream<T_Element> expectArray(PsiElement array, BiFunction<PendingDataContext, PsiElement, T_Element> computer) {
		return this.expectArray(array, PsiElement.class, (PsiElement element) -> computer.apply(this, element));
	}
}