package builderb0y.globescript;

import java.util.List;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class GlobeScriptAnnotator implements Annotator {

	public void annotate(ExpressionParser parser, AnnotationHolder holder) {
		parser.parseEntireInput().forEach((Token token) -> {
			AnnotationBuilder builder;
			if (token.color != null) {
				if (token.tooltip != null) {
					builder = holder.newAnnotation(HighlightSeverity.ERROR, token.tooltip);
				}
				else {
					builder = holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES);
				}
				builder.range(token.range).textAttributes(token.color).create();
			}
		});
		parser.reader.comments.forEach((Token token) -> {
			holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).range(token.range).textAttributes(token.color).create();
		});
	}

	public static class RawAnnotator extends GlobeScriptAnnotator {

		@Override
		public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
			String text = element.getText();
			ExpressionParser parser = new ExpressionParser(
				new ExpressionReader(text, 0, text.length()),
				new ScriptEnvironment().addEnvironment(ScriptEnvironment.BUILTIN)
			);
			this.annotate(parser, holder);
		}
	}

	public static class JsonAnnotator extends GlobeScriptAnnotator {

		@Override
		public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
			int start, end;
			if (element instanceof JsonArray array) {
				List<JsonValue> values = array.getValueList();
				if (values.isEmpty()) return;
				for (JsonValue value : values) {
					if (!(value instanceof JsonStringLiteral)) return;
				}
				start = values.getFirst().getTextOffset() + 1;
				end = values.getLast().getTextRange().getEndOffset() - 1;
			}
			else if (element instanceof JsonStringLiteral string && !string.isPropertyName() && !(string.getParent() instanceof JsonArray)) {
				start = element.getTextOffset() + 1;
				end = element.getTextRange().getEndOffset() - 1;
			}
			else {
				return;
			}
			String text = element.getContainingFile().getText();
			ExpressionParser parser = new ExpressionParser(
				new JsonExpressionReader(text, start, end),
				new ScriptEnvironment().addEnvironment(ScriptEnvironment.BUILTIN)
			);
			this.annotate(parser, holder);
		}
	}
}