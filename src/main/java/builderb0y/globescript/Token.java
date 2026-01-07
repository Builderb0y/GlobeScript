package builderb0y.globescript;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Token {

	public static final Token[] EMPTY_ARRAY = {};

	public Object parent;
	public int parentIndex = -1;
	public TextRange range;
	public TextAttributesKey color;
	public @Nullable String tooltip;
	public Token[] children;

	public Token(@NotNull CharSequence text, int start, int end, @NotNull TextAttributesKey color) {
		this.range = new TextRange(start, end);
		this.color = color;
		this.parent = text;
		this.children = EMPTY_ARRAY;
	}

	public Token(@NotNull CharSequence text, @NotNull Token @NotNull ... children) {
		this.parent = text;
		this.range = new TextRange(
			children[0].range.getStartOffset(),
			children[children.length - 1].range.getEndOffset()
		);
		this.children = children;

		for (int index = 0, length = children.length; index < length; index++) {
			Token child = children[index];
			if (child.parent instanceof Token) {
				throw new IllegalArgumentException("Changing parent of " + child + " from " + child.parent + " to " + this);
			}
			else {
				child.parent = this;
				child.parentIndex = index;
			}
		}
	}

	public Token(CharSequence text, @NotNull List<@NotNull Token> children) {
		this(text, children.toArray(EMPTY_ARRAY));
	}

	public static @NotNull Token @NotNull [] filterNulls(@Nullable Token @NotNull ... tokens) {
		int count = 0;
		for (Token token : tokens) {
			if (token != null) count++;
		}
		if (count == tokens.length) return tokens;
		Token[] result = new Token[count];
		int index = 0;
		for (Token token : tokens) {
			if (token != null) result[index++] = token;
		}
		return result;
	}

	public CharSequence getEntireText() {
		Token token = this;
		while (true) {
			switch (token.parent) {
				case Token parent -> token = parent;
				case CharSequence text -> { return text; }
				default -> throw new IllegalStateException("Unexpected object in parent slot: " + token.parent);
			}
		}
	}

	public CharSequence getText() {
		CharSequence entireText = this.getEntireText();
		return entireText != null ? this.range.subSequence(entireText) : null;
	}

	public CharSequence getIdentifierText() {
		CharSequence text = this.getEntireText();
		if (text == null) return null;
		int start = this.range.getStartOffset();
		int end = this.range.getEndOffset();
		if (text.charAt(start) == '`') start++;
		if (text.charAt(end - 1) == '`') end--;
		return text.subSequence(start, end);
	}

	public Token getParent() {
		return this.parent instanceof Token parent ? parent : null;
	}

	public Token withColor(TextAttributesKey color) {
		this.color = color;
		return this;
	}

	public Token withTooltip(String tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	public boolean isLeaf() {
		return this.children.length == 0;
	}

	public Token firstLeaf() {
		Token token = this;
		while (!token.isLeaf()) token = token.children[0];
		return token;
	}

	public Stream<Token> streamLeaves() {
		return this.isLeaf() ? Stream.of(this) : Stream.of(this.children).flatMap(Token::streamLeaves);
	}

	public void forEach(Consumer<Token> action) {
		action.accept(this);
		for (Token child : this.children) {
			child.forEach(action);
		}
	}

	@Override
	public String toString() {
		return (this.color != null ? this.color.getExternalName() : "<no color>") + " " + this.getText() + " " + this.range;
	}
}