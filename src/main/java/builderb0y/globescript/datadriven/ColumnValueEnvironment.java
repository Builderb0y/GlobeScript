package builderb0y.globescript.datadriven;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.intellij.json.psi.JsonBooleanLiteral;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Range;

import builderb0y.globescript.Colors;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.ColumnValueEnvironment.AccessSchema;
import builderb0y.globescript.datadriven.CustomClassEnvironment.CyclicException;
import builderb0y.globescript.datadriven.CustomClassEnvironment.TypeElement;
import builderb0y.globescript.datadriven.EnvironmentModel.FieldData;
import builderb0y.globescript.datadriven.EnvironmentModel.MethodData;
import builderb0y.globescript.datadriven.EnvironmentModel.ParameterModel;
import builderb0y.globescript.datadriven.EnvironmentModel.VariableData;
import builderb0y.globescript.datadriven.GsEnv.StandardTypes;
import builderb0y.globescript.util.Util;

public class ColumnValueEnvironment extends DynamicRegistry<AccessSchema> {

	public static final int
		FLAG_ASSIGNABLE = 1 << 0,
		FLAG_Y_PROVIDED = 1 << 1,
		FLAG_XZ_PROVIDED = 1 << 2,
		FLAG_XYZ_PROVIDED = FLAG_XZ_PROVIDED | FLAG_Y_PROVIDED;

	public ColumnValueEnvironment(PackData packData) {
		super(packData, "bigglobe", "worldgen", "column_value");
	}

	@Override
	public AccessSchema compute(VirtualFile file) {
		PsiFile psiFile = PsiManager.getInstance(this.packData.projectData.project).findFile(file);
		if (
			psiFile instanceof JsonFile jsonFile &&
			jsonFile.getTopLevelValue() instanceof JsonObject root &&
			Util.findProperty(root, "params") instanceof JsonObject params &&
			Util.findProperty(params, "type") instanceof JsonStringLiteral type &&
			Util.findProperty(params, "is_3d") instanceof JsonBooleanLiteral is3D
		) {
			boolean border = (
				Util.findProperty(root, "type") instanceof JsonStringLiteral computeType &&
				switch (computeType.getValue()) {
					case "decision_tree", "bigglobe:decision_tree" -> true;
					default -> false;
				} &&
				Util.findProperty(root, "has_border") instanceof JsonBooleanLiteral hasBorder &&
				hasBorder.getValue()
			);
			return new AccessSchema(ID.parseBG(type.getValue()), is3D.getValue(), border);
		}
		return null;
	}

	public void setupEnvironment(EnvironmentModel environment, VirtualFile file, int flags) {
		for (Map.Entry<ID, AccessSchema> entry : this.elements.entrySet()) {
			entry.getValue().setupEnvironment(environment, entry.getKey().toString(), flags);
		}
		ID id = this.idOf(file);
		if (id != null) {
			for (Map.Entry<ID, AccessSchema> entry : this.elements.entrySet()) {
				if (entry.getKey().namespace().equals(id.namespace())) {
					int start = relativize(entry.getKey().path(), id.path());
					if (start >= 0) {
						String shorthand = entry.getKey().path().substring(start);
						entry.getValue().setupEnvironment(environment, shorthand, flags);
					}
				}
			}
			environment.addImportedValue(new VariableData("column", Colors.GLOBAL, new TokenInfo(this.packData.projectData.environment().standardTypes.columnStorage)));
		}
		if ((flags & FLAG_Y_PROVIDED) != 0) {
			environment.addVariable(new VariableData("y", Colors.GLOBAL, new TokenInfo(this.packData.projectData.environment().standardTypes.int_)));
		}
	}

	public static int relativize(String selfPath, String callerPath) {
		int start = 0;
		while (true) {
			int selfSlash = selfPath.indexOf('/', start);
			int callerSlash = callerPath.indexOf('/', start);
			if (selfSlash >= 0) {
				if (callerSlash >= 0) {
					if (selfSlash == callerSlash && selfPath.regionMatches(start, callerPath, start, selfSlash - start)) {
						start = selfSlash + 1; //a:b/c/... trying to reference a:b/c/...
					}
					else {
						return -1; //a:123/... trying to reference a:456/...
					}
				}
				else {
					return start; //a:b/123 trying to reference a:b/c/...
				}
			}
			else {
				if (callerSlash >= 0) {
					return -1; //a:b/c/... trying to reference a:b/123
				}
				else {
					return start; //a:b/123 trying to reference a:b/456
				}
			}
		}
	}

	public class AccessSchema extends DynamicRegistryElement {

		public static final AtomicInteger BORDER_COUNTER = new AtomicInteger();

		public final ID type;
		public final boolean is3D;
		public final boolean border;

		public AccessSchema(ID type, boolean is3D, boolean border) {
			this.type = type;
			this.is3D = is3D;
			this.border = border;
		}

		public RawTypeModel resolveType() {
			CustomClassEnvironment environment = ColumnValueEnvironment.this.packData.customClasses;
			if (environment != null && environment.get(this.type) instanceof TypeElement type) try {
				return type.resolve(new HashSet<>());
			}
			catch (CyclicException ignored) {}
			return null;
		}

		public void setupEnvironment(EnvironmentModel environment, String name, int flags) {
			RawTypeModel basicType = this.resolveType();
			if (basicType != null) {
				StandardTypes standardTypes = ColumnValueEnvironment.this.packData.projectData.environment().standardTypes;
				RawTypeModel borderedType;
				if (this.border) {
					borderedType = new RawTypeModel(TypeModifiersModel.FINAL, "Border_" + BORDER_COUNTER.getAndIncrement(), standardTypes.object, RawTypeModel.EMPTY_ARRAY, null);
					environment.addInstanceField(new FieldData(borderedType, "value", Colors.INSTANCE_FIELD, new TokenInfo(basicType, TokenInfo.FLAG_ASSIGNABLE)));
					environment.addInstanceField(new FieldData(borderedType, "border", Colors.INSTANCE_FIELD, new TokenInfo(standardTypes.double_, TokenInfo.FLAG_ASSIGNABLE)));
				}
				else {
					borderedType = basicType;
				}
				TokenInfo info = new TokenInfo(borderedType, (flags & FLAG_ASSIGNABLE) != 0 ? TokenInfo.FLAG_ASSIGNABLE : 0);
				RawTypeModel column  = standardTypes.columnStorage;
				RawTypeModel lookup  = standardTypes.columnLookup;
				RawTypeModel intType = standardTypes.int_;
				for (int providedArguments = 0; providedArguments <= 3; providedArguments++) {
					if (isValidAccess(AccessMode.COLUMN, this.is3D, flags, providedArguments)) {
						environment.addInstanceMethod(new MethodData(name, Colors.INSTANCE_METHOD, column, info, createParameters(intType, providedArguments)));
						if (providedArguments == 0) {
							environment.addInstanceField(new FieldData(column, name, Colors.INSTANCE_FIELD, info));
						}
					}
					if (isValidAccess(AccessMode.LOOKUP, this.is3D, flags, providedArguments)) {
						environment.addInstanceMethod(new MethodData(name, Colors.INSTANCE_METHOD, lookup, info, createParameters(intType, providedArguments)));
						if (providedArguments == 0) {
							environment.addInstanceField(new FieldData(lookup, name, Colors.INSTANCE_FIELD, info));
						}
					}
				}
			}
		}
	}

	public static enum AccessMode {
		COLUMN,
		LOOKUP;
	}

	public static boolean isValidAccess(
		AccessMode access,
		boolean is3D,
		@MagicConstant(flags = { FLAG_XZ_PROVIDED, FLAG_Y_PROVIDED })
		int implicitArguments,
		@Range(from = 0L, to = 3L)
		int explicitArguments
	) {
		if (!is3D && (explicitArguments & 1) != 0) {
			return false;
		}
		if (access == AccessMode.COLUMN && explicitArguments >= 2) {
			return false;
		}
		int have = implicitArguments | switch (explicitArguments) {
			case 0 -> 0;
			case 1 -> FLAG_Y_PROVIDED;
			case 2 -> FLAG_XZ_PROVIDED;
			case 3 -> FLAG_XYZ_PROVIDED;
			default -> throw new IllegalArgumentException(Integer.toString(explicitArguments));
		};
		if (access == AccessMode.LOOKUP && (have & FLAG_XZ_PROVIDED) == 0) return false;
		if (is3D                        && (have & FLAG_Y_PROVIDED ) == 0) return false;

		return true;
	}

	public static ParameterModel[] createParameters(RawTypeModel int_, int count) {
		return switch (count) {
			case 0 -> ParameterModel.EMPTY_ARRAY;
			case 1 -> new ParameterModel[] {
				new ParameterModel("y", int_, false)
			};
			case 2 -> new ParameterModel[] {
				new ParameterModel("x", int_, false),
				new ParameterModel("z", int_, false)
			};
			case 3 -> new ParameterModel[] {
				new ParameterModel("x", int_, false),
				new ParameterModel("y", int_, false),
				new ParameterModel("z", int_, false)
			};
			default -> throw new IllegalArgumentException(Integer.toString(count));
		};
	}
}