package builderb0y.globescript.util;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

public class Util {

	public static final Hash.Strategy<Object> DEFAULT_STRATEGY = new Hash.Strategy<>() {

		@Override
		public int hashCode(Object o) {
			return Objects.hashCode(o);
		}

		@Override
		public boolean equals(Object a, Object b) {
			return Objects.equals(a, b);
		}
	};
	public static final Hash.Strategy<CharSequence> CHAR_SEQUENCE_STRATEGY = new Strategy<>() {

		@Override
		public int hashCode(CharSequence o) {
			if (o instanceof String) return o.hashCode(); //use cached hash code.
			int hash = 0;
			if (o != null) {
				for (int index = 0, length = o.length(); index < length; index++) {
					hash = hash * 31 + o.charAt(index);
				}
			}
			return hash;
		}

		@Override
		public boolean equals(CharSequence a, CharSequence b) {
			if (a == b) return true;
			if (a == null || b == null) return false;
			int length = a.length();
			if (b.length() != length) return false;
			for (int index = 0; index < length; index++) {
				if (a.charAt(index) != b.charAt(index)) return false;
			}
			return true;
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> Hash.Strategy<T> defaultHashStrategy() {
		return (Hash.Strategy<T>)(DEFAULT_STRATEGY);
	}

	public static Set<CharSequence> charSequenceSet(String... contents) {
		ObjectOpenCustomHashSet<CharSequence> set = new ObjectOpenCustomHashSet<>(contents.length, CHAR_SEQUENCE_STRATEGY);
		Collections.addAll(set, contents);
		return set;
	}

	public static boolean regionMatches(CharSequence a, int aOffset, CharSequence b, int bOffset, int length) {
		if (aOffset < 0 || bOffset < 0 || aOffset + length > a.length() || bOffset + length > b.length()) return false;
		for (int offset = 0; offset < length; offset++) {
			if (a.charAt(aOffset + offset) != b.charAt(bOffset + offset)) return false;
		}
		return true;
	}

	public static int indexOf(CharSequence sequence, char c) {
		return indexOf(sequence, c, 0, sequence.length());
	}

	public static int indexOf(CharSequence sequence, char c, int start, int end) {
		for (int index = start; index < end; index++) {
			if (sequence.charAt(index) == c) return index;
		}
		return -1;
	}

	public static int hexI2C(int i) {
		return i + (i < 10 ? '0' : 'A' - 10);
	}

	public static int hexC2I(int c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'z') return c + (10 - 'a');
		if (c >= 'A' && c <= 'Z') return c + (10 - 'A');
		return -1;
	}

	//everything below here copy-pasted from big globe.

	//////////////////////////////// modulus ////////////////////////////////

	//in java, the default % operator is fucking useless when a < 0 || b <= 0.
	//these methods are how you do modulus operations PROPERLY.
	//these implementations make the following guarantees:
	//	if (b > 0), then (a mod b) >= 0.
	//	if (b < 0), then (a mod b) <= 0.
	//	if (b == 0), then (a mod b) == 0.
	//		for floats and doubles, if (b == 0), then (a mod b) = 0 with the same sign as b.
	//	for floats and doubles, if (b is NaN), then (a mod b) = b.
	//	for floats and doubles, the sign bit of (a mod b) equals the sign bit of b.

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static int modulus(int a, int b) {
		if      (b > 0) return modulus_BP(a, b);
		else if (b < 0) return modulus_BN(a, b);
		else            return 0; //lim[b -> 0] (a mod b) = 0 for all values of a.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static int modulus_BP(int a, int b) {
		return (a %= b) < 0 ? a + b : a;
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static int modulus_BN(int a, int b) {
		return (a %= -b) > 0 ? a + b : a;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static long modulus(long a, long b) {
		if      (b > 0L) return modulus_BP(a, b);
		else if (b < 0L) return modulus_BN(a, b);
		else             return 0L; //lim[b -> 0] (a mod b) = 0 for all values of a.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0L).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static long modulus_BP(long a, long b) {
		return (a %= b) < 0L ? a + b : a;
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0L).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static long modulus_BN(long a, long b) {
		return (a %= -b) > 0L ? a + b : a;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static float modulus(float a, float b) {
		if      (b > 0.0F) return modulus_BP(a, b);
		else if (b < 0.0F) return modulus_BN(a, b);
		else               return b; //+0.0, -0.0, and NaN.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0.0F).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static float modulus_BP(float a, float b) {
		return (a %= b) + (a < 0.0F ? b : 0.0F); //adding 0.0 will convert -0.0 to +0.0.
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0.0F).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static float modulus_BN(float a, float b) {
		float mod = a % -b;
		//b is negative, so this acts as subtraction, not addition.
		if (mod > 0.0F) mod += b;
		//convert +0.0F to -0.0F.
		mod = Float.intBitsToFloat(Float.floatToRawIntBits(mod) | 0x8000_0000);
		return mod;
	}

	/** returns (a mod b) when nothing is known in advance about the signs of a or b. */
	public static double modulus(double a, double b) {
		if      (b > 0.0D) return modulus_BP(a, b);
		else if (b < 0.0D) return modulus_BN(a, b);
		else               return b; //+0.0, -0.0, and NaN.
	}

	/**
	returns (a mod b) when b is known in advance to be positive (> 0.0D).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static double modulus_BP(double a, double b) {
		return (a %= b) + (a < 0.0D ? b : 0.0D); //adding 0.0 will convert -0.0 to +0.0.
	}

	/**
	returns (a mod b) when b is known in advance to be negative (< 0.0D).
	this method makes no assumptions about a.
	*/
	@SuppressWarnings("UseOfRemainderOperator")
	public static double modulus_BN(double a, double b) {
		double mod = a % -b;
		//b is negative, so this acts as subtraction, not addition.
		if (mod > 0.0D) mod += b;
		//convert +0.0D to -0.0.
		mod = Double.longBitsToDouble(Double.doubleToRawLongBits(mod) | 0x8000_0000_0000_0000L);
		return mod;
	}

	//////////////////////////////// shifting ////////////////////////////////

	public static int signedLeftShift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? 0 : a << b;
		}
		else {
			return b <= -31 ? a >> 31 : a >> -b;
		}
	}

	public static long signedLeftShift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? 0 : a << b;
		}
		else {
			return b <= -63 ? a >> 63 : a >> -b;
		}
	}

	public static int signedRightShift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? a >> 31 : a >> b;
		}
		else {
			return b <= -31 ? 0 : a << -b;
		}
	}

	public static long signedRightShift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? a >> 63 : a >> b;
		}
		else {
			return b <= -63 ? 0 : a << -b;
		}
	}

	public static int unsignedLeftShift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? 0 : a << b;
		}
		else {
			return b <= -31 ? 0 : a >>> -b;
		}
	}

	public static long unsignedLeftShift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? 0 : a << b;
		}
		else {
			return b <= -63 ? 0 : a >>> -b;
		}
	}

	public static int unsignedRightShift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? 0 : a >>> b;
		}
		else {
			return b <= -31 ? 0 : a << -b;
		}
	}

	public static long unsignedRightShift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? 0 : a >>> b;
		}
		else {
			return b <= -63 ? 0 : a << -b;
		}
	}

	public static int negate(int x) {
		return -Math.max(x, -Integer.MAX_VALUE);
	}

	//////////////////////////////// powers ////////////////////////////////

	public static int pow(int a, int b) {
		if (b > 0) {
			int result = a;
			int bit = Integer.highestOneBit(b);
			while ((bit >>>= 1) != 0) {
				result *= result;
				if ((b & bit) != 0) {
					result *= a;
				}
			}
			return result;
		}
		else if (b < 0) {
			return (b & 1) == 0 ? negativeEven(a) : negativeOdd(a);
		}
		else {
			return 1;
		}
	}

	public static long pow(long a, int b) {
		if (b > 0L) {
			long result = a;
			int bit = Integer.highestOneBit(b);
			while ((bit >>>= 1) != 0) {
				result *= result;
				if ((b & bit) != 0) {
					result *= a;
				}
			}
			return result;
		}
		else if (b < 0L) {
			return (b & 1) == 0 ? negativeEven(a) : negativeOdd(a);
		}
		else {
			return 1;
		}
	}

	public static float pow(float a, int b) {
		if (b == 0) return 1.0F;
		boolean negative = b < 0;
		if (negative) b = -b;
		float result = a;
		int bit = Integer.highestOneBit(b);
		while ((bit >>>= 1) != 0) {
			result *= result;
			if ((b & bit) != 0) {
				result *= a;
			}
		}
		if (negative) result = 1.0F / result;
		return result;
	}

	public static double pow(double a, int b) {
		if (b == 0) return 1.0D;
		boolean negative = b < 0;
		if (negative) b = -b;
		double result = a;
		int bit = Integer.highestOneBit(b);
		while ((bit >>>= 1) != 0) {
			result *= result;
			if ((b & bit) != 0) {
				result *= a;
			}
		}
		if (negative) result = 1.0D / result;
		return result;
	}

	public static float pow(float a, float b) {
		return (float)(Math.pow((double)(a), (double)(b)));
	}

	public static float exp(float x) {
		return (float)(Math.exp((double)(x)));
	}

	public static int negativeEven(int operand) {
		if (operand ==  1) return 1;
		if (operand ==  0) throw new ArithmeticException("divide by 0");
		if (operand == -1) return 1;
		return 0;
	}

	public static long negativeEven(long operand) {
		if (operand ==  1L) return 1L;
		if (operand ==  0L) throw new ArithmeticException("divide by 0");
		if (operand == -1L) return 1L;
		return 0L;
	}

	public static int negativeOdd(int operand) {
		if (operand ==  1) return 1;
		if (operand ==  0) throw new ArithmeticException("divide by 0");
		if (operand == -1) return -1;
		return 0;
	}

	public static long negativeOdd(long operand) {
		if (operand ==  1L) return 1L;
		if (operand ==  0L) throw new ArithmeticException("divide by 0");
		if (operand == -1L) return -1L;
		return 0L;
	}
}