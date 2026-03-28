package builderb0y.globescript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Token implements OneOrMoreTokens {

	public static final Token[] EMPTY_ARRAY = {};

	public Object parent;
	public int parentIndex = -1;
	public TextRange range;
	public TextAttributesKey color;
	public List<PsiErrorDisplay> tooltips = new ArrayList<>(2);
	public TokenInfo info;
	public Token[] children;

	public Token(@NotNull CharSequence text, int start, int end, TokenInfo info) {
		this.parent = text;
		this.range = new TextRange(start, end);
		this.info = info;
		this.children = EMPTY_ARRAY;
	}

	public Token(@NotNull CharSequence text, TokenInfo info, @Nullable Token @NotNull ... children) {
		this.parent = text;
		this.info = info;

		int length = children.length, count = 0;
		for (int index = 0; index < length; index++) {
			Token child = children[index];
			if (child != null) {
				count++;
				if (child.parent instanceof Token) {
					throw new IllegalArgumentException("Changing parent of " + child + " from " + child.parent + " to " + this);
				}
				else {
					child.parent = this;
					child.parentIndex = index; //optimistic.
				}
			}
		}

		if (count < length) {
			Token[] newChildren = new Token[count];
			int writeIndex = 0;
			for (Token child : children) {
				if (child != null) {
					(newChildren[writeIndex] = child).parentIndex = writeIndex++;
				}
			}
			children = newChildren;
		}

		this.range = new TextRange(
			children[0].range.getStartOffset(),
			children[children.length - 1].range.getEndOffset()
		);
		this.children = children;
	}

	public Token(CharSequence text, TokenInfo info, @NotNull List<@Nullable Token> children) {
		this(text, info, children.toArray(EMPTY_ARRAY));
	}

	public Token[] disownChildren() {
		CharSequence text = this.getEntireText();
		Token[] children = this.children;
		this.children = EMPTY_ARRAY;
		for (Token child : children) {
			child.parent = text;
		}
		return children;
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

	public Token withInfo(TokenInfo info) {
		this.info = info;
		return this;
	}

	public Token initIdentifier(TextAttributesKey color, TokenInfo info) {
		return this.withColor(color).withInfo(info);
	}

	public Token withTooltip(String tooltip) {
		return this.withError(new PsiErrorDisplay(this.range.getStartOffset(), this.range.getEndOffset(), tooltip));
	}

	public Token withError(PsiErrorDisplay error) {
		this.tooltips.add(error);
		return this;
	}

	public Token error(String tooltip) {
		return this.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip(tooltip);
	}

	public boolean isLeaf() {
		return this.children.length == 0;
	}

	public Token firstLeaf() {
		Token token = this;
		while (!token.isLeaf()) token = token.children[0];
		return token;
	}

	public static List<Token> typesOnly(Token... tokens) {
		List<Token> list = new ArrayList<>(tokens.length);
		for (Token child : tokens) {
			if (child.info.type() != null) list.add(child);
		}
		return list;
	}

	public static List<Token> typesOnly(List<Token> tokens) {
		List<Token> list = new ArrayList<>(tokens.size());
		for (Token child : tokens) {
			if (child.info.type() != null) list.add(child);
		}
		return list;
	}

	public List<Token> getChildrenWithTypes() {
		return typesOnly(this.children);
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
		return (this.color != null ? this.color.getExternalName() : "<no color>") + " " + this.info.type() + " " + this.getText() + " " + this.range;
	}

	@Override
	public void addTo(List<Token> list) {
		list.add(this);
	}

	@Override
	public Token group() {
		return this;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder extends ArrayList<Token> {

		public Token build(CharSequence entireText, TokenInfo info) {
			return new Token(entireText, info, this);
		}

		public Builder with(OneOrMoreTokens tokens) {
			if (tokens != null) this.add(tokens.group());
			return this;
		}

		public Builder withAll(OneOrMoreTokens tokens) {
			if (tokens != null) tokens.addTo(this);
			return this;
		}
	}
}