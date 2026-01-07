package builderb0y.globescript;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({ "unused", "DeprecatedIsStillUsed" })
public class ExpressionReader {

	public CharSequence input;
	public int bufferStart, bufferEnd;
	public int cursor;
	public List<Token> comments;

	public ExpressionReader(CharSequence input, int bufferStart, int bufferEnd) {
		this.input = input;
		this.bufferStart = bufferStart;
		this.bufferEnd = bufferEnd;
		this.cursor = bufferStart;
		this.comments = new ArrayList<>();
	}

	public void rollback(int cursor) {
		if (cursor > this.cursor) throw new IllegalArgumentException("Rolling forward");
		this.cursor = cursor;
		while (!this.comments.isEmpty() && this.comments.getLast().range.getStartOffset() >= cursor) this.comments.removeLast();
	}

	@Deprecated
	public int charAt(int index) {
		return index < this.bufferEnd ? this.input.charAt(index) : -1;
	}

	@Deprecated
	public boolean canRead() {
		return this.cursor < this.bufferEnd;
	}

	public boolean canReadAfterWhitespace() {
		this.skipWhitespace();
		return this.canRead();
	}

	@Deprecated
	public int read() {
		int read = this.peek();
		if (read >= 0) this.skip();
		return read;
	}

	public int readAfterWhitespace() {
		this.skipWhitespace();
		return this.read();
	}

	@Deprecated
	public int peek() {
		return this.charAt(this.cursor);
	}

	public int peekAfterWhitespace() {
		this.skipWhitespace();
		return this.peek();
	}

	@Deprecated
	public boolean skip() {
		if (this.canRead()) {
			this.cursor++;
			return true;
		}
		return false;
	}

	@Deprecated
	public void skip(int count) {
		this.cursor = Math.clamp(this.cursor + count, this.bufferStart, this.bufferEnd);
	}

	//////////////// has(char)

	@Deprecated
	public boolean has(char expected) {
		if (this.peek() == expected) {
			this.skip();
			return true;
		}
		return false;
	}

	@Deprecated
	public @Nullable Token has(char expected, TextAttributesKey color) {
		if (this.has(expected)) {
			return new Token(this.input, this.cursor - 1, this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean has(char expected, TextAttributesKey color, List<Token> tokens) {
		Token token = this.has(expected, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(char expected) {
		this.skipWhitespace();
		return this.has(expected);
	}

	public @Nullable Token hasAfterWhitespace(char expected, TextAttributesKey color) {
		this.skipWhitespace();
		return this.has(expected, color);
	}

	public boolean hasAfterWhitespace(char expected, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.has(expected, color, tokens);
	}

	//////////////// has(IntPredicate)

	@Deprecated
	public boolean has(IntPredicate predicate) {
		int c = this.peek();
		if (c >= 0 && predicate.test(c)) {
			this.skip();
			return true;
		}
		return false;
	}

	@Deprecated
	public @Nullable Token has(IntPredicate predicate, TextAttributesKey color) {
		if (this.has(predicate)) {
			return new Token(this.input, this.cursor - 1, this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean has(IntPredicate predicate, TextAttributesKey color, List<Token> tokens) {
		Token token = this.has(predicate, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(IntPredicate predicate) {
		this.skipWhitespace();
		return this.has(predicate);
	}

	public @Nullable Token hasAfterWhitespace(IntPredicate predicate, TextAttributesKey color) {
		this.skipWhitespace();
		return this.has(predicate, color);
	}

	public boolean hasAfterWhitespace(IntPredicate predicate, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.has(predicate, color, tokens);
	}

	//////////////// has(CharSequence)

	@Deprecated
	public boolean has(CharSequence expected) {
		int revert = this.cursor;
		for (int index = 0, length = expected.length(); index < length; index++) {
			if (this.read() != expected.charAt(index)) {
				this.cursor = revert;
				return false;
			}
		}
		return true;
	}

	@Deprecated
	public @Nullable Token has(CharSequence expected, TextAttributesKey color) {
		if (this.has(expected)) {
			return new Token(this.input, this.cursor - expected.length(), this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean has(CharSequence expected, TextAttributesKey color, List<Token> tokens) {
		Token token = this.has(expected, color);
		if (this.has(expected)) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(CharSequence expected) {
		this.skipWhitespace();
		return this.has(expected);
	}

	public @Nullable Token hasAfterWhitespace(CharSequence expected, TextAttributesKey color) {
		this.skipWhitespace();
		return this.has(expected, color);
	}

	public boolean hasAfterWhitespace(CharSequence expected, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.has(expected, color, tokens);
	}

	//////////////// hasMulti(IntPredicate)

	@Deprecated
	public boolean hasMulti(IntPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.cursor != start;
	}

	@Deprecated
	public @Nullable Token hasMulti(IntPredicate predicate, TextAttributesKey color) {
		int start = this.cursor;
		if (this.hasMulti(predicate)) {
			return new Token(this.input, start, this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean hasMulti(IntPredicate predicate, TextAttributesKey color, List<Token> tokens) {
		Token token = this.hasMulti(predicate, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasMultiAfterWhitespace(IntPredicate predicate) {
		this.skipWhitespace();
		return this.hasMulti(predicate);
	}

	public @Nullable Token hasMultiAfterWhitespace(IntPredicate predicate, TextAttributesKey color) {
		this.skipWhitespace();
		return this.hasMulti(predicate, color);
	}

	public boolean hasMultiAfterWhitespace(IntPredicate predicate, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.hasMulti(predicate, color, tokens);
	}

	//////////////// nextMulti(IntPredicate)

	@Deprecated
	public void skipWhile(IntPredicate predicate) {
		for (int c; (c = this.peek()) >= 0 && predicate.test(c); this.skip());
	}

	@Deprecated
	public @Nullable CharSequence peekMulti(IntPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		int end = this.cursor;
		if (end != start) {
			this.cursor = start;
			return this.input.subSequence(start, end);
		}
		return null;
	}

	public @Nullable CharSequence peekMultiAfterWhitespace(IntPredicate predicate) {
		this.skipWhitespace();
		return this.peekMulti(predicate);
	}

	@Deprecated
	public @Nullable CharSequence readMulti(IntPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.cursor == start ? null : this.input.subSequence(start, this.cursor);
	}

	public @Nullable CharSequence readMultiAfterWhitespace(IntPredicate predicate) {
		this.skipWhitespace();
		return this.readMulti(predicate);
	}

	//////////////// operators

	@Deprecated
	public @Nullable CharSequence peekOperator() {
		return this.peekMultiAfterWhitespace(ExpressionReader::isOperatorSymbol);
	}

	public @Nullable CharSequence peekOperatorAfterWhitespace() {
		this.skipWhitespace();
		return this.peekOperator();
	}

	@Deprecated
	public @Nullable CharSequence readOperator() {
		return this.readMulti(ExpressionReader::isOperatorSymbol);
	}

	public @Nullable CharSequence readOperatorAfterWhitespace() {
		this.skipWhitespace();
		return this.readOperator();
	}

	@Deprecated
	public boolean hasOperator(CharSequence operator) {
		int revert = this.cursor;
		if (this.has(operator) && !isOperatorSymbol(this.peek())) {
			return true;
		}
		else {
			this.cursor = revert;
			return false;
		}
	}

	@Deprecated
	public @Nullable Token hasOperator(CharSequence operator, TextAttributesKey color) {
		if (this.hasOperator(operator)) {
			return new Token(this.input, this.cursor - operator.length(), this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean hasOperator(CharSequence operator, TextAttributesKey color, List<Token> tokens) {
		Token token = this.hasOperator(operator, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasOperatorAfterWhitespace(CharSequence operator) {
		this.skipWhitespace();
		return this.hasOperator(operator);
	}

	public @Nullable Token hasOperatorAfterWhitespace(CharSequence operator, TextAttributesKey color) {
		this.skipWhitespace();
		return this.hasOperator(operator, color);
	}

	public boolean hasOperatorAfterWhitespace(CharSequence operator, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.hasOperator(operator, color, tokens);
	}

	@Deprecated
	public boolean hasOperator(Set<CharSequence> operators) {
		int revert = this.cursor;
		CharSequence sequence = this.readOperator();
		if (operators.contains(sequence)) {
			return true;
		}
		else {
			this.cursor = revert;
			return false;
		}
	}

	@Deprecated
	public @Nullable Token hasOperator(Set<CharSequence> operators, TextAttributesKey color) {
		int start = this.cursor;
		if (this.hasOperator(operators)) {
			return new Token(this.input, start, this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean hasOperator(Set<CharSequence> operators, TextAttributesKey color, List<Token> tokens) {
		Token token = this.hasOperator(operators, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasOperatorAfterWhitespace(Set<CharSequence> operators) {
		this.skipWhitespace();
		return this.hasOperator(operators);
	}

	public @Nullable Token hasOperatorAfterWhitespace(Set<CharSequence> operators, TextAttributesKey color) {
		this.skipWhitespace();
		return this.hasOperator(operators, color);
	}

	public boolean hasOperatorAfterWhitespace(Set<CharSequence> operators, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.hasOperator(operators, color, tokens);
	}

	@Deprecated
	public @Nullable Token nextOperator() {
		if (isOperatorSymbol(this.peek())) {
			int start = this.cursor;
			this.skip();
			this.skipWhile(ExpressionReader::isOperatorSymbol);
			return new Token(this.input, start, this.cursor, Colors.OPERATOR);
		}
		return null;
	}

	public @Nullable Token nextOperatorAfterWhitespace() {
		this.skipWhitespace();
		return this.nextOperator();
	}

	//////////////// identifiers

	@Deprecated
	public @Nullable CharSequence peekIdentifier() {
		int start = this.cursor;
		int startChar = this.peek();
		if (isLetterOrUnderscore(startChar)) {
			this.skip();
			this.hasMulti(ExpressionReader::isLetterNumberOrUnderscore);
			int end = this.cursor;
			this.cursor = start;
			return this.input.subSequence(start, end);
		}
		else if (startChar == '`') {
			this.skip();
			this.hasMulti((int c) -> c != '`' && c != '\n' && c != '\r');
			int end = this.cursor;
			this.cursor = start;
			return this.input.subSequence(start + 1, end);
		}
		else {
			return null;
		}
	}

	public @Nullable CharSequence peekIdentifierAfterWhitespace() {
		this.skipWhitespace();
		return this.peekIdentifier();
	}

	@Deprecated
	public @Nullable CharSequence readIdentifier() {
		int start = this.cursor;
		int startChar = this.peek();
		if (isLetterOrUnderscore(startChar)) {
			this.skip();
			this.hasMulti(ExpressionReader::isLetterNumberOrUnderscore);
			int end = this.cursor;
			return this.input.subSequence(start, end);
		}
		else if (startChar == '`') {
			this.skip();
			this.hasMulti((int c) -> c != '`' && c != '\n' && c != '\r');
			int end = this.cursor;
			this.has('`');
			return this.input.subSequence(start + 1, end);
		}
		else {
			return null;
		}
	}

	public @Nullable CharSequence readIdentifierAfterWhitespace() {
		this.skipWhitespace();
		return this.readIdentifier();
	}

	@Deprecated
	public boolean hasIdentifier(CharSequence identifier) {
		int revert = this.cursor;
		int startChar = this.peek();
		if (isLetterOrUnderscore(startChar)) {
			if (this.has(identifier)) {
				if (isLetterNumberOrUnderscore(this.peek())) {
					this.cursor = revert;
					return false;
				}
				return true;
			}
		}
		else if (startChar == '`') {
			this.skip();
			if (this.has(identifier)) {
				if (this.read() != '`') {
					this.cursor = revert;
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@Deprecated
	public @Nullable Token hasIdentifier(CharSequence identifier, TextAttributesKey color) {
		int start = this.cursor;
		if (this.hasIdentifier(identifier)) {
			return new Token(this.input, start, this.cursor, color);
		}
		return null;
	}

	@Deprecated
	public boolean hasIdentifier(CharSequence identifier, TextAttributesKey color, List<Token> tokens) {
		Token token = this.hasIdentifier(identifier, color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public boolean hasIdentifierAfterWhitespace(CharSequence identifier) {
		this.skipWhitespace();
		return this.hasIdentifier(identifier);
	}

	public @Nullable Token hasIdentifierAfterWhitespace(CharSequence identifier, TextAttributesKey color) {
		this.skipWhitespace();
		return this.hasIdentifier(identifier, color);
	}

	public boolean hasIdentifierAfterWhitespace(CharSequence identifier, TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.hasIdentifier(identifier, color, tokens);
	}

	@Deprecated
	public @Nullable Token nextIdentifier() {
		int start = this.cursor;
		int startChar = this.peek();
		if (isLetterOrUnderscore(startChar)) {
			this.skip();
			this.skipWhile(ExpressionReader::isLetterNumberOrUnderscore);
			return new Token(this.input, start, this.cursor, Colors.NORMAL_IDENTIFIER);
		}
		else if (startChar == '`') {
			this.skip();
			this.skipWhile((int c) -> c != '`' && c != '\n' && c != '\r');
			this.has('`');
			return new Token(this.input, start, this.cursor, Colors.ESCAPED_IDENTIFIER);
		}
		return null;
	}

	@Deprecated
	public @Nullable Token nextIdentifier(TextAttributesKey color) {
		Token token = this.nextIdentifier();
		if (token != null) token.color = color;
		return token;
	}

	@Deprecated
	public boolean nextIdentifier(TextAttributesKey color, List<Token> tokens) {
		Token token = this.nextIdentifier(color);
		if (token != null) {
			tokens.add(token);
			return true;
		}
		return false;
	}

	public @Nullable Token nextIdentifierAfterWhitespace() {
		this.skipWhitespace();
		return this.nextIdentifier();
	}

	public @Nullable Token nextIdentifierAfterWhitespace(TextAttributesKey color) {
		this.skipWhitespace();
		return this.nextIdentifier(color);
	}

	public boolean nextIdentifierAfterWhitespace(TextAttributesKey color, List<Token> tokens) {
		this.skipWhitespace();
		return this.nextIdentifier(color, tokens);
	}

	//////////////// numbers

	@Deprecated
	public @Nullable Token nextNumber() {
		int start = this.cursor;
		int startChar = this.peek();
		if (isNumber(startChar)) {
			this.skipWhile(ExpressionReader::isLetterNumberDotOrUnderscore);
			return new Token(this.input, start, this.cursor, Colors.NUMBER);
		}
		return null;
	}

	//////////////// whitespace

	public boolean splitNewLinesInMultiLineTokens() {
		return false;
	}

	public void skipWhitespace() {
		whitespace:
		while (true) {
			switch (this.peek()) {
				case ' ', '\t', '\n', '\r' -> {
					this.skip();
				}
				case ';' -> {
					int start = this.cursor;
					this.skip();
					if (this.has('(')) {
						int depth = 0;
						scopedComment:
						while (true) {
							int eol = this.cursor;
							int read = this.read();
							switch (read) {
								case '\n', '\r' -> {
									if (this.splitNewLinesInMultiLineTokens()) {
										if (eol > start) this.comments.add(new Token(this.input, start, eol, Colors.SCOPED_COMMENT));
										start = this.cursor;
									}
								}
								case '(', '[', '{' -> {
									depth++;
								}
								case ')', ']', '}' -> {
									if (--depth < 0) {
										break scopedComment;
									}
								}
								case -1 -> {
									break scopedComment;
								}
							}
						}
						this.comments.add(new Token(this.input, start, this.cursor, Colors.SCOPED_COMMENT));
					}
					else if (this.has(';')) {
						blockComment:
						while (true) {
							int eol = this.cursor;
							int read = this.read();
							switch (read) {
								case '\n', '\r' -> {
									if (this.splitNewLinesInMultiLineTokens()) {
										if (eol > start) this.comments.add(new Token(this.input, start, eol, Colors.SCOPED_COMMENT));
										start = this.cursor;
									}
								}
								case ';' -> {
									if (this.has(';')) {
										break blockComment;
									}
								}
								case -1 -> {
									break blockComment;
								}
							}
						}
						this.comments.add(new Token(this.input, start, this.cursor, Colors.BLOCK_COMMENT));
					}
					else {
						this.skipWhile((int c) -> c != '\n' && c != '\r');
						this.comments.add(new Token(this.input, start, this.cursor, Colors.LINE_COMMENT));
					}
				}
				default -> {
					break whitespace;
				}
			}
		}
	}

	//////////////// util

	/*
	@Deprecated
	public static boolean regionMatches(CharSequence a, int aOffset, CharSequence b, int bOffset, int length) {
		if (aOffset < 0 || bOffset < 0 || aOffset + length > a.length() || bOffset + length > b.length()) return false;
		for (int offset = 0; offset < length; offset++) {
			if (a.charAt(aOffset + offset) != b.charAt(bOffset + offset)) return false;
		}
		return true;
	}
	*/

	public static boolean isOperatorSymbol(int c) {
		return switch (c) {
			case '!', '#', '$', '%', '&', '*', '+', ',', '-', '.', '/', ':', '<', '=', '>', '?', '@', '\\', '^', '|', '~' -> true;
			default -> false;
		};
	}

	public static boolean isLetterOrUnderscore(int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
	}

	public static boolean isNumber(int c) {
		return (c >= '0' && c <= '9');
	}

	public static boolean isLetterNumberOrUnderscore(int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_');
	}

	public static boolean isLetterNumberDotOrUnderscore(int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '.') || (c == '_');
	}
}