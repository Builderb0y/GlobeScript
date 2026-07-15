package builderb0y.globescript;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.PasteProvider;
import com.intellij.json.JsonLanguage;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actions.PasteAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.util.Producer;
import org.jetbrains.annotations.NotNull;

import builderb0y.globescript.util.Util;

public class JsonPasteHandler implements PasteProvider {

	@Override
	public void performPaste(@NotNull DataContext dataContext) {
		PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
		String pasted;
		try {
			pasted = (String)(
				dataContext
				.getData(PasteAction.TRANSFERABLE_PROVIDER)
				.get()
				.getTransferData(DataFlavor.stringFlavor)
			);
		}
		catch (Exception exception) {
			exception.printStackTrace();
			return;
		}
		IndentOptions indentOptions = CodeStyle.getIndentOptions(file);
		String tab = indentOptions.USE_TAB_CHARACTER ? "\t" : " ".repeat(indentOptions.INDENT_SIZE);
		Caret caret = dataContext.getData(CommonDataKeys.CARET);
		Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
		ApplicationManager.getApplication().runWriteAction(() -> {
			int pos;
			if (caret.hasSelection()) {
				file.getFileDocument().deleteString(pos = caret.getSelectionStart(), caret.getSelectionEnd());
				caret.moveToOffset(pos);
				caret.removeSelection();
			}
			else {
				pos = caret.getOffset();
			}
			if (file.getLanguage() == JsonLanguage.INSTANCE) {
				if (file.findElementAt(pos).getParent() instanceof JsonStringLiteral string) {
					if (pasted.indexOf('\n') >= 0 || pasted.indexOf('\r') >= 0) {
						if (string.getParent() instanceof JsonProperty) {
							JsonEnterHandler.ensureArray(file.getFileDocument(), caret, string, tab);
						}
						insertMultiLine(pasted, file, caret, tab, true);
					}
					else {
						CharSequence trimmed = toJson(pasted, looksLikeJson(pasted, true, true));
						file.getFileDocument().insertString(pos, trimmed);
						caret.moveToOffset(pos + trimmed.length());
					}
				}
				else {
					file.getFileDocument().insertString(pos, pasted);
					caret.moveToOffset(pos + pasted.length());
				}
			}
			else if (file.getLanguage() == Instances.LANGUAGE) {
				if (pasted.indexOf('\n') >= 0 || pasted.indexOf('\r') >= 0) {
					insertMultiLine(pasted, file, caret, tab, false);
				}
				else {
					CharSequence toInsert = (
						looksLikeJson(pasted, true, true)
						? toPlainText(pasted)
						: pasted
					);
					file.getFileDocument().insertString(pos, toInsert);
					caret.moveToOffset(pos + toInsert.length());
				}
			}
			if (editor != null) EditorModificationUtil.scrollToCaret(editor);
		});
	}

	public static void insertMultiLine(String pasted, PsiFile file, Caret caret, String tab, boolean isJson) {
		int pos = caret.getOffset();
		Document document = file.getFileDocument();
		String[] lines = pasted.lines().toArray(String[]::new);
		boolean looksLikeJson = allLooksLikeJson(lines);
		CharSequence firstLine = toJson(lines[0], looksLikeJson);
		document.insertString(pos, firstLine);
		caret.moveToOffset(pos + firstLine.length());
		for (int index = 1; index < lines.length; index++) {
			CharSequence line = toJson(lines[index], looksLikeJson);
			char firstChar = line.isEmpty() ? 0 : line.charAt(0);
			JsonEnterHandler.insertNewLine(null, document, caret, tab, isJson, firstChar == ')' || firstChar == ']' || firstChar == '}' ? -1 : 0);
			pos = caret.getOffset();
			document.insertString(pos, line);
			caret.moveToOffset(pos + line.length());
		}
	}

	public static boolean allLooksLikeJson(CharSequence[] lines) {
		for (int index = 0, length = lines.length; index < length; index++) {
			if (!looksLikeJson(lines[index], index != 0, index != length - 1)) return false;
		}
		return true;
	}

	public static boolean looksLikeJson(CharSequence line, boolean notFirst, boolean notLast) {
		int length = line.length();
		if (notFirst) {
			for (int index = 0; index < length; index++) {
				char c = line.charAt(index);
				if (!Character.isWhitespace(c)) {
					if (c == '"') break;
					else return false;
				}
			}
		}
		if (notLast) {
			boolean seenComma = false;
			for (int index = length; --index >= 0; ) {
				char c = line.charAt(index);
				if (!Character.isWhitespace(c)) {
					if (c == ',') {
						if (seenComma) return false;
						else seenComma = true;
					}
					else if (c == '"') {
						break;
					}
					else {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static CharSequence toJson(CharSequence line, boolean fromJson) {
		StringBuilder builder = new StringBuilder(line.length());
		boolean seenStart = false;
		int end = 0;
		for (int index = 0, length = line.length(); index < length; index++) {
			char c = line.charAt(index);
			if (!seenStart) {
				if (!Character.isWhitespace(c)) {
					if (!(fromJson && c == '"')) {
						builder.append(c);
					}
					seenStart = true;
				}
			}
			else {
				if (c < ' ' || c > '~' || c == '"' || c == '\\') {
					switch (c) {
						case '"'  -> builder.append("\\\"");
						case '\b' -> builder.append("\\\b");
						case '\f' -> builder.append("\\\f");
						case '\t' -> builder.append("\\\t");
						case '\n', '\r' -> throw new IllegalArgumentException("Newline found in single-line...");
						case '\\' -> {
							if (fromJson) {
								builder.append(c).append(line.charAt(++index));
							}
							else {
								builder.append("\\\\");
							}
						}
						default -> {
							builder
							.append('\\')
							.append('u')
							.append(Util.hexI2C(c >>> 12))
							.append(Util.hexI2C(c >>>  8))
							.append(Util.hexI2C(c >>>  4))
							.append(Util.hexI2C(c       ))
							;
						}
					}
				}
				else {
					builder.append(c);
				}
			}
			if (!fromJson || (c != '"' && c != ',' && !Character.isWhitespace(c))) {
				end = builder.length();
			}
		}
		builder.setLength(end);
		return builder;
	}

	public static CharSequence toPlainText(CharSequence line) {
		StringBuilder builder = new StringBuilder(line.length());
		boolean seenStart = false;
		int end = 0;
		for (int index = 0, length = line.length(); index < length; index++) {
			char c = line.charAt(index);
			if (!seenStart && !Character.isWhitespace(c)) {
				seenStart = true;
				if (c == '"') continue;
			}
			if (c == '\\') {
				c = line.charAt(++index);
				switch (c) {
					case '"'  -> builder.append('"');
					case 'b'  -> builder.append('\b');
					case 'f'  -> builder.append('\f');
					case 't'  -> builder.append('\t');
					case 'n'  -> builder.append('\n');
					case 'r'  -> builder.append('\r');
					case '\\' -> builder.append('\\');
					case 'u'  -> {
						done: {
							int index2 = index;
							invalid: {
								int part0 = Util.hexC2I(line.charAt(++index2)); if (part0 < 0) break invalid;
								int part1 = Util.hexC2I(line.charAt(++index2)); if (part1 < 0) break invalid;
								int part2 = Util.hexC2I(line.charAt(++index2)); if (part2 < 0) break invalid;
								int part3 = Util.hexC2I(line.charAt(++index2)); if (part3 < 0) break invalid;
								builder.append((char)((part0 << 12) | (part1 <<  8) | (part2 <<  4) | (part3)));
								index = index2;
								break done;
							}
							builder.append("\\u");
						}
					}
				}
			}
			else {
				builder.append(c);
			}
			if (c != '"' && c != ',' && !Character.isWhitespace(c)) {
				end = builder.length();
			}
		}
		builder.setLength(end);
		return builder;
	}

	@Override
	public boolean isPastePossible(@NotNull DataContext dataContext) {
		PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
		if (file == null) return false;
		Producer<Transferable> transferableProducer = dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER);
		if (transferableProducer == null || transferableProducer.get() == null || !transferableProducer.get().isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
		return file.getLanguage() == JsonLanguage.INSTANCE || file.getLanguage() == Instances.LANGUAGE;
	}

	@Override
	public boolean isPasteEnabled(@NotNull DataContext dataContext) {
		PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
		if (file == null) return false;
		Producer<Transferable> transferableProducer = dataContext.getData(PasteAction.TRANSFERABLE_PROVIDER);
		if (transferableProducer == null || transferableProducer.get() == null || !transferableProducer.get().isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
		if (file.getLanguage() == JsonLanguage.INSTANCE) {
			Caret caret = dataContext.getData(CommonDataKeys.CARET);
			if (caret == null) return false;
			PsiElement element = file.findElementAt(caret.getOffset());
			if (element == null) return false;
			element = element.getParent();
			if (element == null) return false;
			return element instanceof JsonStringLiteral string && !string.isPropertyName();
		}
		else {
			return file.getLanguage() == Instances.LANGUAGE;
		}
	}
}