package builderb0y.globescript;


import builderb0y.globescript.ConstantValue.*;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;
import builderb0y.globescript.util.Util;

public class ConstantFolding {

	public static interface UnaryConstantFolder {

		public abstract TokenInfo fold(ScriptEnvironment environment, Token value);
	}

	public static enum UnaryPlusConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			if (!value.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
				value.withTooltip("Can't apply unary plus to " + value.info.type());
			}
			return new TokenInfo(value.info.type(), value.info.flags());
		}
	}

	public static enum UnaryMinusConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			if (value.info.constant() instanceof NumericConstantValue numericConstant) {
				return new TokenInfo(
					switch (numericConstant.precision()) {
						case BYTE -> new ByteConstantValue(environment.standardTypes.byte_, (byte)(-numericConstant.byteValue()));
						case SHORT -> new ShortConstantValue(environment.standardTypes.short_, (short)(-numericConstant.shortValue()));
						case CHAR -> new IntConstantValue(environment.standardTypes.int_, -numericConstant.charValue());
						case INT -> new IntConstantValue(environment.standardTypes.int_, -numericConstant.intValue());
						case LONG -> new LongConstantValue(environment.standardTypes.long_, -numericConstant.longValue());
						case FLOAT -> new FloatConstantValue(environment.standardTypes.float_, -numericConstant.floatValue());
						case DOUBLE -> new DoubleConstantValue(environment.standardTypes.double_, -numericConstant.doubleValue());
					},
					value.info.flags()
				);
			}
			else {
				if (!value.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					value.withTooltip("Can't apply unary minus to " + value.info.type());
				}
				return value.info;
			}
		}
	}

	public static enum BooleanNotConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			if (value.info.constant() instanceof BooleanConstantValue booleanConstant) {
				return new TokenInfo(new BooleanConstantValue(environment.standardTypes.boolean_, !booleanConstant.value()));
			}
			else {
				if (!value.info.isAssignableToOrCanCast(environment, environment.standardTypes.boolean_, Plicity.IMPLICIT)) {
					value.withTooltip("Can't apply boolean 'not' operation to " + value.info.type());
				}
				return value.info;
			}
		}
	}

	public static enum BitwiseNegateConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			if (value.info.constant() instanceof IntegerConstantValue integerConstant) {
				return new TokenInfo(
					switch (integerConstant.precision()) {
						case BYTE -> new ByteConstantValue(environment.standardTypes.byte_, (byte)(~integerConstant.byteValue()));
						case SHORT -> new ShortConstantValue(environment.standardTypes.short_, (short)(~integerConstant.shortValue()));
						case CHAR -> new IntConstantValue(environment.standardTypes.int_, ~integerConstant.charValue());
						case INT -> new IntConstantValue(environment.standardTypes.int_, ~integerConstant.intValue());
						case LONG -> new LongConstantValue(environment.standardTypes.long_, ~integerConstant.longValue());
						case FLOAT, DOUBLE -> integerConstant; //???
					},
					value.info.flags()
				);
			}
			else {
				if (!value.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveInteger, Plicity.IMPLICIT)) {
					value.withTooltip("Can't bitwise negate " + value.info.type());
				}
				return value.info;
			}
		}
	}

	public static enum PreUpdateConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			return new TokenInfo(value.info.type(), (value.info.flags() & (TokenInfo.FLAG_GENERIC | TokenInfo.FLAG_JUMPS)) | TokenInfo.FLAG_STATEMENT);
		}
	}

	public static enum PostUpdateConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			return new TokenInfo(value.info.type(), (value.info.flags() & (TokenInfo.FLAG_GENERIC | TokenInfo.FLAG_JUMPS)) | TokenInfo.FLAG_STATEMENT);
		}
	}

	public static enum VoidUpdateConstantFolder implements UnaryConstantFolder {
		INSTANCE;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token value) {
			return new TokenInfo(environment.standardTypes.void_, (value.info.flags() & TokenInfo.FLAG_JUMPS) | TokenInfo.FLAG_STATEMENT);
		}
	}

	public static interface BinaryConstantFolder {

		public abstract TokenInfo fold(ScriptEnvironment environment, Token left, Token right);
	}

	public static enum ArithmeticConstantFolder implements BinaryConstantFolder {
		ADD,
		SUB,
		MUL,
		DIV,
		MOD;

		public RawTypeModel merge(ScriptEnvironment environment, TokenInfo left, TokenInfo right) {
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.int_, Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.int_, Plicity.IMPLICIT)) return environment.standardTypes.int_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.long_, Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.long_, Plicity.IMPLICIT)) return environment.standardTypes.long_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.float_, Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.float_, Plicity.IMPLICIT)) return environment.standardTypes.float_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.double_, Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.double_, Plicity.IMPLICIT)) return environment.standardTypes.double_;
			return environment.standardTypes.int_;
		}

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token left, Token right) {
			if (left.info.constant() instanceof NumericConstantValue leftNumeric && right.info.constant() instanceof NumericConstantValue rightNumeric) {
				NumericPrecision precision = NumericPrecision.max(leftNumeric.precision(), rightNumeric.precision());
				return new TokenInfo(switch (precision) {
					case BYTE, CHAR, SHORT, INT -> new IntConstantValue(environment.standardTypes.int_, this.applyAsInt(leftNumeric.intValue(), rightNumeric.intValue()));
					case LONG -> new LongConstantValue(environment.standardTypes.long_, this.applyAsLong(leftNumeric.longValue(), rightNumeric.longValue()));
					case FLOAT -> new FloatConstantValue(environment.standardTypes.float_, this.applyAsFloat(leftNumeric.floatValue(), rightNumeric.floatValue()));
					case DOUBLE -> new DoubleConstantValue(environment.standardTypes.double_, this.applyAsDouble(leftNumeric.doubleValue(), rightNumeric.doubleValue()));
				});
			}
			else {
				if (!left.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					left.withTooltip("Can't implicitly cast " + left.info.type() + " to " + environment.standardTypes.primitiveNumber + " for " + this + " operation");
				}
				if (!right.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to " + environment.standardTypes.primitiveNumber + " for " + this + " operation");
				}
				return new TokenInfo(
					this.merge(environment, left.info, right.info),
					(left.info.flags() | right.info.flags()) & TokenInfo.FLAG_JUMPS
				);
			}
		}

		public int applyAsInt(int left, int right) {
			return switch (this) {
				case ADD -> left + right;
				case SUB -> left - right;
				case MUL -> left * right;
				case DIV -> Math.floorDiv(left, right);
				case MOD -> Util.modulus(left, right);
			};
		}

		public long applyAsLong(long left, long right) {
			return switch (this) {
				case ADD -> left + right;
				case SUB -> left - right;
				case MUL -> left * right;
				case DIV -> Math.floorDiv(left, right);
				case MOD -> Util.modulus(left, right);
			};
		}

		public float applyAsFloat(float left, float right) {
			return switch (this) {
				case ADD -> left + right;
				case SUB -> left - right;
				case MUL -> left * right;
				case DIV -> left / right;
				case MOD -> Util.modulus(left, right);
			};
		}

		public double applyAsDouble(double left, double right) {
			return switch (this) {
				case ADD -> left + right;
				case SUB -> left - right;
				case MUL -> left * right;
				case DIV -> left / right;
				case MOD -> Util.modulus(left, right);
			};
		}
	}

	public static enum PowerConstantFolder implements BinaryConstantFolder {
		INSTANCE;

		public RawTypeModel merge(ScriptEnvironment environment, TokenInfo left, TokenInfo right) {
			if (right.isAssignableToOrCanCast(environment, environment.standardTypes.long_, Plicity.IMPLICIT)) {
				if (left.isAssignableToOrCanCast(environment, environment.standardTypes.int_, Plicity.IMPLICIT)) return environment.standardTypes.int_;
			}
			else if (right.isAssignableToOrCanCast(environment, environment.standardTypes.float_, Plicity.IMPLICIT)) {
				return left.isAssignableToOrCanCast(environment, environment.standardTypes.float_, Plicity.IMPLICIT) ? environment.standardTypes.float_ : environment.standardTypes.double_;
			}
			else if (right.isAssignableToOrCanCast(environment, environment.standardTypes.double_, Plicity.IMPLICIT)) {
				return environment.standardTypes.double_;
			}
			return left.type();
		}

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token left, Token right) {
			if (left.info.constant() instanceof NumericConstantValue leftConstant && right.info.constant() instanceof NumericConstantValue rightConstant && !(rightConstant instanceof LongConstantValue)) {
				NumericPrecision leftPrecision = leftConstant.precision();
				NumericPrecision rightPrecision = rightConstant.precision();
				return new TokenInfo(switch (rightPrecision) {
					case BYTE, SHORT, CHAR, INT -> this.toIntPower(environment, leftConstant, rightConstant.intValue());
					case LONG -> {
						right.withTooltip("long exponents are not supported");
						yield this.toIntPower(environment, leftConstant, Math.clamp(rightConstant.longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE));
					}
					case FLOAT -> (
						leftPrecision == NumericPrecision.DOUBLE
						? new DoubleConstantValue(environment.standardTypes.double_, Math.pow(leftConstant.doubleValue(), rightConstant.doubleValue()))
						: new FloatConstantValue(environment.standardTypes.float_, Util.pow(leftConstant.floatValue(), rightConstant.floatValue()))
					);
					case DOUBLE -> new DoubleConstantValue(environment.standardTypes.double_, Math.pow(leftConstant.doubleValue(), rightConstant.doubleValue()));
				});
			}
			else {
				if (!left.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					left.withTooltip("Can't implicitly cast " + left.info.type() + " to " + environment.standardTypes.primitiveNumber + " for power operation");
				}
				if (!right.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to " + environment.standardTypes.primitiveNumber + " for power operation");
				}
				else if (right.info.type() == environment.standardTypes.long_) {
					right.withTooltip("long exponents are not supported");
				}
				return new TokenInfo(
					this.merge(environment, left.info, right.info),
					(left.info.flags() | right.info.flags()) & TokenInfo.FLAG_JUMPS
				);
			}
		}

		public ConstantValue toIntPower(ScriptEnvironment environment, NumericConstantValue leftConstant, int exponent) {
			return switch (leftConstant.precision()) {
				case BYTE, SHORT, CHAR, INT -> new IntConstantValue(environment.standardTypes.int_, Util.pow(leftConstant.intValue(), exponent));
				case LONG -> new LongConstantValue(environment.standardTypes.long_, Util.pow(leftConstant.longValue(), exponent));
				case FLOAT -> new FloatConstantValue(environment.standardTypes.float_, Util.pow(leftConstant.floatValue(), exponent));
				case DOUBLE -> new DoubleConstantValue(environment.standardTypes.double_, Util.pow(leftConstant.doubleValue(), exponent));
			};
		}
	}

	public static enum BitwiseConstantFolder implements BinaryConstantFolder {
		AND,
		OR,
		XOR;

		public RawTypeModel merge(ScriptEnvironment environment, TokenInfo left, TokenInfo right) {
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.boolean_, Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.boolean_, Plicity.IMPLICIT)) return environment.standardTypes.boolean_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.byte_,    Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.byte_,    Plicity.IMPLICIT)) return environment.standardTypes.byte_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.char_,    Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.char_,    Plicity.IMPLICIT)) return environment.standardTypes.char_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.short_,   Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.short_,   Plicity.IMPLICIT)) return environment.standardTypes.short_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.int_,     Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.int_,     Plicity.IMPLICIT)) return environment.standardTypes.int_;
			if (left.isAssignableToOrCanCast(environment, environment.standardTypes.long_,    Plicity.IMPLICIT) || right.isAssignableToOrCanCast(environment, environment.standardTypes.long_,    Plicity.IMPLICIT)) return environment.standardTypes.long_;
			return environment.standardTypes.byte_;
		}

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token left, Token right) {
			if (left.info.constant() instanceof IntegerConstantValue leftConstant && right.info.constant() instanceof IntegerConstantValue rightConstant) {
				NumericPrecision precision = NumericPrecision.max(leftConstant.precision(), rightConstant.precision());
				return new TokenInfo(switch (precision) {
					case BYTE -> new ByteConstantValue(environment.standardTypes.byte_, this.applyAsByte(leftConstant.byteValue(), rightConstant.byteValue()));
					case SHORT -> new ShortConstantValue(environment.standardTypes.short_, this.applyAsShort(leftConstant.shortValue(), rightConstant.shortValue()));
					case CHAR -> new CharConstantValue(environment.standardTypes.char_, this.applyAsChar(leftConstant.charValue(), rightConstant.charValue()));
					case INT -> new IntConstantValue(environment.standardTypes.int_, this.applyAsInt(leftConstant.intValue(), rightConstant.intValue()));
					case LONG -> new LongConstantValue(environment.standardTypes.long_, this.applyAsLong(leftConstant.longValue(), rightConstant.longValue()));
					case FLOAT -> new NonConstantValue(environment.standardTypes.float_);
					case DOUBLE -> new NonConstantValue(environment.standardTypes.double_);
				});
			}
			else {
				if (!left.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveBitwise, Plicity.IMPLICIT)) {
					left.withTooltip("Can't implicitly cast " + left.info.type() + " to " + environment.standardTypes.primitiveBitwise + " for " + this + " operation");
				}
				if (!right.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveBitwise, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to " + environment.standardTypes.primitiveBitwise + " for " + this + " operation");
				}
				return new TokenInfo(
					this.merge(environment, left.info, right.info),
					(left.info.flags() | right.info.flags()) & TokenInfo.FLAG_JUMPS
				);
			}
		}

		public byte applyAsByte(byte left, byte right) {
			return switch (this) {
				case AND -> (byte)(left & right);
				case OR  -> (byte)(left | right);
				case XOR -> (byte)(left ^ right);
			};
		}

		public char applyAsChar(char left, char right) {
			return switch (this) {
				case AND -> (char)(left & right);
				case OR  -> (char)(left | right);
				case XOR -> (char)(left ^ right);
			};
		}

		public short applyAsShort(short left, short right) {
			return switch (this) {
				case AND -> (short)(left & right);
				case OR  -> (short)(left | right);
				case XOR -> (short)(left ^ right);
			};
		}

		public int applyAsInt(int left, int right) {
			return switch (this) {
				case AND -> left & right;
				case OR  -> left | right;
				case XOR -> left ^ right;
			};
		}

		public long applyAsLong(long left, long right) {
			return switch (this) {
				case AND -> left & right;
				case OR  -> left | right;
				case XOR -> left ^ right;
			};
		}
	}

	public static enum SignedShiftConstantFolder implements BinaryConstantFolder {
		LEFT,
		RIGHT;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token left, Token right) {
			if (left.info.constant() instanceof NumericConstantValue leftConstant && right.info.constant() instanceof IntegerConstantValue rightConstant) {
				int shift = Math.clamp(rightConstant.longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE);
				return new TokenInfo(switch (leftConstant.precision()) {
					case BYTE, CHAR, SHORT, INT -> new IntConstantValue(environment.standardTypes.int_, this.applyAsInt(leftConstant.intValue(), shift));
					case LONG -> new LongConstantValue(environment.standardTypes.long_, this.applyAsLong(leftConstant.longValue(), shift));
					case FLOAT -> new FloatConstantValue(environment.standardTypes.float_, this.applyAsFloat(leftConstant.floatValue(), shift));
					case DOUBLE -> new DoubleConstantValue(environment.standardTypes.double_, this.applyAsDouble(rightConstant.doubleValue(), shift));
				});
			}
			else {
				if (!left.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveNumber, Plicity.IMPLICIT)) {
					left.withTooltip("Can't implicitly cast " + left.info.type() + " to " + environment.standardTypes.primitiveNumber + " for shift operation");
				}
				if (!right.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveInteger, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to " + environment.standardTypes.primitiveInteger + " for shift operation");
				}
				return new TokenInfo(left.info.type(), (left.info.flags() | right.info.flags()) & TokenInfo.FLAG_JUMPS);
			}
		}

		public int applyAsInt(int value, int shift) {
			return switch (this) {
				case LEFT  -> Util.signedLeftShift(value, shift);
				case RIGHT -> Util.signedRightShift(value, shift);
			};
		}

		public long applyAsLong(long value, int shift) {
			return switch (this) {
				case LEFT  -> Util.signedLeftShift(value, shift);
				case RIGHT -> Util.signedRightShift(value, shift);
			};
		}

		public float applyAsFloat(float value, int shift) {
			return switch (this) {
				case LEFT -> Math.scalb(value, shift);
				case RIGHT -> Math.scalb(value, Util.negate(shift));
			};
		}

		public double applyAsDouble(double value, int shift) {
			return switch (this) {
				case LEFT -> Math.scalb(value, shift);
				case RIGHT -> Math.scalb(value, Util.negate(shift));
			};
		}
	}

	public static enum UnsignedShiftConstantFolder implements BinaryConstantFolder {
		LEFT,
		RIGHT;

		@Override
		public TokenInfo fold(ScriptEnvironment environment, Token left, Token right) {
			if (left.info.constant() instanceof IntegerConstantValue leftConstant && right.info.constant() instanceof IntegerConstantValue rightConstant) {
				int shift = Math.clamp(rightConstant.longValue(), Integer.MIN_VALUE, Integer.MAX_VALUE);
				return new TokenInfo(switch (leftConstant.precision()) {
					case BYTE, CHAR, SHORT, INT -> new IntConstantValue(environment.standardTypes.int_, this.applyAsInt(leftConstant.intValue(), shift));
					case LONG -> new LongConstantValue(environment.standardTypes.long_, this.applyAsLong(leftConstant.longValue(), shift));
					case FLOAT -> new NonConstantValue(environment.standardTypes.float_);
					case DOUBLE -> new NonConstantValue(environment.standardTypes.double_);
				});
			}
			else {
				if (!left.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveInteger, Plicity.IMPLICIT)) {
					left.withTooltip("Can't implicitly cast " + left.info.type() + " to " + environment.standardTypes.primitiveInteger + " for unsigned shift operation");
				}
				if (!right.info.isAssignableToOrCanCast(environment, environment.standardTypes.primitiveInteger, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to " + environment.standardTypes.primitiveInteger + " for unsigned shift operation");
				}
				return new TokenInfo(left.info.type(), (left.info.flags() | right.info.flags()) & TokenInfo.FLAG_JUMPS);
			}
		}

		public int applyAsInt(int value, int shift) {
			return switch (this) {
				case LEFT -> Util.unsignedLeftShift(value, shift);
				case RIGHT -> Util.unsignedRightShift(value, shift);
			};
		}

		public long applyAsLong(long value, int shift) {
			return switch (this) {
				case LEFT -> Util.unsignedLeftShift(value, shift);
				case RIGHT -> Util.unsignedRightShift(value, shift);
			};
		}
	}
}