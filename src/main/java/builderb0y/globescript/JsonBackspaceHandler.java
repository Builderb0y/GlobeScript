package builderb0y.globescript;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import org.jetbrains.annotations.NotNull;

public class JsonBackspaceHandler extends BackspaceHandlerDelegate {

	@Override
	public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {

	}

	@Override
	public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
		if (file.getLanguage() instanceof JsonLanguage && c == '"') {
			Caret caret = editor.getCaretModel().getCurrentCaret();
			IndentOptions indentOptions = CodeStyle.getIndentOptions(file);
			String tab = indentOptions.USE_TAB_CHARACTER ? "\t" : " ".repeat(indentOptions.INDENT_SIZE);
			int position = caret.getOffset() - tab.length();
			if (Util.regionMatches(editor.getDocument().getImmutableCharSequence(), position, tab, 0, tab.length())) {
				editor.getDocument().replaceString(position, position + tab.length(), "\"");
				return true;
			}
		}
		return false;
	}
}