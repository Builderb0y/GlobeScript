package builderb0y.globescript;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ConstantValue.*;
import builderb0y.globescript.datadriven.DataContext.StandardTypes;

@SuppressWarnings({ "unused", "DeprecatedIsStillUsed", "ImplicitNumericConversion" })
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

	public ExpressionReader(CharSequence input) {
		this(input, 0, input.length());
	}

	public void rollback(int cursor) {
		if (cursor > this.cursor) throw new IllegalArgumentException("Rolling forward");
		this.cursor = cursor;
		while (!this.comments.isEmpty() && this.comments.getLast().range.getStartOffset() >= cursor) this.comments.removeLast();
	}

	@Deprecated
	public int charAt(int index) {
		return index >= this.bufferStart && index < this.bufferEnd ? this.input.charAt(index) : -1;
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
			return new Token(this.input, this.cursor - 1, this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
			return new Token(this.input, this.cursor - 1, this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
			return new Token(this.input, this.cursor - expected.length(), this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
			return new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
	public boolean skipWhile(IntPredicate predicate) {
		int c = this.peek();
		if (c >= 0 && predicate.test(c)) {
			do {
				this.skip();
				c = this.peek();
			}
			while (c >= 0 && predicate.test(c));
			return true;
		}
		return false;
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
			return new Token(this.input, this.cursor - operator.length(), this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
			return new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(color);
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
			return new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(Colors.OPERATOR);
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
			return new Token(this.input, start, this.cursor, TokenInfo.UNKNOWN).withColor(color);
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
			return new Token(this.input, start, this.cursor, TokenInfo.UNKNOWN).withColor(Colors.NORMAL_IDENTIFIER);
		}
		else if (startChar == '`') {
			this.skip();
			this.skipWhile((int c) -> c != '`' && c != '\n' && c != '\r');
			this.has('`');
			return new Token(this.input, start, this.cursor, TokenInfo.UNKNOWN).withColor(Colors.ESCAPED_IDENTIFIER);
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

	public static final BigDecimal
		BYTE_MIN = BigDecimal.valueOf(Byte.MIN_VALUE),
		BYTE_MAX = BigDecimal.valueOf(Byte.MAX_VALUE),
		UBYTE_MIN = BigDecimal.ZERO,
		UBYTE_MAX = BigDecimal.valueOf(0xFFL),

		SHORT_MIN = BigDecimal.valueOf(Short.MIN_VALUE),
		SHORT_MAX = BigDecimal.valueOf(Short.MAX_VALUE),
		USHORT_MIN = BigDecimal.ZERO,
		USHORT_MAX = BigDecimal.valueOf(0xFFFFL),

		INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE),
		INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE),
		UINT_MIN = BigDecimal.ZERO,
		UINT_MAX = BigDecimal.valueOf(0xFFFF_FFFFL),

		LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE),
		LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE),
		ULONG_MIN = BigDecimal.ZERO,
		ULONG_MAX = new BigDecimal(new BigInteger("FFFFFFFFFFFFFFFF", 16));

	public @Nullable Token nextNumberAfterWhitespace(StandardTypes standardTypes) {
		this.skipWhitespace();
		return this.nextNumber(standardTypes);
	}

	@Deprecated
	public @Nullable Token nextNumber(StandardTypes standardTypes) {
		int start = this.cursor;
		if (isNumber(this.peek())) {
			List<PsiErrorDisplay> errors = new ArrayList<>(1);
			DecimalInfo firstPart = this.nextRadixDecimal(errors, true);
			int c = this.peek();
			if (c == 'p' || c == 'P') {
				this.skip();
				boolean negative = this.has('-');
				if (!negative) this.has('+');
				BigInteger precision = BigInteger.valueOf(Math.max(firstPart.radixPosition, 0));
				BigInteger toAddOrSubtract = this.nextRadixDecimal(errors, false).value;
				if (negative) precision = precision.add(toAddOrSubtract);
				else precision = precision.subtract(toAddOrSubtract);
				//log2(Double.MIN_VALUE) = -1074, so 1080 is a good max precision.
				if (precision.abs().compareTo(BigInteger.valueOf(1080)) <= 0) {
					firstPart.radixPosition = precision.intValue();
				}
				else {
					firstPart.radixPosition = 1080 * precision.signum();
				}
				c = this.peek();
			}
			boolean unsigned = c == 'u' || c == 'U';
			if (unsigned) {
				this.skip();
				c = this.peek();
			}
			boolean isNonInteger = firstPart.radixPosition > 0;
			BigDecimal finalValue = new BigDecimal(firstPart.value);
			if (isNonInteger) {
				finalValue = finalValue.divide(BigDecimal.valueOf(firstPart.radix).pow(firstPart.radixPosition), MathContext.DECIMAL128);
			}
			else if (firstPart.radixPosition < 0) {
				finalValue = finalValue.multiply(BigDecimal.valueOf(firstPart.radix).pow(-firstPart.radixPosition), MathContext.DECIMAL128);
			}
			ConstantValue constantValue = this.truncate(standardTypes, c, isNonInteger, unsigned, finalValue, errors);
			int errorStart = this.cursor;
			if (this.skipWhile(ExpressionReader::isLetterNumberDotOrUnderscore)) {
				errors.add(new PsiErrorDisplay(errorStart, this.cursor, "Extra trailing characters in number literal"));
			}
			Token token = new Token(this.input, start, this.cursor, new TokenInfo(constantValue));
			token.tooltips = errors;
			token.color = Colors.NUMBER;
			return token;
		}
		return null;
	}

	public ConstantValue truncate(
		StandardTypes standardTypes,
		int c,
		boolean isNonInteger,
		boolean unsigned,
		BigDecimal finalValue,
		List<PsiErrorDisplay> errors
	) {
		return switch (c) {
			case 'y', 'Y' -> {
				this.skip();
				if (isNonInteger) {
					errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Byte literals cannot have radix points."));
				}
				if (finalValue.compareTo(unsigned ? UBYTE_MIN : BYTE_MIN) < 0 || finalValue.compareTo(unsigned ? UBYTE_MAX : BYTE_MAX) > 0) {
					errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Literal value out of" + (unsigned ? " unsigned " : " ") + "byte range"));
				}
				yield new ByteConstantValue(standardTypes.byte_, finalValue.byteValue());
			}
			case 's', 'S' -> {
				this.skip();
				if (isNonInteger) {
					errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Short literals cannot have radix points."));
				}
				if (finalValue.compareTo(unsigned ? USHORT_MIN : SHORT_MIN) < 0 || finalValue.compareTo(unsigned ? USHORT_MAX : SHORT_MAX) > 0) {
					errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Literal value out of" + (unsigned ? " unsigned " : " ") + "short range"));
				}
				yield new ShortConstantValue(standardTypes.short_, finalValue.shortValue());
			}
			case 'i', 'I' -> {
				this.skip();
				if (isNonInteger) {
					yield new FloatConstantValue(standardTypes.float_, finalValue.floatValue());
				}
				else {
					if (finalValue.compareTo(unsigned ? UINT_MIN : INT_MIN) < 0 || finalValue.compareTo(unsigned ? UINT_MAX : INT_MAX) > 0) {
						errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Literal value out of" + (unsigned ? " unsigned " : " ") + "int range"));
					}
					yield new IntConstantValue(standardTypes.int_, finalValue.intValue());
				}
			}
			case 'l', 'L' -> {
				this.skip();
				if (isNonInteger) {
					yield new DoubleConstantValue(standardTypes.double_, finalValue.doubleValue());
				}
				else {
					if (finalValue.compareTo(unsigned ? ULONG_MIN : LONG_MIN) < 0 || finalValue.compareTo(unsigned ? ULONG_MAX : LONG_MAX) > 0) {
						errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Literal value out of" + (unsigned ? " unsigned " : " ") + "long range"));
					}
					yield new LongConstantValue(standardTypes.long_, finalValue.longValue());
				}
			}
			default -> {
				if (isNonInteger) {
					double doubleValue = finalValue.doubleValue();
					if (((double)(float)(doubleValue)) == doubleValue) {
						yield new FloatConstantValue(standardTypes.float_, (float)(doubleValue));
					}
					else {
						yield new DoubleConstantValue(standardTypes.double_, doubleValue);
					}
				}
				else {
					if (finalValue.compareTo(unsigned ? ULONG_MIN : LONG_MIN) < 0 || finalValue.compareTo(unsigned ? ULONG_MAX : LONG_MAX) > 0) {
						errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Literal value out of" + (unsigned ? " unsigned " : " ") + "long range"));
					}
					long longValue = finalValue.longValue();
					if (((long)(int)(longValue)) == longValue) {
						yield new IntConstantValue(standardTypes.int_, (int)(longValue));
					}
					else {
						yield new LongConstantValue(standardTypes.long_, longValue);
					}
				}
			}
		};
	}

	@Deprecated
	public DecimalInfo nextRadixDecimal(List<PsiErrorDisplay> errors, boolean allowNonIntegers) {
		int start = this.cursor;
		DecimalInfo firstPart = this.nextDecimal(10, errors, allowNonIntegers);
		int c = this.peek();
		if (c == 'x' || c == 'X') {
			this.skip();
			if (firstPart.radixPosition >= 0) {
				errors.add(new PsiErrorDisplay(start, this.cursor - 1, "Can't have a fractional radix"));
			}
			int radix;
			if (firstPart.value.compareTo(BigInteger.TWO) >= 0 && firstPart.value.compareTo(BigInteger.valueOf(16)) <= 0) {
				radix = firstPart.value.intValue();
			}
			else {
				errors.add(new PsiErrorDisplay(start, this.cursor - 1, "Invalid radix: " + firstPart.value + " (must be between 2 and 16, inclusive)" + (firstPart.value.signum() == 0 ? "; hex literals use the prefix '16x', not '0x'." : "")));
				radix = 10;
			}
			return this.nextDecimal(radix, errors, allowNonIntegers);
		}
		else {
			return firstPart;
		}
	}

	@Deprecated
	public DecimalInfo nextDecimal(int radix, List<PsiErrorDisplay> errors, boolean allowNonIntegers) {
		StringBuilder buffer = new StringBuilder();
		int dotPosition = Integer.MIN_VALUE;
		while (this.canRead()) {
			int c = this.peek();
			int digit = asciiDigit(c, radix);
			if (digit >= 0) {
				this.skip();
				buffer.append((char)(c));
				if (dotPosition != Integer.MIN_VALUE) dotPosition++;
			}
			else if (c == '_') {
				this.skip();
				continue;
			}
			else if (c == '.') {
				this.skip();
				if (!allowNonIntegers) errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Radix point not allowed here"));
				if (dotPosition >= 0) errors.add(new PsiErrorDisplay(this.cursor - 1, this.cursor, "Multiple radix points"));
				else dotPosition = 0;
			}
			else {
				break;
			}
		}
		if (dotPosition == 0) errors.add(new PsiErrorDisplay(this.cursor, this.cursor, "Missing fractional part of number"));
		return new DecimalInfo(new BigInteger(buffer.toString(), radix), radix, dotPosition & Integer.MAX_VALUE);
	}

	public static class DecimalInfo {

		public BigInteger value;
		public int radix;
		public int radixPosition;

		public DecimalInfo(BigInteger value, int radix, int radixPosition) {
			this.value = value;
			this.radix = radix;
			this.radixPosition = radixPosition;
		}

		@Override
		public @NotNull String toString() {
			return this.value.toString();
		}
	}

	public static int asciiDigit(int c, int radix) {
		int digit;
		if (c >= 'A') {
			if (c >= 'a') {
				digit = c + (10 - 'a');
			}
			else {
				digit = c + (10 - 'A');
			}
		}
		else {
			if (c >= '0') {
				digit = c - '0';
			}
			else {
				return -1;
			}
		}
		return digit < radix ? digit : -1;
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
										if (eol > start) this.comments.add(new Token(this.input, start, eol, TokenInfo.NON_VALUE).withColor(Colors.SCOPED_COMMENT));
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
						this.comments.add(new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(Colors.SCOPED_COMMENT));
					}
					else if (this.has(';')) {
						blockComment:
						while (true) {
							int eol = this.cursor;
							int read = this.read();
							switch (read) {
								case '\n', '\r' -> {
									if (this.splitNewLinesInMultiLineTokens()) {
										if (eol > start) this.comments.add(new Token(this.input, start, eol, TokenInfo.NON_VALUE).withColor(Colors.SCOPED_COMMENT));
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
						this.comments.add(new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(Colors.BLOCK_COMMENT));
					}
					else {
						this.skipWhile((int c) -> c != '\n' && c != '\r');
						this.comments.add(new Token(this.input, start, this.cursor, TokenInfo.NON_VALUE).withColor(Colors.LINE_COMMENT));
					}
				}
				default -> {
					break whitespace;
				}
			}
		}
	}

	//////////////// util

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