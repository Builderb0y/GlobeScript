package builderb0y.globescript;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.json.psi.JsonElement;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.datadriven.GsEnv;
import builderb0y.globescript.datadriven.PendingReference;
import builderb0y.globescript.datadriven.PendingSchema.When;
import builderb0y.globescript.datadriven.ReferenceModel;
import builderb0y.globescript.util.StringSimilarity;

public class TagCompleter extends CompletionContributor {

	public TagCompleter() {
		this.extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement().withSuperParent(1, JsonStringLiteral.class),
			new Provider()
		);
	}

	@Override
	public void beforeCompletion(@NotNull CompletionInitializationContext context) {
		super.beforeCompletion(context);
	}

	public static class Provider extends CompletionProvider<CompletionParameters> {

		@Override
		public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet results) {
			PsiElement basicString = parameters.getPosition();
			if (!(basicString.getParent() instanceof JsonStringLiteral jsonString)) return;
			VirtualFile directory = parameters.getOriginalFile().getVirtualFile();
			if (directory != null && !jsonString.isPropertyName()) {
				String text = parameters.getPosition().getText();
				{
					int start = text.startsWith("\"") ? 1 : 0;
					int end = parameters.getOffset() - parameters.getPosition().getTextOffset();
					text = text.substring(start, end);
				}
				if (TagReferencer.isTagEntry(jsonString)) {
					Matcher matcher = TagReferencer.TAG_START.matcher(directory.getPath());
					if (matcher.find()) {
						int start = matcher.start() + "/data".length();
						while (directory.getPath().length() > start) directory = directory.getParent();
						String registry = matcher.group(2);
						this.search(text, registry, directory, results, jsonString.getProject(), PendingReference.Type.EITHER, null);
					}
				}
				String filePath = directory.getPath();
				JsonElement root = jsonString;
				while (root.getParent() instanceof JsonElement parent && !(parent instanceof PsiFile)) {
					root = parent;
				}
				GsEnv metadata = GsEnv.find(directory);
				if (metadata != null) for (Map.Entry<Pattern, List<ReferenceModel>> entry : metadata.references.entrySet()) {
					Matcher matcher = entry.getKey().matcher(filePath);
					if (matcher.find()) {
						int start = matcher.start() + "/data".length();
						while (directory.getPath().length() > start) directory = directory.getParent();
						for (ReferenceModel model : entry.getValue()) {
							PsiElement startingPoint;
							if (
								(startingPoint = model.jsonPath.getRootFor(jsonString)) != null &&
								model.when.test(startingPoint)
							) {
								this.search(text, model.registry, directory, results, jsonString.getProject(), model.type, model.filter);
							}
						}
					}
				}
			}
		}

		public void search(String text, String registry, VirtualFile dataFolder, CompletionResultSet results, Project project, PendingReference.Type type, When filter) {
			for (VirtualFile namespace : dataFolder.getChildren()) {
				if (type != PendingReference.Type.TAG) {
					VirtualFile elements = namespace.findFileByRelativePath(registry);
					if (elements != null) {
						VfsUtilCore.iterateChildrenRecursively(
							elements,
							(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
							(VirtualFile fileOrDir) -> {
								if (!fileOrDir.isDirectory()) {
									if (filter != null) {
										PsiFile psiFile = PsiManager.getInstance(project).findFile(fileOrDir);
										if (!(psiFile instanceof JsonFile jsonFile) || !filter.test(jsonFile.getTopLevelValue())) {
											return true;
										}
									}
									String path = fileOrDir.getPath();
									int start = elements.getPath().length() + 1;
									int end = path.length() - ".json".length();
									path = path.substring(start, end);
									String identifier = namespace.getName() + ":" + path;
									if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
										results.addElement(LookupElementBuilder.create(identifier).withPsiElement(PsiManager.getInstance(project).findFile(fileOrDir)));
									}
								}
								return true;
							}
						);
					}
				}

				if (type != PendingReference.Type.ELEMENT) {
					VirtualFile tags = namespace.findChild("tags");
					if (tags != null) {
						tags = tags.findFileByRelativePath(registry);
						if (tags != null) {
							final VirtualFile tags_ = tags;
							VfsUtilCore.iterateChildrenRecursively(
								tags_,
								(VirtualFile file) -> file.isDirectory() || "json".equals(file.getExtension()),
								(VirtualFile fileOrDir) -> {
									if (!fileOrDir.isDirectory()) {
										String path = fileOrDir.getPath();
										int start = tags_.getPath().length() + 1;
										int end = path.length() - ".json".length();
										path = path.substring(start, end);
										String identifier = "#" + namespace.getName() + ":" + path;
										if (StringSimilarity.compare(text, identifier).compareTo(StringSimilarity.NO_MATCH) > 0) {
											results.addElement(LookupElementBuilder.create(identifier).withPsiElement(PsiManager.getInstance(project).findFile(fileOrDir)));
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