package builderb0y.globescript;

import javax.swing.*;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.NlsContexts.Label;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Instances {

	public static final Language LANGUAGE = new GlobeScriptLanguage();
	public static final IElementType ELEMENT_TYPE = new IElementType("Instances.ELEMENT_TYPE", LANGUAGE);
	public static final LanguageFileType FILE_TYPE = GlobeScriptFileType.INSTANCE;
	public static final IFileElementType FILE_ELEMENT_TYPE = new IFileElementType(LANGUAGE);
	public static final Icon ICON = IconLoader.getIcon("/icons/globe.png", Instances.class);

	public static class GlobeScriptLanguage extends Language {

		public GlobeScriptLanguage() {
			super("GlobeScript");
		}
	}

	public static class GlobeScriptFileType extends LanguageFileType {

		public static final GlobeScriptFileType INSTANCE = new GlobeScriptFileType();

		public GlobeScriptFileType() {
			super(Instances.LANGUAGE);
		}

		@Override
		public @NonNls @NotNull String getName() {
			return "GlobeScript";
		}

		@Override
		public @Label @NotNull String getDescription() {
			return "Script files for Big Globe";
		}

		@Override
		public @NlsSafe @NotNull String getDefaultExtension() {
			return "gs";
		}

		@Override
		public @Nullable Icon getIcon() {
			return ICON;
		}
	}

	public static class GlobeScriptFile extends PsiFileBase {

		public GlobeScriptFile(@NotNull FileViewProvider viewProvider) {
			super(viewProvider, LANGUAGE);
		}

		@Override
		public @NotNull FileType getFileType() {
			return GlobeScriptFileType.INSTANCE;
		}
	}

	public static class DummySyntaxHighlighter implements SyntaxHighlighter {

		@Override
		public @NotNull Lexer getHighlightingLexer() {
			return new WorstPossibleLexer();
		}

		@Override
		public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
			return TextAttributesKey.EMPTY_ARRAY;
		}
	}

	public static class GlobeScriptPsiParser implements PsiParser {

		@Override
		public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
			Marker marker = builder.mark();
			while (!builder.eof()) {
				builder.advanceLexer();
			}
			marker.done(ELEMENT_TYPE);
			return builder.getTreeBuilt();
		}
	}
}