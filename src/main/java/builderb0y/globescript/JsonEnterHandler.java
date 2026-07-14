package builderb0y.globescript;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.EnterHandler;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.json.JsonLanguage;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.util.Util;

public class JsonEnterHandler implements EnterHandlerDelegate {

	@Override
	public Result preprocessEnter(
		@NotNull PsiFile psiFile,
		@NotNull Editor editor,
		@NotNull Ref<Integer> caretOffset,
		@NotNull Ref<Integer> caretAdvance,
		@NotNull DataContext dataContext,
		@Nullable EditorActionHandler originalHandler
	) {
		if (EnterHandler.getLanguage(dataContext) instanceof JsonLanguage) {
			int pos = caretOffset.get();
			PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
			manager.commitDocument(editor.getDocument());
			manager.doPostponedOperationsAndUnblockDocument(editor.getDocument());
			PsiElement element = psiFile.findElementAt(pos);
			if (element != null) element = element.getParent();
			if (element instanceof JsonStringLiteral string && !string.isPropertyName()) {
				IndentOptions indentOptions = CodeStyle.getIndentOptions(psiFile);
				String tab = indentOptions.USE_TAB_CHARACTER ? "\t" : " ".repeat(indentOptions.INDENT_SIZE);
				boolean isArray;
				Caret caret = editor.getCaretModel().getCurrentCaret();
				if (string.getParent() instanceof JsonProperty) {
					ensureArray(editor.getDocument(), caret, string, tab);
					isArray = true;
				}
				else {
					isArray = string.getParent() instanceof JsonArray;
				}
				if (isArray) {
					char atCaret = editor.getDocument().getImmutableCharSequence().charAt(caret.getOffset());
					insertNewLine(editor, editor.getDocument(), caret, tab, true, atCaret == ')' || atCaret == ']' || atCaret == '}' ? -1 : 0);
					return Result.Stop;
				}
			}
		}
		return Result.Continue;
	}

	public static void insertNewLine(Editor editor, Document document, Caret caret, String tab, boolean isJson, int extraIndent) {
		CharSequence documentText = document.getImmutableCharSequence();
		int pos = caret.getOffset();
		int lineStart = DocumentUtil.getLineStartOffset(pos, document);
		int indentEnd = DocumentUtil.getFirstNonSpaceCharOffset(document, lineStart, documentText.length());
		StringBuilder toInsert = (
			new StringBuilder(16)
			.append(isJson ? "\",\n" : "\n")
			.append(documentText, lineStart, indentEnd)
		);
		int parentheses = 0;
		for (int offset = indentEnd; ++offset < pos;) {
			switch (documentText.charAt(offset)) {
				case '(', '[', '{' -> parentheses++;
				case ')', ']', '}' -> parentheses--;
			}
		}
		if (parentheses > 0) {
			extraIndent++;
		}
		if (extraIndent > 0) {
			toInsert.append(tab.repeat(extraIndent));
		}
		else while (extraIndent < 0) {
			if (Util.regionMatches(toInsert, toInsert.length() - tab.length(), tab, 0, tab.length())) {
				toInsert.setLength(toInsert.length() - tab.length());
				extraIndent++;
			}
			else {
				break;
			}
		}
		if (isJson) toInsert.append('"');
		document.insertString(pos, toInsert);
		caret.moveToOffset(pos + toInsert.length());
		if (editor != null) EditorModificationUtil.scrollToCaret(editor);
	}

	public static void ensureArray(Document document, Caret caret, JsonStringLiteral string, String tab) {
		int pos = caret.getOffset();
		int lineStart = DocumentUtil.getLineStartOffset(string.getTextOffset(), document);
		int indentEnd = DocumentUtil.getFirstNonSpaceCharOffset(document, lineStart, string.getTextOffset());
		CharSequence documentText = document.getImmutableCharSequence();
		StringBuilder replacement = (
			new StringBuilder(string.getTextLength() + 64)
			.append("[\n")
			.append(documentText, lineStart, indentEnd)
			.append(tab)
			.append(documentText, string.getTextOffset(), pos)
		);
		int newCaretPos = string.getTextOffset() + replacement.length();
		replacement
		.append(documentText, pos, string.getTextOffset() + string.getTextLength())
		.append('\n')
		.append(documentText, lineStart, indentEnd)
		.append(']');
		document.replaceString(string.getTextOffset(), string.getTextOffset() + string.getTextLength(), replacement);
		caret.moveToOffset(newCaretPos);
	}
}