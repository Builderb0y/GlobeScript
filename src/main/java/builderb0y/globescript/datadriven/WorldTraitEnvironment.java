package builderb0y.globescript.datadriven;

import java.util.HashSet;
import java.util.Map;

import com.intellij.json.psi.JsonBooleanLiteral;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import builderb0y.globescript.Colors;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.ColumnValueEnvironment.AccessMode;
import builderb0y.globescript.datadriven.CustomClassEnvironment.CyclicException;
import builderb0y.globescript.datadriven.CustomClassEnvironment.TypeElement;
import builderb0y.globescript.datadriven.EnvironmentModel.FieldData;
import builderb0y.globescript.datadriven.EnvironmentModel.MethodData;
import builderb0y.globescript.datadriven.GsEnv.StandardTypes;
import builderb0y.globescript.datadriven.WorldTraitEnvironment.WorldTraitElement;
import builderb0y.globescript.util.Util;

public class WorldTraitEnvironment extends DynamicRegistry<WorldTraitElement> {

	public WorldTraitEnvironment(PackData packData) {
		super(packData, "bigglobe", "worldgen", "world_trait");
	}

	@Override
	public WorldTraitElement compute(VirtualFile file) {
		PsiFile psiFile = PsiManager.getInstance(this.packData.projectData.project).findFile(file);
		if (
			psiFile instanceof JsonFile jsonFile &&
			jsonFile.getTopLevelValue() instanceof JsonObject root &&
			Util.findProperty(root, "type") instanceof JsonStringLiteral type &&
			Util.findProperty(root, "is_3d") instanceof JsonBooleanLiteral is3D
		) {
			return new WorldTraitElement(ID.parseBG(type.getValue()), is3D.getValue());
		}
		return null;
	}

	public void setupEnvironment(EnvironmentModel environment, int flags) {
		for (Map.Entry<ID, WorldTraitElement> entry : this.elements.entrySet()) {
			entry.getValue().setupEnvironment(environment, entry.getKey().toString(), flags);
		}
	}

	public class WorldTraitElement extends DynamicRegistryElement {

		public final ID type;
		public final boolean is3D;

		public WorldTraitElement(ID type, boolean is3D) {
			this.type = type;
			this.is3D = is3D;
		}

		public RawTypeModel resolveType() {
			CustomClassEnvironment environment = WorldTraitEnvironment.this.packData.customClasses;
			if (environment != null && environment.get(this.type) instanceof TypeElement type) try {
				return type.resolve(new HashSet<>());
			}
			catch (CyclicException ignored) {}
			return null;
		}

		public void setupEnvironment(EnvironmentModel environment, String name, int flags) {
			RawTypeModel type = this.resolveType();
			if (type != null) {
				StandardTypes standardTypes = WorldTraitEnvironment.this.packData.projectData.environment().standardTypes;
				TokenInfo info = new TokenInfo(type, (flags & ColumnValueEnvironment.FLAG_ASSIGNABLE) != 0 ? TokenInfo.FLAG_ASSIGNABLE : 0);
				RawTypeModel column  = standardTypes.columnWorldTraits;
				RawTypeModel lookup  = standardTypes.lookupWorldTraits;
				RawTypeModel intType = standardTypes.int_;
				for (int providedArguments = 0; providedArguments <= 3; providedArguments++) {
					if (ColumnValueEnvironment.isValidAccess(AccessMode.COLUMN, this.is3D, flags, providedArguments)) {
						environment.addInstanceMethod(new MethodData(name, Colors.INSTANCE_METHOD, column, info, ColumnValueEnvironment.createParameters(intType, providedArguments)));
						if (providedArguments == 0) {
							environment.addInstanceField(new FieldData(column, name, Colors.INSTANCE_FIELD, info));
						}
					}
					if (ColumnValueEnvironment.isValidAccess(AccessMode.LOOKUP, this.is3D, flags, providedArguments)) {
						environment.addInstanceMethod(new MethodData(name, Colors.INSTANCE_METHOD, lookup, info, ColumnValueEnvironment.createParameters(intType, providedArguments)));
						if (providedArguments == 0) {
							environment.addInstanceField(new FieldData(lookup, name, Colors.INSTANCE_FIELD, info));
						}
					}
				}
			}
		}
	}
}