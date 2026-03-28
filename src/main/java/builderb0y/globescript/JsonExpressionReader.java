package builderb0y.globescript;

import builderb0y.globescript.util.Util;

public class JsonExpressionReader extends ExpressionReader {

	public JsonExpressionReader(CharSequence input, int bufferStart, int bufferEnd) {
		super(input, bufferStart, bufferEnd);
	}

	@Override
	@Deprecated
	public int charAt(int index) {
		int c = super.charAt(index);
		if (c == '\\') {
			c = super.charAt(index + 1);
			return switch (c) {
				case '"'  -> '"';
				case '\\' -> '\\';
				case '/'  -> '/';
				case 'b'  -> '\b';
				case 'f'  -> '\f';
				case 'n'  -> '\n';
				case 'r'  -> '\r';
				case 't'  -> '\t';
				case 'u'  -> {
					int c4 = Util.hexC2I(super.charAt(index + 2)); if (c4 < 0) yield '\\';
					int c3 = Util.hexC2I(super.charAt(index + 3)); if (c3 < 0) yield '\\';
					int c2 = Util.hexC2I(super.charAt(index + 4)); if (c2 < 0) yield '\\';
					int c1 = Util.hexC2I(super.charAt(index + 5)); if (c1 < 0) yield '\\';
					yield (c4 << 12) | (c3 << 8) | (c2 << 4) | c1;
				}
				default -> '\\';
			};
		}
		else if (c == '"') {
			return '\n';
		}
		else {
			return c;
		}
	}

	@Override
	@Deprecated
	public boolean skip() {
		int c = super.charAt(this.cursor);
		if (c < 0) return false;
		if (c == '\\') {
			c = super.charAt(this.cursor + 1);
			this.cursor += switch (c) {
				case '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> 2;
				case 'u' -> {
					if (
						Util.hexC2I(super.charAt(this.cursor + 2)) >= 0 &&
						Util.hexC2I(super.charAt(this.cursor + 3)) >= 0 &&
						Util.hexC2I(super.charAt(this.cursor + 4)) >= 0 &&
						Util.hexC2I(super.charAt(this.cursor + 5)) >= 0
					) {
						yield 6;
					}
					else {
						yield 1;
					}
				}
				default -> 1;
			};
		}
		else if (c == '"') {
			while (super.skip()) {
				if (super.charAt(this.cursor) == '"') {
					super.skip();
					break;
				}
			}
		}
		else {
			super.skip();
		}
		return true;
	}

	@Override
	@Deprecated
	public void skip(int count) {
		while (--count >= 0 && this.skip());
	}

	@Override
	public boolean splitNewLinesInMultiLineTokens() {
		return true;
	}
}