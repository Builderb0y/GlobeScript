package builderb0y.globescript;

import java.util.List;
import java.util.Map;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.datadriven.*;
import builderb0y.globescript.datadriven.ReferenceModel.TargetModel;
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
					PackData providedPack = pack.projectData.getProvidedDataPack();
					UID uid = pack.identify(containingFile);
					if (uid != null) {
						if (uid.tag()) {
							this.search(text, pack, new TargetModel(uid.registry(), null, null), PendingReference.Type.EITHER, results);
							if (providedPack != null) this.search(text, providedPack, new TargetModel(uid.registry(), null, null), PendingReference.Type.EITHER, results);
						}
						else {
							for (Map.Entry<ID, List<ReferenceModel>> entry : pack.projectData.environment().references.entrySet()) {
								if (entry.getKey().registryEquals(uid)) {
									for (ReferenceModel model : entry.getValue()) {
										PsiElement startingPoint = model.reference.jsonPath.getRootFor(jsonString);
										if (startingPoint != null && (model.reference.condition == null || model.reference.condition.test(startingPoint))) {
											this.search(text, pack, model.declaration, model.type, results);
											if (providedPack != null) this.search(text, providedPack, model.declaration, model.type, results);
										}
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
			if (referenceType.elementsAllowed()) {
				target.registry.walkJsonRegistry(pack.dataFolder, false, (VirtualFile file, ID id) -> {
					PsiFile psiFile = PsiManager.getInstance(pack.projectData.project).findFile(file);
					if (target.condition != null) {
						if (!(psiFile instanceof JsonFile jsonFile) || !target.condition.test(jsonFile.getTopLevelValue())) {
							return;
						}
					}
					String identifier = id.toString();
					if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
						results.addElement(LookupElementBuilder.create(identifier).withPsiElement(psiFile));
					}
				});
			}
			if (referenceType.tagsAllowed()) {
				target.registry.walkJsonRegistry(pack.dataFolder, true, (VirtualFile file, ID id) -> {
					String identifier = id.toTagString();
					if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
						PsiFile psiFile = PsiManager.getInstance(pack.projectData.project).findFile(file);
						results.addElement(LookupElementBuilder.create(identifier).withPsiElement(psiFile));
					}
				});
			}
		}
	}
}