package builderb0y.globescript.datadriven;

import java.util.*;

import com.intellij.json.psi.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import builderb0y.globescript.Colors;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.CustomClassEnvironment.CustomElement;
import builderb0y.globescript.datadriven.EnvironmentModel.FieldData;
import builderb0y.globescript.datadriven.EnvironmentModel.MethodData;
import builderb0y.globescript.datadriven.EnvironmentModel.ParameterModel;
import builderb0y.globescript.datadriven.EnvironmentModel.TypeData;
import builderb0y.globescript.util.Util;

public class CustomClassEnvironment extends DynamicRegistry<CustomElement> {

	public CustomClassEnvironment(PackData packData) {
		super(packData, "bigglobe", "custom_class");
	}

	public void setupEnvironment(EnvironmentModel environment) {
		for (CustomElement value : this.elements.values()) {
			value.applyTo(environment);
		}
	}

	public CustomElement get(ID id) {
		CustomElement element = this.elements.get(id);
		return element == SpecialMarker.ABSENT ? null : element;
		/*
		if (element != null) {
			if (element == SpecialMarker.ABSENT) return null;
		}
		else {
			VirtualFile namespace = this.dataFolder.findChild(id.namespace());
			if (namespace != null) {
				VirtualFile bigglobe = namespace.findChild("bigglobe");
				if (bigglobe != null) {
					VirtualFile customClasses = bigglobe.findChild("custom_class");
					if (customClasses != null) {
						VirtualFile elementFile = customClasses;
						for (String part : id.path().split("/")) {
							elementFile = elementFile.findChild(part);
							if (elementFile == null) break;
						}
						if (elementFile != null) {
							element = this.compute(elementFile);
						}
					}
				}
			}
			this.elements.put(id, element == null ? SpecialMarker.ABSENT : element);
		}
		return element;
		*/
	}

	@Override
	public CustomElement compute(VirtualFile file) {
		PsiFile psiFile = PsiManager.getInstance(this.packData.projectData.project).findFile(file);
		if (
			psiFile instanceof JsonFile jsonFile &&
			jsonFile.getTopLevelValue() instanceof JsonObject root &&
			Util.findProperty(root, "element_type") instanceof JsonStringLiteral elementType
		) {
			return switch (elementType.getValue()) {
				case "bigglobe:class/builtin",     "class/builtin"     -> this.parseBuiltinClass(root);
				case "bigglobe:class/normal",      "class/normal"      -> this.parseNormalClass(root);
				case "bigglobe:class/enum",        "class/enum"        -> this.parseEnumClass(root);
				//case "bigglobe:class/interface",   "class/interface"   -> {}
				case "bigglobe:field/normal",      "field/normal"      -> this.parseNormalField(root);
				case "bigglobe:field/constant",    "field/constant"    -> this.parseConstantField(root);
				case "bigglobe:field/enum",        "field/enum"        -> this.parseEnumField(root);
				case "bigglobe:method/static",     "method/static"     -> this.parseStaticMethod(root);
				case "bigglobe:method/normal",     "method/normal"     -> this.parseNormalMethod(root);
				case "bigglobe:method/override",   "method/override"   -> this.parseOverrideMethod(root);
				case "bigglobe:method/abstract",   "method/abstract"   -> this.parseAbstractMethod(root);
				case "bigglobe:property/normal",   "property/normal"   -> this.parseNormalProperty(root);
				case "bigglobe:property/override", "property/override" -> this.parseOverridePRoperty(root);
				case "bigglobe:property/abstract", "property/abstract" -> this.parseAbstractProperty(root);
				default -> null;
			};
		}
		else {
			return null;
		}
	}

	public BuiltinClassElement parseBuiltinClass(JsonObject root) {
		if (Util.findProperty(root, "java_type") instanceof JsonStringLiteral javaType) {
			return new BuiltinClassElement(javaType.getValue());
		}
		return null;
	}

	public NormalClassElement parseNormalClass(JsonObject root) {
		if (Util.findProperty(root, "name") instanceof JsonStringLiteral name) {
			String extends_ = Util.findProperty(root, "extends") instanceof JsonStringLiteral string ? string.getValue() : null;
			boolean abstract_ = Util.findProperty(root, "abstract") instanceof JsonBooleanLiteral bool ? bool.getValue() : false;
			return new NormalClassElement(name.getValue(), extends_ != null ? ID.parseBG(extends_) : null, abstract_);
		}
		return null;
	}

	public EnumClassElement parseEnumClass(JsonObject root) {
		if (Util.findProperty(root, "name") instanceof JsonStringLiteral name) {
			String extends_ = Util.findProperty(root, "extends") instanceof JsonStringLiteral string ? string.getValue() : null;
			boolean abstract_ = Util.findProperty(root, "abstract") instanceof JsonBooleanLiteral bool ? bool.getValue() : false;
			return new EnumClassElement(name.getValue(), extends_ != null ? ID.parseBG(extends_) : null, abstract_);
		}
		return null;
	}

	public NormalFieldElement parseNormalField(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "field_type") instanceof JsonStringLiteral fieldType
		) {
			return new NormalFieldElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(fieldType.getValue()));
		}
		return null;
	}

	public ConstantFieldElement parseConstantField(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "field_type") instanceof JsonStringLiteral fieldType
		) {
			return new ConstantFieldElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(fieldType.getValue()));
		}
		return null;
	}

	public EnumFieldElement parseEnumField(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "impl_type") instanceof JsonStringLiteral implType
		) {
			return new EnumFieldElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(implType.getValue()));
		}
		return null;
	}

	public ParameterElement parseParameter(JsonValue value) {
		if (
			value instanceof JsonObject root &&
			Util.findProperty(root, "name") instanceof JsonStringLiteral parameterName &&
			Util.findProperty(root, "type") instanceof JsonStringLiteral parameterType
		) {
			return new ParameterElement(parameterName.getValue(), ID.parseBG(parameterType.getValue()));
		}
		return null;
	}

	public ParameterElement[] parseParameters(JsonArray parameters) {
		List<JsonValue> parameterList = parameters.getValueList();
		int parameterCount = parameterList.size();
		ParameterElement[] parameterElements = new ParameterElement[parameterCount];
		for (int index = 0; index < parameterCount; index++) {
			if ((parameterElements[index] = this.parseParameter(parameterList.get(index))) == null) {
				return null;
			}
		}
		return parameterElements;
	}

	public StaticMethodElement parseStaticMethod(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "return_type") instanceof JsonStringLiteral returnType &&
			Util.findProperty(root, "parameters") instanceof JsonArray parameters
		) {
			return new StaticMethodElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(returnType.getValue()), this.parseParameters(parameters));
		}
		return null;
	}

	public NormalMethodElement parseNormalMethod(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "return_type") instanceof JsonStringLiteral returnType &&
			Util.findProperty(root, "parameters") instanceof JsonArray parameters
		) {
			return new NormalMethodElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(returnType.getValue()), this.parseParameters(parameters));
		}
		return null;
	}

	public AbstractMethodElement parseAbstractMethod(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "return_type") instanceof JsonStringLiteral returnType &&
			Util.findProperty(root, "parameters") instanceof JsonArray parameters
		) {
			return new AbstractMethodElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(returnType.getValue()), this.parseParameters(parameters));
		}
		return null;
	}

	public OverrideMethodElement parseOverrideMethod(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "override") instanceof JsonStringLiteral override
		) {
			return new OverrideMethodElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(override.getValue()));
		}
		return null;
	}

	public NormalPropertyElement parseNormalProperty(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "property_type") instanceof JsonStringLiteral propertyType
		) {
			return new NormalPropertyElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(propertyType.getValue()), Util.findProperty(root, "set") != null);
		}
		return null;
	}

	public AbstractPropertyElement parseAbstractProperty(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "property_type") instanceof JsonStringLiteral propertyType &&
			Util.findProperty(root, "settable") instanceof JsonBooleanLiteral settable
		) {
			return new AbstractPropertyElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(propertyType.getValue()), settable.getValue());
		}
		return null;
	}

	public OverridePropertyElement parseOverridePRoperty(JsonObject root) {
		if (
			Util.findProperty(root, "name") instanceof JsonStringLiteral name &&
			Util.findProperty(root, "owner") instanceof JsonStringLiteral owner &&
			Util.findProperty(root, "override") instanceof JsonStringLiteral override
		) {
			return new OverridePropertyElement(name.getValue(), ID.parseBG(owner.getValue()), ID.parseBG(override.getValue()));
		}
		return null;
	}

	public static abstract class CustomElement extends DynamicRegistryElement {

		public final String name;

		public CustomElement(String name) {
			this.name = name;
		}

		public abstract void applyTo(EnvironmentModel environment);
	}

	public static class SpecialMarker extends CustomElement {

		public static final SpecialMarker ABSENT = new SpecialMarker();

		public SpecialMarker() {
			super(null);
		}

		@Override
		public void applyTo(EnvironmentModel environment) {
			throw new UnsupportedOperationException();
		}
	}

	public static class CyclicException extends Exception {

		public static final CyclicException INSTANCE = new CyclicException();

		public CyclicException() {
			super(null, null, false, false);
		}
	}

	public static abstract class TypeElement extends CustomElement {

		public TypeElement(String name) {
			super(name);
		}

		public abstract RawTypeModel resolve(Set<ID> seen) throws CyclicException;

		public void applyTo(EnvironmentModel environment, RawTypeModel type) {
			environment.addType(new TypeData(this.name, Colors.TYPE, new TokenInfo(type)));
		}

		@Override
		public void applyTo(EnvironmentModel environment) {
			RawTypeModel type = null;
			try {
				type = this.resolve(new HashSet<>());
			}
			catch (CyclicException ignored) {}
			if (type != null) {
				this.applyTo(environment, type);
			}
		}
	}

	public class BuiltinClassElement extends TypeElement {

		public BuiltinClassElement(String name) {
			super(name);
		}

		@Override
		public RawTypeModel resolve(Set<ID> seen) {
			return CustomClassEnvironment.this.packData.projectData.environment().types.get(this.name);
		}

		@Override
		public void applyTo(EnvironmentModel environment) {
			//no-op.
		}
	}

	public abstract class UserClassElement extends TypeElement {

		public final ID extends_;
		public final boolean abstract_;
		public RawTypeModel resolution;

		public UserClassElement(String name, ID extends_, boolean abstract_) {
			super(name);
			this.extends_ = extends_;
			this.abstract_ = abstract_;
		}

		public UserClassElement extends_() {
			return this.extends_ != null && CustomClassEnvironment.this.get(this.extends_) instanceof UserClassElement element ? element : null;
		}

		@Override
		public RawTypeModel resolve(Set<ID> seen) throws CyclicException {
			RawTypeModel result = this.resolution;
			if (result == null) {
				if (this.extends_ != null) {
					UserClassElement element = this.extends_();
					if (element != null) {
						if (!seen.add(this.extends_)) {
							throw CyclicException.INSTANCE;
						}
						try {
							result = element.resolve(seen);
						}
						finally {
							seen.remove(this.extends_);
						}
						if (result != null) {
							result = new RawTypeModel(this.abstract_ ? TypeModifiersModel.ABSTRACT : 0, this.name, result, RawTypeModel.EMPTY_ARRAY, Collections.emptySet());
						}
					}
				}
				else {
					result = new RawTypeModel(this.abstract_ ? TypeModifiersModel.ABSTRACT : 0, this.name, CustomClassEnvironment.this.packData.projectData.environment().standardTypes.object, RawTypeModel.EMPTY_ARRAY, Collections.emptySet());
				}
				this.resolution = result == null ? RawTypeModel.ERROR : result;
			}
			else if (result == RawTypeModel.ERROR) {
				return null;
			}
			return result;
		}

		@Override
		public void clearCaches() {
			super.clearCaches();
			this.resolution = null;
		}
	}

	public class NormalClassElement extends UserClassElement {

		public NormalClassElement(String name, ID extends_, boolean abstract_) {
			super(name, extends_, abstract_);
		}

		@Override
		public UserClassElement extends_() {
			return super.extends_() instanceof NormalClassElement element ? element : null;
		}

		@Override
		public void applyTo(EnvironmentModel environment, RawTypeModel type) {
			super.applyTo(environment, type);
			environment.addStaticMethod(new MethodData("new", Colors.KEYWORD, type, new TokenInfo(type)));
		}
	}

	public class EnumClassElement extends UserClassElement {

		public EnumClassElement(String name, ID extends_, boolean abstract_) {
			super(name, extends_, abstract_);
		}

		@Override
		public UserClassElement extends_() {
			return super.extends_() instanceof EnumClassElement element ? element : null;
		}
	}

	public abstract class MemberElement extends CustomElement {

		public final ID owner;

		public MemberElement(String name, ID owner) {
			super(name);
			this.owner = owner;
		}

		public UserClassElement owner() {
			return CustomClassEnvironment.this.get(this.owner) instanceof UserClassElement element ? element : null;
		}
	}

	public abstract class FieldElement extends MemberElement {

		public final ID fieldType;

		public FieldElement(String name, ID owner, ID fieldType) {
			super(name, owner);
			this.fieldType = fieldType;
		}

		public TypeElement fieldType() {
			return CustomClassEnvironment.this.get(this.fieldType) instanceof TypeElement element ? element : null;
		}

		public abstract boolean assignable();

		public abstract boolean isStatic();

		@Override
		public void applyTo(EnvironmentModel environment) {
			UserClassElement ownerElement = this.owner();
			TypeElement fieldTypeElement = this.fieldType();
			if (ownerElement != null && fieldTypeElement != null) {
				RawTypeModel ownerType = null, fieldType = null;
				try {
					HashSet<ID> cyclicChecker = new HashSet<>();
					ownerType = ownerElement.resolve(cyclicChecker);
					assert cyclicChecker.isEmpty();
					fieldType = fieldTypeElement.resolve(cyclicChecker);
					assert cyclicChecker.isEmpty();
				}
				catch (CyclicException ignored) {}
				if (ownerType != null && fieldType != null) {
					if (this.isStatic()) {
						environment.addStaticField(new FieldData(ownerType, this.name, Colors.STATIC_FIELD, new TokenInfo(fieldType, this.assignable() ? TokenInfo.FLAG_ASSIGNABLE : 0)));
					}
					else {
						environment.addInstanceField(new FieldData(ownerType, this.name, Colors.INSTANCE_FIELD, new TokenInfo(fieldType, this.assignable() ? TokenInfo.FLAG_ASSIGNABLE : 0)));
					}
				}
			}
		}
	}

	public abstract class InstanceFieldElement extends FieldElement {

		public InstanceFieldElement(String name, ID owner, ID fieldType) {
			super(name, owner, fieldType);
		}

		@Override
		public boolean isStatic() {
			return false;
		}
	}

	public class NormalFieldElement extends InstanceFieldElement {

		public NormalFieldElement(String name, ID owner, ID fieldType) {
			super(name, owner, fieldType);
		}

		@Override
		public boolean assignable() {
			return true;
		}
	}

	public class ConstantFieldElement extends InstanceFieldElement {

		public ConstantFieldElement(String name, ID owner, ID fieldType) {
			super(name, owner, fieldType);
		}

		@Override
		public boolean assignable() {
			return false;
		}
	}

	public class EnumFieldElement extends FieldElement {

		public EnumFieldElement(String name, ID owner, ID implType) {
			super(name, owner, implType);
		}

		@Override
		public UserClassElement owner() {
			return super.owner() instanceof EnumClassElement element ? element : null;
		}

		@Override
		public TypeElement fieldType() {
			return super.fieldType() instanceof EnumClassElement element ? element : null;
		}

		@Override
		public boolean assignable() {
			return false;
		}

		@Override
		public boolean isStatic() {
			return true;
		}
	}

	public class ParameterElement {

		public final String name;
		public final ID type;

		public ParameterElement(String name, ID type) {
			this.name = name;
			this.type = type;
		}

		public TypeElement type() {
			return CustomClassEnvironment.this.get(this.type) instanceof TypeElement element ? element : null;
		}
	}

	public abstract class MethodElement extends MemberElement {

		public static final ParameterModel[] INVALID_PARAMETERS = {};

		public ParameterModel[] parameterResolution;

		public MethodElement(String name, ID owner) {
			super(name, owner);
		}

		@Override
		public void clearCaches() {
			super.clearCaches();
			this.parameterResolution = null;
		}

		public ParameterModel[] resolveParameters(Set<ID> seen) {
			ParameterModel[] parameterTypes = this.parameterResolution;
			if (parameterTypes == null) {
				try {
					ParameterElement[] parameters = this.parameters(seen);
					assert seen.isEmpty();
					if (parameters != null) {
					parameterTypes = new ParameterModel[parameters.length];
						for (int index = 0, length = parameters.length; index < length; index++) {
							ParameterElement parameter = parameters[index];
							RawTypeModel resolution = parameter.type().resolve(seen);
							assert seen.isEmpty();
							if (resolution == null) throw CyclicException.INSTANCE;
							parameterTypes[index] = new ParameterModel(parameter.name, resolution, false);
						}
					}
				}
				catch (CyclicException ignored) {
					parameterTypes = null;
				}
				this.parameterResolution = parameterTypes == null ? INVALID_PARAMETERS : parameterTypes;
			}
			else if (parameterTypes == INVALID_PARAMETERS) {
				parameterTypes = null;
			}
			return parameterTypes;
		}

		public abstract TypeElement returnType(Set<ID> seen) throws CyclicException;

		public abstract ParameterElement[] parameters(Set<ID> seen) throws CyclicException;

		public abstract boolean isStatic();

		@Override
		public void applyTo(EnvironmentModel environment) {
			try {
				UserClassElement owner = this.owner();
				if (owner != null) {
					Set<ID> seen = new HashSet<>();
					TypeElement return_ = this.returnType(seen);
					assert seen.isEmpty();
					ParameterElement[] parameters = this.parameters(seen);
					assert seen.isEmpty();
					if (return_ != null && parameters != null) {
						RawTypeModel ownerType = this.owner().resolve(seen);
						assert seen.isEmpty();
						RawTypeModel returnType = return_.resolve(seen);
						assert seen.isEmpty();
						ParameterModel[] parameterTypes = this.resolveParameters(seen);
						assert seen.isEmpty();
						if (ownerType != null && returnType != null && parameterTypes != null) {
							if (this.isStatic()) {
								environment.addStaticMethod(new MethodData(this.name, Colors.STATIC_METHOD, ownerType, new TokenInfo(returnType), parameterTypes));
							}
							else {
								environment.addInstanceMethod(new MethodData(this.name, Colors.INSTANCE_METHOD, ownerType, new TokenInfo(returnType), parameterTypes));
							}
						}
					}
				}
			}
			catch (CyclicException ignored) {}
		}
	}

	public class StaticMethodElement extends MethodElement {

		public final ID returnType;
		public final ParameterElement[] parameters;

		public StaticMethodElement(String name, ID owner, ID returnType, ParameterElement... parameters) {
			super(name, owner);
			this.returnType = returnType;
			this.parameters = parameters;
		}

		@Override
		public TypeElement returnType(Set<ID> seen) throws CyclicException {
			return CustomClassEnvironment.this.get(this.returnType) instanceof TypeElement element ? element : null;
		}

		@Override
		public ParameterElement[] parameters(Set<ID> seen) throws CyclicException {
			return this.parameters;
		}

		@Override
		public boolean isStatic() {
			return true;
		}
	}

	public abstract class InstanceMethodElement extends MethodElement {

		public InstanceMethodElement(String name, ID owner) {
			super(name, owner);
		}

		@Override
		public boolean isStatic() {
			return false;
		}
	}

	public class NormalMethodElement extends InstanceMethodElement {

		public final ID returnType;
		public final ParameterElement[] parameters;

		public NormalMethodElement(String name, ID owner, ID returnType, ParameterElement... parameters) {
			super(name, owner);
			this.returnType = returnType;
			this.parameters = parameters;
		}

		@Override
		public TypeElement returnType(Set<ID> seen) {
			return CustomClassEnvironment.this.get(this.returnType) instanceof TypeElement element ? element : null;
		}

		@Override
		public ParameterElement[] parameters(Set<ID> seen) {
			return this.parameters;
		}
	}

	public class AbstractMethodElement extends InstanceMethodElement {

		public final ID returnType;
		public final ParameterElement[] parameters;

		public AbstractMethodElement(String name, ID owner, ID returnType, ParameterElement... parameters) {
			super(name, owner);
			this.returnType = returnType;
			this.parameters = parameters;
		}

		@Override
		public TypeElement returnType(Set<ID> seen) {
			return CustomClassEnvironment.this.get(this.returnType) instanceof TypeElement element ? element : null;
		}

		@Override
		public ParameterElement[] parameters(Set<ID> seen) {
			return this.parameters;
		}
	}

	public class OverrideMethodElement extends InstanceMethodElement {

		public final ID override;

		public OverrideMethodElement(String name, ID owner, ID override) {
			super(name, owner);
			this.override = override;
		}

		@Override
		public TypeElement returnType(Set<ID> seen) throws CyclicException {
			if (CustomClassEnvironment.this.get(this.override) instanceof InstanceMethodElement element) {
				if (!seen.add(this.override)) throw CyclicException.INSTANCE;
				try {
					return element.returnType(seen);
				}
				finally {
					seen.remove(this.override);
				}
			}
			return null;
		}

		@Override
		public ParameterElement[] parameters(Set<ID> seen) throws CyclicException {
			if (CustomClassEnvironment.this.get(this.override) instanceof InstanceMethodElement element) {
				if (!seen.add(this.override)) throw CyclicException.INSTANCE;
				try {
					return element.parameters(seen);
				}
				finally {
					seen.remove(this.override);
				}
			}
			return null;
		}
	}

	public abstract class PropertyElement extends MemberElement {

		public PropertyElement(String name, ID owner) {
			super(name, owner);
		}

		public abstract TypeElement propertyType(Set<ID> seen) throws CyclicException;

		public abstract boolean settable(Set<ID> seen) throws CyclicException;

		@Override
		public void applyTo(EnvironmentModel environment) {
			try {
				UserClassElement ownerElement = this.owner();
				if (ownerElement != null) {
					HashSet<ID> cyclicChecker = new HashSet<>();
					TypeElement propertyTypeElement = this.propertyType(cyclicChecker);
					if (propertyTypeElement != null) {
						RawTypeModel ownerType = ownerElement.resolve(cyclicChecker);
						assert cyclicChecker.isEmpty();
						if (ownerType != null) {
							RawTypeModel propertyType = propertyTypeElement.resolve(cyclicChecker);
							assert cyclicChecker.isEmpty();
							if (propertyType != null) {
								environment.addInstanceField(new FieldData(ownerType, this.name, Colors.INSTANCE_FIELD, new TokenInfo(propertyType, this.settable(cyclicChecker) ? TokenInfo.FLAG_ASSIGNABLE : 0)));
							}
						}
					}
				}
			}
			catch (CyclicException ignored) {}
		}
	}

	public class NormalPropertyElement extends PropertyElement {

		public final ID propertyType;
		public final boolean settable;

		public NormalPropertyElement(String name, ID owner, ID propertyType, boolean settable) {
			super(name, owner);
			this.propertyType = propertyType;
			this.settable = settable;
		}

		@Override
		public TypeElement propertyType(Set<ID> seen) throws CyclicException {
			return CustomClassEnvironment.this.get(this.propertyType) instanceof TypeElement element ? element : null;
		}

		@Override
		public boolean settable(Set<ID> seen) throws CyclicException {
			return this.settable;
		}
	}

	public class AbstractPropertyElement extends PropertyElement {

		public final ID propertyType;
		public final boolean settable;

		public AbstractPropertyElement(String name, ID owner, ID propertyType, boolean settable) {
			super(name, owner);
			this.propertyType = propertyType;
			this.settable = settable;
		}

		@Override
		public TypeElement propertyType(Set<ID> seen) throws CyclicException {
			return CustomClassEnvironment.this.get(this.propertyType) instanceof TypeElement element ? element : null;
		}

		@Override
		public boolean settable(Set<ID> seen) throws CyclicException {
			return this.settable;
		}
	}

	public class OverridePropertyElement extends PropertyElement {

		public final ID override;

		public OverridePropertyElement(String name, ID owner, ID override) {
			super(name, owner);
			this.override = override;
		}

		@Override
		public TypeElement propertyType(Set<ID> seen) throws CyclicException {
			if (CustomClassEnvironment.this.get(this.override) instanceof PropertyElement element) {
				if (!seen.add(this.override)) throw CyclicException.INSTANCE;
				try {
					return element.propertyType(seen);
				}
				finally {
					seen.remove(this.override);
				}
			}
			return null;
		}

		@Override
		public boolean settable(Set<ID> seen) throws CyclicException {
			if (CustomClassEnvironment.this.get(this.override) instanceof PropertyElement element) {
				if (!seen.add(this.override)) throw CyclicException.INSTANCE;
				try {
					return element.settable(seen);
				}
				finally {
					seen.remove(this.override);
				}
			}
			return false;
		}
	}
}