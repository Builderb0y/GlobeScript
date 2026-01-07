package builderb0y.globescript;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class GlobeScriptParserDefinition implements ParserDefinition {

	@Override
	public @NotNull Lexer createLexer(Project project) {
		return new WorstPossibleLexer();
	}

	@Override
	public @NotNull PsiParser createParser(Project project) {
		return new Instances.GlobeScriptPsiParser();
	}

	@Override
	public @NotNull IFileElementType getFileNodeType() {
		return Instances.FILE_ELEMENT_TYPE;
	}

	@Override
	public @NotNull TokenSet getCommentTokens() {
		return TokenSet.EMPTY;
	}

	@Override
	public @NotNull TokenSet getWhitespaceTokens() {
		return TokenSet.EMPTY;
	}

	@Override
	public @NotNull TokenSet getStringLiteralElements() {
		return TokenSet.EMPTY;
	}

	@Override
	public @NotNull PsiElement createElement(ASTNode node) {
		return new ASTWrapperPsiElement(node);
	}

	@Override
	public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
		return new Instances.GlobeScriptFile(viewProvider);
	}
}