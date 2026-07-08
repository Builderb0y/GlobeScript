package builderb0y.globescript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix;
import com.intellij.codeInsight.daemon.quickFix.NewFileLocation;
import com.intellij.codeInsight.daemon.quickFix.TargetDirectory;
import com.intellij.json.psi.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.GsEnv;
import builderb0y.globescript.datadriven.PendingReference;
import builderb0y.globescript.datadriven.ReferenceModel;

public class TagReferencer extends PsiReferenceContributor {

	public static final Pattern TAG_START = Pattern.compile("/data/([a-z_.\\-]+)/tags/(bigglobe/(?:custom_class|worldgen/(?:feature_dispatcher|overrider))|block|fluid|item|worldgen/(?:biome|configured_feature|structure|structure_type))/[a-z_.\\-/]+\\.json$");

	@Override
	public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
		registrar.registerReferenceProvider(
			PlatformPatterns.psiElement(JsonStringLiteral.class),
			new PsiReferenceProvider() {

				@Override
				public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
					if (!(element instanceof JsonStringLiteral jsonElement) || jsonElement.isPropertyName()) return PsiReference.EMPTY_ARRAY;
					VirtualFile directory = element.getContainingFile().getOriginalFile().getVirtualFile();
					if (directory == null) return PsiReference.EMPTY_ARRAY;

					if (isTagEntry(jsonElement)) {
						Matcher matcher = TAG_START.matcher(directory.getPath());
						if (matcher.find()) {
							int start = matcher.start() + "/data".length();
							while (directory.getPath().length() > start) directory = directory.getParent();
							String registry = matcher.group(2);
							return new PsiReference[] { new TagReference(jsonElement, directory, registry, "minecraft", PendingReference.Type.EITHER) };
						}
					}
					GsEnv env = GsEnv.find(element.getContainingFile().getVirtualFile());
					if (env != null) {
						String filePath = directory.getPath();
						for (Map.Entry<Pattern, List<ReferenceModel>> entry : env.references.entrySet()) {
							Matcher matcher = entry.getKey().matcher(filePath);
							if (matcher.find()) {
								int start = matcher.start() + "/data".length();
								while (directory.getPath().length() > start) directory = directory.getParent();
								for (ReferenceModel model : entry.getValue()) {
									PsiElement startingPoint;
									if (
										(startingPoint = model.jsonPath.getRootFor(jsonElement)) != null &&
										model.when.test(startingPoint)
									) {
										return new PsiReference[] { new TagReference(jsonElement, directory, model.registry, model.defaultNamespace, model.type) };
									}
								}
							}
						}
					}
					return PsiReference.EMPTY_ARRAY;
				}
			}
		);
	}

	public static boolean isTagEntry(JsonStringLiteral element) {
		return (
			( //normal tag elements.
				element.getParent() instanceof JsonArray array &&
				array.getParent() instanceof JsonProperty property &&
				property.getName().equals("values")
			)
			||
			( //optional tag elements.
				element.getParent() instanceof JsonProperty id &&
				id.getName().equals("id") &&
				id.getParent() instanceof JsonObject object &&
				object.getParent() instanceof JsonArray theSameArray &&
				theSameArray.getParent() instanceof JsonProperty theSameProperty &&
				theSameProperty.getName().equals("values")
			)
		);
	}

	public static class TagReference extends PsiReferenceBase<JsonStringLiteral> {

		public final VirtualFile dataDirectory;
		public final String registry, defaultNamespace;
		public final PendingReference.Type type;

		public TagReference(
			@NotNull JsonStringLiteral element,
			VirtualFile directory,
			String registry,
			String defaultNamespace,
			PendingReference.Type type
		) {
			super(element);
			this.dataDirectory = directory;
			this.registry = registry;
			this.defaultNamespace = defaultNamespace;
			this.type = type;
		}

		public boolean isTag() {
			return this.myElement.getValue().startsWith("#");
		}

		public NewFileLocation getCreationLocation() {
			String elementID = this.myElement.getValue();
			boolean isTag = elementID.startsWith("#");
			int colon = elementID.indexOf(':');
			String namespace, path;
			if (colon >= 0) {
				namespace = elementID.substring(isTag ? 1 : 0, colon);
				path = elementID.substring(colon + 1);
			}
			else {
				namespace = this.defaultNamespace;
				path = elementID.substring(isTag ? 1 : 0);
			}
			List<String> list = new ArrayList<>();
			list.add(namespace);
			if (isTag) list.add("tags");
			Collections.addAll(list, this.registry.split("/"));
			Collections.addAll(list, path.split("/"));
			String name = list.removeLast() + ".json";
			return new NewFileLocation(
				Collections.singletonList(
					new TargetDirectory(
						PsiManager.getInstance(this.myElement.getProject()).findDirectory(this.dataDirectory)
					)
				),
				list.toArray(new String[list.size()]),
				name
			);
		}

		public Supplier<String> getCreationContents() {
			return (
				this.isTag()
				? () -> """
				{
					"replace": false,
					"values": [

					]
				}"""
				: () -> ""
			);
		}

		public CreateFilePathFix createQuickFix() {
			return new CreateFilePathFix(
				this.myElement,
				this.getCreationLocation(),
				this.getCreationContents()
			);
		}

		public String getErrorMessage() {
			boolean isTag = this.isTag();
			if (this.type == (isTag ? PendingReference.Type.ELEMENT : PendingReference.Type.TAG)) {
				return isTag ? "Must specify an element here, not a tag." : "Must specify a tag here, not an element.";
			}
			else {
				return isTag ? "Can't find this tag." : "Can't find this element.";
			}
		}

		@Override
		public @Nullable PsiElement resolve() {
			String elementID = this.myElement.getValue();
			boolean isTag = elementID.startsWith("#");
			int colon = elementID.indexOf(':');
			String elementNamespace, elementPath;
			if (colon >= 0) {
				elementNamespace = elementID.substring(isTag ? 1 : 0, colon);
				elementPath = elementID.substring(colon + 1);
			}
			else {
				elementNamespace = this.defaultNamespace;
				elementPath = elementID.substring(isTag ? 1 : 0);
			}
			String registry = this.registry;
			String path = elementNamespace + (isTag ? "/tags/" : "/") + registry + "/" + elementPath + ".json";
			VirtualFile found = this.dataDirectory.findFileByRelativePath(path);
			if (found != null) {
				return PsiManager.getInstance(this.myElement.getProject()).findFile(found);
			}
			return null;
		}

		@Override
		public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
			String oldName = this.myElement.getValue();
			int nameStart = oldName.lastIndexOf('/');
			if (nameStart < 0) nameStart = oldName.lastIndexOf(':');
			nameStart++;

			int nameEnd = newElementName.lastIndexOf('.');
			if (nameEnd < 0) nameEnd = newElementName.length();

			String finalName = oldName.substring(0, nameStart) + newElementName.substring(0, nameEnd);
			return super.handleElementRename(finalName);
		}

		@Override
		public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
			return super.bindToElement(element);
		}
	}
}