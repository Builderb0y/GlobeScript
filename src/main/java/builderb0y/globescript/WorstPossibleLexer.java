package builderb0y.globescript;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorstPossibleLexer extends LexerBase {

	public CharSequence buffer;
	public int bufferStart, bufferEnd;
	public boolean done;

	@Override
	public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.bufferStart = startOffset;
		this.bufferEnd = endOffset;
	}

	@Override
	public int getState() {
		return 0;
	}

	@Override
	public @Nullable IElementType getTokenType() {
		return this.done ? null : Instances.ELEMENT_TYPE;
	}

	@Override
	public int getTokenStart() {
		return this.done ? this.bufferEnd : this.bufferStart;
	}

	@Override
	public int getTokenEnd() {
		return this.bufferEnd;
	}

	@Override
	public void advance() {
		this.done = true;
	}

	@Override
	public @NotNull CharSequence getBufferSequence() {
		return this.buffer;
	}

	@Override
	public int getBufferEnd() {
		return this.bufferEnd;
	}
}