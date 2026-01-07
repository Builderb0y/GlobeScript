package builderb0y.globescript;

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

	public static int hexI2C(int i) {
		return i + (i < 10 ? '0' : 'A' - 10);
	}

	public static int hexC2I(int c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'z') return c + (10 - 'a');
		if (c >= 'A' && c <= 'Z') return c + (10 - 'A');
		return -1;
	}
}