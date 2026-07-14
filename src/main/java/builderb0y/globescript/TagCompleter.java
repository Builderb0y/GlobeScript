package builderb0y.globescript;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.datadriven.PackData;
import builderb0y.globescript.datadriven.PendingReference;
import builderb0y.globescript.datadriven.ReferenceModel;
import builderb0y.globescript.datadriven.ReferenceModel.TargetModel;
import builderb0y.globescript.datadriven.UID;
import builderb0y.globescript.util.StringSimilarity;

public class TagCompleter extends CompletionContributor {

	public TagCompleter() {
		this.extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().withSuperParent(1, JsonStringLiteral.class),
			new Provider()
		);
	}

	public static class Provider extends CompletionProvider<CompletionParameters> {

		@Override
		public void addCompletions(
			@NotNull CompletionParameters parameters,
			@NotNull ProcessingContext context,
			@NotNull CompletionResultSet results
		) {
			if (!(parameters.getPosition().getParent() instanceof JsonStringLiteral jsonString) || jsonString.isPropertyName()) {
				return;
			}
			String text = parameters.getPosition().getText();
			{
				int start = text.startsWith("\"") ? 1 : 0;
				int end = parameters.getOffset() - parameters.getPosition().getTextOffset();
				text = text.substring(start, end);
			}
			VirtualFile containingFile = parameters.getOriginalFile().getVirtualFile();
			if (containingFile != null) {
				PackData pack = PackData.find(containingFile);
				if (pack != null) {
					UID uid = pack.identify(containingFile);
					if (uid != null) {
						if (uid.tag()) {
							this.search(text, pack, new TargetModel(uid.registry(), null, null), PendingReference.Type.EITHER, results);
						}
						else {
							for (ReferenceModel reference : pack.projectData.environment().references) {
								if (reference.reference.registry.equals(uid.registry())) {
									PsiElement startingPoint = reference.reference.jsonPath.getRootFor(jsonString);
									if (startingPoint != null && (reference.reference.condition == null || reference.reference.condition.test(startingPoint))) {
										this.search(text, pack, reference.declaration, reference.type, results);
									}
								}
							}
						}
					}
				}
			}
		}

		public void search(
			String text,
			PackData pack,
			TargetModel target,
			PendingReference.Type referenceType,
			CompletionResultSet results
		) {
			String registryPath = (
				target.registry.namespace().equals("minecraft")
				? target.registry.path()
				: target.registry.namespace() + "/" + target.registry.path()
			);
			for (VirtualFile namespace : VfsUtil.getChildren(pack.dataFolder)) {
				if (referenceType.elementsAllowed()) {
					VirtualFile elements = namespace.findFileByRelativePath(registryPath);
					if (elements != null) {
						VfsUtilCore.iterateChildrenRecursively(
							elements,
							(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
							(VirtualFile fileOrDir) -> {
								if (!fileOrDir.isDirectory()) {
									PsiFile psiFile = PsiManager.getInstance(pack.projectData.project).findFile(fileOrDir);
									if (target.condition != null) {
										if (!(psiFile instanceof JsonFile jsonFile) || !target.condition.test(jsonFile.getTopLevelValue())) {
											return true;
										}
									}
									String path = VfsUtilCore.getRelativePath(fileOrDir, elements);
									path = path.substring(0, path.length() - ".json".length());
									String identifier = namespace.getName() + ":" + path;
									if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
										results.addElement(LookupElementBuilder.create(identifier).withPsiElement(psiFile));
									}
								}
								return true;
							}
						);
					}
				}
				if (referenceType.tagsAllowed()) {
					VirtualFile allTags = namespace.findChild("tags");
					if (allTags != null) {
						VirtualFile specificTags = allTags.findFileByRelativePath(registryPath);
						if (specificTags != null) {
							VfsUtilCore.iterateChildrenRecursively(
								specificTags,
								(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
								(VirtualFile fileOrDir) -> {
									if (!fileOrDir.isDirectory()) {
										String path = VfsUtilCore.getRelativePath(fileOrDir, specificTags);
										path = path.substring(0, path.length() - ".json".length());
										String identifier = "#" + namespace.getName() + ":" + path;
										if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
											PsiFile psiFile = PsiManager.getInstance(pack.projectData.project).findFile(fileOrDir);
											results.addElement(LookupElementBuilder.create(identifier).withPsiElement(psiFile));
										}
									}
									return true;
								}
							);
						}
					}
				}
			}
		}
	}
}