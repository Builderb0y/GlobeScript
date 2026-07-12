package builderb0y.globescript;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.google.common.base.Predicates;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.json.psi.*;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.StructureParser.Structure;
import builderb0y.globescript.TagReferencer.TagReference;
import builderb0y.globescript.datadriven.*;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.datadriven.PendingElement.ShorthandParser;
import builderb0y.globescript.datadriven.PendingEnvironment.*;

public abstract class GlobeScriptAnnotator implements Annotator {

	public void annotate(ExpressionParser parser, AnnotationHolder holder) {
		parser.parseEntireInput().forEach((Token token) -> this.annotate(token, holder));
		parser.reader.comments.forEach((Token token) -> {
			holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(token.range).textAttributes(token.color).create();
		});
	}

	public void annotate(Token token, AnnotationHolder holder) {
		if (token.info.type() == RawTypeModel.UNKNOWN) {
			holder.newAnnotation(HighlightSeverity.ERROR, "Parser failed to assign info to " + token.getText() + "! This is a bug!").range(token.range).textAttributes(Colors.ERROR).create();
		}
		else {
			if (token.color != null && !token.range.isEmpty()) {
				holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(token.range).textAttributes(token.color).create();
			}
			for (PsiErrorDisplay error : token.tooltips) {
				error.addTo(null, holder);
			}
		}
	}

	public static class RawAnnotator extends GlobeScriptAnnotator {

		@Override
		public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
			if (!(element instanceof PsiFile)) {
				VirtualFile file = element.getContainingFile().getVirtualFile();
				ProjectData data = ProjectData.find(file);
				if (data != null) {
					String text = element.getText();
					ExpressionParser parser = new ExpressionParser(
						new ExpressionReader(text, 0, text.length()),
						data.combineAllEnvironments(file)
					);
					this.annotate(parser, holder);

				}
			}
		}
	}

	public static class JsonAnnotator extends GlobeScriptAnnotator {

		public static final Pattern SCRIPT_TEMPLATE_PATH = Pattern.compile("/data/[^/]+/bigglobe/script_template/");

		public boolean annotateEnvironment(JsonElement element, AnnotationHolder holder, GsEnv metadata) {
			List<PsiErrorDisplay> errors = metadata.errors.get(element);
			if (errors != null) {
				for (PsiErrorDisplay error : errors) {
					error.addTo(element, holder);
				}
			}
			if (element instanceof JsonStringLiteral string) {
				if (!string.isPropertyName()) {
					ShorthandParser<?, ?> parser = switch (getPathNoArrays(string)) {
						case "types"                         -> PendingType       .Shorthand.PARSER;
						case "environments>types"            -> PendingExposedType.Shorthand.PARSER;
						case "environments>variables"        -> PendingVariable   .Shorthand.PARSER;
						case "environments>instance_fields"  -> PendingField      .Shorthand.INSTANCE_PARSER;
						case "environments>static_fields"    -> PendingField      .Shorthand.STATIC_PARSER;
						case "environments>functions"        -> PendingFunction   .Shorthand.PARSER;
						case "environments>instance_methods" -> PendingMethod     .Shorthand.INSTANCE_PARSER;
						case "environments>static_methods"   -> PendingMethod     .Shorthand.STATIC_PARSER;
						case "environments>casters"          -> PendingCaster     .Shorthand.PARSER;
						case "environments>imported_values"  -> PendingVariable   .Shorthand.PARSER;
						default -> null;
					};
					exit:
					if (parser != null) {
						String fileText = element.getContainingFile().getText();
						JsonExpressionReader reader = new JsonExpressionReader(
							fileText,
							maybeIncrement(fileText, element.getTextRange().getStartOffset()),
							maybeDecrement(fileText, element.getTextRange().getEndOffset())
						);
						Structure structure;
						try {
							structure = parser.parseAndCheckEOF(reader);
						}
						catch (RuntimeException exception) {
							break exit;
						}
						Consumer<Token> action = (Token token) -> {
							this.annotate(token, holder);
						};
						structure.group().forEach(action);
						reader.comments.forEach(action);
						return true;
					}
				}
			}
			return false;
		}

		public boolean checkInvalidReferences(JsonElement element, AnnotationHolder holder) {
			for (PsiReference reference : element.getReferences()) {
				if (reference instanceof TagReference myReference && myReference.resolve() == null) {
					holder
					.newAnnotation(HighlightSeverity.ERROR, myReference.getErrorMessage())
					.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
					.withFix(myReference.createQuickFix())
					.create();
					return true;
				}
			}
			return false;
		}

		public boolean checkRequiredTags(JsonFile file, AnnotationHolder holder, GsEnv metadata) {
			JsonValue top = file.getTopLevelValue();
			String path = file.getOriginalFile().getVirtualFile().getPath();
			for (RequiredTagModel requiredTag : metadata.requiredTags) {
				if (requiredTag.filePath.matcher(path).find() && requiredTag.when.test(top)) {
					if (ReferencesSearch.search(file).anyMatch(Predicates.alwaysTrue())) {
						break;
					}
					else {
						holder.newAnnotation(HighlightSeverity.WARNING, "This file must be added to a tag to function properly.").fileLevel().create();
						return true;
					}
				}
			}
			return false;
		}

		public static TextRange getArrayRange(JsonElement jsonElement) {
			String fileText = jsonElement.getContainingFile().getText();
			int start, end;
			if (jsonElement instanceof JsonArray array) {
				List<JsonValue> values = array.getValueList();
				if (values.isEmpty()) return null;
				for (JsonValue value : values) {
					if (!(value instanceof JsonStringLiteral)) return null;
				}
				start = maybeIncrement(fileText, values.getFirst().getTextOffset());
				end = maybeDecrement(fileText, values.getLast().getTextRange().getEndOffset());
			}
			else if (jsonElement instanceof JsonStringLiteral string && !string.isPropertyName() && !(string.getParent() instanceof JsonArray)) {
				start = maybeIncrement(fileText, jsonElement.getTextOffset());
				end = maybeDecrement(fileText, jsonElement.getTextRange().getEndOffset());
			}
			else {
				return null;
			}
			return end > start ? new TextRange(start, end) : null;
		}

		public boolean annotateScript(JsonElement jsonElement, AnnotationHolder holder, GsEnv metadata) {
			TextRange range = getArrayRange(jsonElement);
			if (range != null) {
				PsiFile psiFile = jsonElement.getContainingFile();
				VirtualFile virtualFile = psiFile.getVirtualFile();
				String filePath = virtualFile.getPath();
				for (SchemaModel schema : metadata.schemas) {
					if (schema.matches(filePath, jsonElement)) {
						String text = psiFile.getText();
						ExpressionParser parser = new ExpressionParser(
							new JsonExpressionReader(text, range.getStartOffset(), range.getEndOffset()),
							schema.copyEnvironment(metadata.standardTypes, virtualFile)
						);
						this.annotate(parser, holder);
						return true;
					}
				}
			}
			return false;
		}

		public void grayOutQuotes(JsonElement element, AnnotationHolder holder) {
			String fileText = element.getContainingFile().getText();
			element.accept(new PsiElementVisitor() {

				@Override
				public void visitElement(@NotNull PsiElement element) {
					super.visitElement(element);
					TextRange range = element.getTextRange();
					int start = range.getStartOffset();
					int end = range.getEndOffset();
					if (element instanceof JsonStringLiteral) {
						if (fileText.charAt(start) == '"') {
							holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(new TextRange(start, start + 1)).textAttributes(Colors.JSON_HIDDEN).create();
						}
						if (fileText.charAt(end - 1) == '"') {
							holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(new TextRange(end - 1, end)).textAttributes(Colors.JSON_HIDDEN).create();
						}
					}
					else if (element.getTextLength() == 1 && fileText.charAt(start) == ',') {
						holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(new TextRange(start, start + 1)).textAttributes(Colors.JSON_HIDDEN).create();
					}
					else {
						element.acceptChildren(this);
					}
				}
			});
		}

		@Override
		public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
			if (!(element instanceof JsonElement jsonElement)) return;

			VirtualFile file = element.getContainingFile().getVirtualFile();
			ProjectData projectData = ProjectData.find(file);
			GsEnv metadata = projectData != null ? projectData.environment() : null;
			if (jsonElement instanceof JsonFile jsonFile) {
				if (metadata != null) {
					if (this.checkRequiredTags(jsonFile, holder, metadata)) return;
				}
			}
			else {
				if (this.checkInvalidReferences(jsonElement, holder)) return;
				boolean annotated;
				VirtualFile envFolder;
				if (metadata != null) {
					if (
						(envFolder = metadata.envFolder()) != null &&
						VfsUtil.isAncestor(envFolder, file, true)
					) {
						annotated = this.annotateEnvironment(jsonElement, holder, metadata);
					}
					else {
						annotated = this.annotateScript(jsonElement, holder, metadata);
					}
				}
				else {
					annotated = false;
				}
				if (annotated) this.grayOutQuotes(jsonElement, holder);
			}
			/*
			if (SCRIPT_TEMPLATE_PATH.matcher(filePath).find()) {
				if (jsonPath.equals("script")) {
					ScriptEnvironment environment = new ScriptEnvironment(
						context.standardTypes,
						context.environments.values().toArray(
							new EnvironmentModel[context.environments.size()]
						)
					);
					if (root instanceof JsonObject object && getValue(object, "inputs") instanceof JsonArray inputs) {
						for (JsonValue input : inputs.getValueList()) {
							if (input instanceof JsonObject inputObject) {
								String name = getValue(inputObject, "name") instanceof JsonStringLiteral string ? string.getValue() : null;
								if (name == null) continue;
								String typeName = getValue(inputObject, "type") instanceof JsonStringLiteral string ? string.getValue() : null;
								if (typeName == null) continue;
								TypeData typeData = environment.getType(typeName);
								if (typeData == null) continue;
								environment.addUserParameter(name, typeData.info.assignable(true));
							}
						}
					}
					ExpressionParser parser = new ExpressionParser(
						new JsonExpressionReader(text, start, end),
						environment
					);
					this.annotate(parser, holder);
					annotated = true;
				}
			}
			*/
		}
	}

	public static int maybeIncrement(CharSequence text, int index) {
		return text.charAt(index) == '"' ? index + 1 : index;
	}

	public static int maybeDecrement(CharSequence text, int index) {
		return text.charAt(index - 1) == '"' ? index - 1 : index;
	}

	public static String getPath(JsonElement element) {
		StringBuilder builder = new StringBuilder(32);
		appendPath(element, builder);
		return builder.toString();
	}

	public static void appendPath(PsiElement element, StringBuilder builder) {
		PsiElement parent = element.getParent();
		if (parent instanceof JsonProperty property) {
			appendPath(parent.getParent(), builder);
			if (!builder.isEmpty()) builder.append('>');
			builder.append(property.getName());
		}
		else if (parent instanceof JsonArray array) {
			appendPath(array, builder);
			if (!builder.isEmpty()) builder.append('>');
			builder.append(array.getValueList().indexOf(element));
		}
	}

	public static String getPathNoArrays(JsonElement element) {
		StringBuilder builder = new StringBuilder(32);
		appendPathNoArrays(element, builder);
		return builder.toString();
	}

	public static void appendPathNoArrays(PsiElement element, StringBuilder builder) {
		PsiElement parent = element.getParent();
		if (parent instanceof JsonProperty property) {
			appendPathNoArrays(parent.getParent(), builder);
			if (!builder.isEmpty()) builder.append('>');
			builder.append(property.getName());
		}
		else if (parent instanceof JsonArray array) {
			appendPathNoArrays(array, builder);
		}
	}
}