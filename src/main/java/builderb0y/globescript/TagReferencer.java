package builderb0y.globescript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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

import builderb0y.globescript.datadriven.*;

public class TagReferencer extends PsiReferenceContributor {

	@Override
	public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
		registrar.registerReferenceProvider(
			PlatformPatterns.psiElement(JsonStringLiteral.class),
			new PsiReferenceProvider() {

				@Override
				public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
					if (!(element instanceof JsonStringLiteral jsonElement) || jsonElement.isPropertyName()) return PsiReference.EMPTY_ARRAY;
					VirtualFile containingFile = element.getContainingFile().getOriginalFile().getVirtualFile();
					if (containingFile == null) return PsiReference.EMPTY_ARRAY;
					PackData pack = PackData.find(containingFile);
					if (pack == null) return PsiReference.EMPTY_ARRAY;
					UID uid = pack.identify(containingFile);
					if (uid == null) return PsiReference.EMPTY_ARRAY;
					if (uid.tag()) {
						if (isTagEntry(jsonElement)) {
							String registryPath = (
								uid.registry().namespace().equals("minecraft")
								? uid.registry().path()
								: uid.registry().namespace() + "/" + uid.registry().path()
							);
							return new PsiReference[] { new TagReference(jsonElement, pack.dataFolder, registryPath, "minecraft", PendingReference.Type.EITHER) };
						}
						else {
							return PsiReference.EMPTY_ARRAY;
						}
					}
					else {
						List<PsiReference> result = new ArrayList<>();
						for (ReferenceModel model : pack.projectData.environment().references) {
							if (model.reference.registry.equals(uid.registry())) {
								PsiElement startingPoint = model.reference.jsonPath.getRootFor(jsonElement);
								if (startingPoint != null && (model.reference.condition == null || model.reference.condition.test(startingPoint))) {
									String registryPath = (
										model.declaration.registry.namespace().equals("minecraft")
										? model.declaration.registry.path()
										: model.declaration.registry.namespace() + "/" + model.declaration.registry.path()
									);
									result.add(new TagReference(jsonElement, pack.dataFolder, registryPath, model.defaultNamespace, model.type));
								}
							}
						}
						return result.toArray(PsiReference.EMPTY_ARRAY);
					}
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
			VirtualFile dataFolder,
			String registry,
			String defaultNamespace,
			PendingReference.Type type
		) {
			super(element);
			this.dataDirectory = dataFolder;
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
			UID uid = PackData.identify(this.dataDirectory, element.getContainingFile().getOriginalFile().getVirtualFile());
			if (uid != null) {
				return super.handleElementRename(uid.formatElement());
			}
			else {
				return this.myElement;
			}
		}
	}
}