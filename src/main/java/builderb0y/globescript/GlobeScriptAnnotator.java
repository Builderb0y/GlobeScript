package builderb0y.globescript;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.google.common.base.Predicates;
import com.intellij.codeInsight.daemon.quickFix.CreateFilePathFix;
import com.intellij.codeInsight.daemon.quickFix.NewFileLocation;
import com.intellij.codeInsight.daemon.quickFix.TargetDirectory;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.json.psi.*;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
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
			if (token.color != null) {
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
				Module module = ModuleUtil.findModuleForPsiElement(element);
				if (module != null) {
					DataContext context = DataContext.getOrCreateInstance(module);
					if (context.isValid()) {
						String text = element.getText();
						ExpressionParser parser = new ExpressionParser(
							new ExpressionReader(text, 0, text.length()),
							new ScriptEnvironment(
								context.standardTypes,
								context.environments.values().toArray(
									new EnvironmentModel[context.environments.size()]
								)
							)
						);
						this.annotate(parser, holder);
					}
				}
			}
		}
	}

	public static class JsonAnnotator extends GlobeScriptAnnotator {

		public static final Pattern SCRIPT_TEMPLATE_PATH = Pattern.compile("/data/[^/]+/bigglobe_script_templates/");

		@Override
		public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
			if (!(element instanceof JsonElement jsonElement)) return;
			for (PsiReference reference : jsonElement.getReferences()) {
				if (reference instanceof TagReference myReference && myReference.resolve() == null) {
					holder
					.newAnnotation(HighlightSeverity.ERROR, myReference.getErrorMessage())
					.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
					.withFix(myReference.createQuickFix())
					.create();
					return;
				}
			}
			String fileText = jsonElement.getContainingFile().getText();
			String filePath = jsonElement.getContainingFile().getVirtualFile().getPath();
			boolean isEnvironment = filePath.contains("/gs_env/");
			boolean annotated = false;
			if (isEnvironment) {
				if (jsonElement instanceof JsonStringLiteral string && !string.isPropertyName()) {
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
						default                              -> null;
					};
					exit:
					if (parser != null) {
						JsonExpressionReader reader = new JsonExpressionReader(
							fileText,
							maybeIncrement(fileText, jsonElement.getTextRange().getStartOffset()),
							maybeDecrement(fileText, jsonElement.getTextRange().getEndOffset())
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
						annotated = true;
					}
				}
			}
			Module module = ModuleUtil.findModuleForPsiElement(jsonElement);
			if (module != null) {
				DataContext context = DataContext.getOrCreateInstance(module);
				if (!isEnvironment && context.isValid()) {
					if (jsonElement instanceof JsonFile file) {
						JsonValue top = file.getTopLevelValue();
						String path = file.getOriginalFile().getVirtualFile().getPath();
						for (RequiredTagModel requiredTag : context.requiredTags) {
							if (requiredTag.filePath.matcher(path).find() && requiredTag.when.test(top)) {
								if (ReferencesSearch.search(file).anyMatch(Predicates.alwaysTrue())) {
									break;
								}
								else {
									holder.newAnnotation(HighlightSeverity.WARNING, "This file must be added to a tag to function properly.").fileLevel().create();
									return;
								}
							}
						}
					}
					int start, end;
					if (jsonElement instanceof JsonArray array) {
						List<JsonValue> values = array.getValueList();
						if (values.isEmpty()) return;
						for (JsonValue value : values) {
							if (!(value instanceof JsonStringLiteral)) return;
						}
						start = maybeIncrement(fileText, values.getFirst().getTextOffset());
						end = maybeDecrement(fileText, values.getLast().getTextRange().getEndOffset());
					}
					else if (jsonElement instanceof JsonStringLiteral string && !string.isPropertyName() && !(string.getParent() instanceof JsonArray)) {
						start = maybeIncrement(fileText, jsonElement.getTextOffset());
						end = maybeDecrement(fileText, jsonElement.getTextRange().getEndOffset());
					}
					else {
						return;
					}

					JsonElement root = jsonElement;
					String jsonPath = getPath(root);
					while (root.getParent() instanceof JsonElement parent && !(parent instanceof PsiFile)) {
						root = parent;
					}
					String text = jsonElement.getContainingFile().getText();
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
					else {
						for (SchemaModel schema : context.schemas) {
							if (schema.matches(filePath, jsonElement)) {
								ExpressionParser parser = new ExpressionParser(
									new JsonExpressionReader(text, start, end),
									schema.copyEnvironment(context.standardTypes)
								);
								this.annotate(parser, holder);
								annotated = true;
								break;
							}
						}
					}
				}
				else {
					List<PsiErrorDisplay> errors = context.errors.get(jsonElement);
					if (errors != null) {
						for (PsiErrorDisplay error : errors) {
							error.addTo(jsonElement, holder);
						}
					}
				}
			}
			if (annotated) {
				jsonElement.accept(new PsiElementVisitor() {

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
		}
	}

	public static int maybeIncrement(CharSequence text, int index) {
		return text.charAt(index) == '"' ? index + 1 : index;
	}

	public static int maybeDecrement(CharSequence text, int index) {
		return text.charAt(index - 1) == '"' ? index - 1 : index;
	}

	public static JsonValue getValue(JsonObject object, String name) {
		JsonProperty property = object.findProperty(name);
		return property != null ? property.getValue() : null;
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