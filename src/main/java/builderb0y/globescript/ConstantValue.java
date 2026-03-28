package builderb0y.globescript;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.RawTypeModel;

public interface ConstantValue {

	public abstract RawTypeModel type();

	public default boolean isCompileTimeConstant() {
		return true;
	}

	public default boolean isRuntimeConstant() {
		return true;
	}

	public default @Nullable NumericPrecision precision() {
		return NumericPrecision.from(this.type());
	}

	public static enum NumericPrecision {
		BYTE,
		CHAR,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE;

		public static final NumericPrecision[] VALUES = values();

		public static NumericPrecision max(NumericPrecision a, NumericPrecision b) {
			return VALUES[Math.max(a.ordinal(), b.ordinal())];
		}

		public static NumericPrecision from(RawTypeModel type) {
			return switch (type.name) {
				case "byte" -> BYTE;
				case "char" -> CHAR;
				case "short" -> SHORT;
				case "int" -> INT;
				case "long" -> LONG;
				case "float" -> FLOAT;
				case "double" -> DOUBLE;
				default -> null;
			};
		}

		public RawTypeModel toType(ScriptEnvironment environment) {
			return switch (this) {
				case BYTE   -> environment.standardTypes.byte_;
				case CHAR   -> environment.standardTypes.char_;
				case SHORT  -> environment.standardTypes.short_;
				case INT    -> environment.standardTypes.int_;
				case LONG   -> environment.standardTypes.long_;
				case FLOAT  -> environment.standardTypes.float_;
				case DOUBLE -> environment.standardTypes.double_;
			};
		}
	}

	public static interface NumericConstantValue extends ConstantValue {

		@Override
		public default @NotNull NumericPrecision precision() {
			NumericPrecision precision = ConstantValue.super.precision();
			if (precision != null) return precision;
			else throw new IllegalStateException(this.getClass().getSimpleName() + " constructed with type " + this.type());
		}

		public abstract byte     byteValue();
		public abstract char     charValue();
		public abstract short   shortValue();
		public abstract int       intValue();
		public abstract long     longValue();
		public abstract float   floatValue();
		public abstract double doubleValue();
	}

	public static interface IntegerConstantValue extends NumericConstantValue {}

	public static interface FloatingPointConstantValue extends NumericConstantValue {}

	public static record NonConstantValue(RawTypeModel type) implements ConstantValue {

		@Override
		public boolean isCompileTimeConstant() {
			return false;
		}

		@Override
		public boolean isRuntimeConstant() {
			return false;
		}
	}

	public static record NullConstantValue(RawTypeModel type) implements ConstantValue {}

	public static record BooleanConstantValue(RawTypeModel type, boolean value) implements ConstantValue {}

	public static record ByteConstantValue(RawTypeModel type, byte value) implements IntegerConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record ShortConstantValue(RawTypeModel type, short value) implements IntegerConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record CharConstantValue(RawTypeModel type, char value) implements IntegerConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record IntConstantValue(RawTypeModel type, int value) implements IntegerConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record LongConstantValue(RawTypeModel type, long value) implements IntegerConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record FloatConstantValue(RawTypeModel type, float value) implements FloatingPointConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record DoubleConstantValue(RawTypeModel type, double value) implements FloatingPointConstantValue {

		@Override public byte     byteValue() { return (byte  )(this.value); }
		@Override public char     charValue() { return (char  )(this.value); }
		@Override public short   shortValue() { return (short )(this.value); }
		@Override public int       intValue() { return (int   )(this.value); }
		@Override public long     longValue() { return (long  )(this.value); }
		@Override public float   floatValue() { return (float )(this.value); }
		@Override public double doubleValue() { return (double)(this.value); }
	}

	public static record StringConstantValue(RawTypeModel type, String value) implements ConstantValue {}

	public static record EnumConstantValue(RawTypeModel type, String name) implements ConstantValue {}

	public static record DynamicConstantValue(RawTypeModel type) implements ConstantValue {

		@Override
		public boolean isCompileTimeConstant() {
			return false;
		}
	}
}