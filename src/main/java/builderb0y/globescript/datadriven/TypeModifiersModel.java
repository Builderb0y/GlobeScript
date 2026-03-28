package builderb0y.globescript.datadriven;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public interface TypeModifiersModel {

	public static final int
		FINAL             = (1 << 0),
		ABSTRACT          = (1 << 1),
		INTERFACE_ONLY    = (1 << 2),
		RECORD_ONLY       = (1 << 3),
		ENUM              = (1 << 4),
		INTERFACE_IMPLIES = INTERFACE_ONLY | ABSTRACT,
		RECORD_IMPLIES    =    RECORD_ONLY | FINAL;

	public static final Object2IntMap<String> BY_NAME = new Object2IntOpenHashMap<>();
	public static final Object INITIALIZER = new Object() {{
		BY_NAME.put("final",     FINAL            );
		BY_NAME.put("abstract",  ABSTRACT         );
		BY_NAME.put("interface", INTERFACE_IMPLIES);
		BY_NAME.put("record",    RECORD_IMPLIES   );
		BY_NAME.put("enum",      ENUM             );
	}};

	public abstract int modifiers();

	public default boolean isFinal    () { return (this.modifiers() & FINAL         ) != 0; }
	public default boolean isAbstract () { return (this.modifiers() & ABSTRACT      ) != 0; }
	public default boolean isInterface() { return (this.modifiers() & INTERFACE_ONLY) != 0; }
	public default boolean isRecord   () { return (this.modifiers() & RECORD_ONLY   ) != 0; }
	public default boolean isEnum     () { return (this.modifiers() & ENUM          ) != 0; }
}