package builderb0y.globescript.datadriven;

import java.util.*;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.TokenInfo;

public class RawTypeModel implements TypeModifiersModel {

	public static final RawTypeModel[] EMPTY_ARRAY = {};
	public static final RawTypeModel
		NON_VALUE = new RawTypeModel(FINAL, "non_value", null, EMPTY_ARRAY, null),
		UNKNOWN = new RawTypeModel(FINAL, "unknown", null, EMPTY_ARRAY, null),
		ERROR = new RawTypeModel(FINAL, "error", null, EMPTY_ARRAY, null);

	public final int modifiers;
		@Override public int modifiers() { return this.modifiers; }
	public final String name;
	public final @Nullable RawTypeModel superClass;
	public final RawTypeModel[] superInterfaces;
	public ObjectLinkedOpenHashSet<RawTypeModel> assignableTypes;
	public final @Nullable Set<String> enumConstantNames;

	public RawTypeModel(int modifiers, String name, @Nullable RawTypeModel superClass, RawTypeModel[] interfaces, @Nullable Set<String> enumConstantNames) {
		this.modifiers = modifiers;
		this.name = name.intern();
		this.superClass = superClass;
		this.superInterfaces = interfaces;
		this.enumConstantNames = enumConstantNames;
	}

	public SequencedSet<RawTypeModel> getAssignableTypes() {
		ObjectLinkedOpenHashSet<RawTypeModel> set = this.assignableTypes;
		if (set == null) {
			set = new ObjectLinkedOpenHashSet<>();
			ObjectLinkedOpenHashSet<RawTypeModel> queue = new ObjectLinkedOpenHashSet<>();
			queue.add(this);
			while (!queue.isEmpty()) {
				RawTypeModel model = queue.removeFirst();
				if (set.add(model)) {
					if (model.superClass != null) queue.add(model.superClass);
					Collections.addAll(queue, model.superInterfaces);
				}
			}
			this.assignableTypes = set;
		}
		return set;
	}

	public boolean extendsOrImplements(RawTypeModel that) {
		return this.getAssignableTypes().contains(that);
	}

	public static RawTypeModel commonAncestor(TokenInfo... infos) {
		SequencedSet<RawTypeModel> set = null;
		for (TokenInfo info : infos) {
			if ((info.flags() & TokenInfo.FLAG_JUMPS) == 0) {
				if (set == null) {
					set = new LinkedHashSet<>(info.type().getAssignableTypes());
				}
				else {
					set.retainAll(info.type().getAssignableTypes());
				}
			}
		}
		return set == null || set.isEmpty() ? ERROR : set.getFirst();
	}

	public static RawTypeModel commonAncestor(List<TokenInfo> infos) {
		SequencedSet<RawTypeModel> set = null;
		for (TokenInfo info : infos) {
			if ((info.flags() & TokenInfo.FLAG_JUMPS) == 0) {
				if (set == null) {
					set = new LinkedHashSet<>(info.type().getAssignableTypes());
				}
				else {
					set.retainAll(info.type().getAssignableTypes());
				}
			}
		}
		return set == null || set.isEmpty() ? ERROR : set.getFirst();
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof RawTypeModel that &&
			this.name.equals(that.name)
		);
	}

	@Override
	public String toString() {
		return this.name;
	}
}