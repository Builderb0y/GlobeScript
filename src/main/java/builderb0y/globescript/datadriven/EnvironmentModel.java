package builderb0y.globescript.datadriven;

import java.util.*;
import java.util.stream.Collectors;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.vfs.VirtualFile;

import builderb0y.globescript.*;

public class EnvironmentModel extends EnvironmentConfigurator {

	public final StackMap<         TypeData.Key,          TypeData > types           = new StackMap<>();
	public final StackMap<     VariableData.Key,      VariableData > variables       = new StackMap<>();
	public final StackMap<        FieldData.Key,         FieldData > staticFields    = new StackMap<>();
	public final StackMap<        FieldData.Key,         FieldData > instanceFields  = new StackMap<>();
	public final StackMap<      KeywordData.Key,       KeywordData > keywords        = new StackMap<>();
	public final StackMap<MemberKeywordData.Key, MemberKeywordData > memberKeywords  = new StackMap<>();
	public final StackMap<     FunctionData.Key, List<FunctionData>> functions       = StackMap.withFallback(Collections.emptyList());
	public final StackMap<       MethodData.Key, List<  MethodData>> staticMethods   = StackMap.withFallback(Collections.emptyList());
	public final StackMap<       MethodData.Key, List<  MethodData>> instanceMethods = StackMap.withFallback(Collections.emptyList());
	public final StackMap<         CastData.Key, List<    CastData>> casters         = StackMap.withFallback(Collections.emptyList());
	public final      Map<     VariableData.Key,      VariableData > importedValues  = new HashMap<>();

	public EnvironmentModel(String name) {
		super(name);
	}

	public EnvironmentModel(VirtualFile source, EnvironmentConfigurator from) {
		super(from.name);
		from.configure(source, this);
	}

	public EnvironmentModel(VirtualFile source, EnvironmentConfigurator... from) {
		super(
			Arrays
			.stream(from)
			.map((EnvironmentConfigurator model) -> model.name)
			.collect(Collectors.joining(", ", "[ ", " ]"))
		);
		for (EnvironmentConfigurator configurator : from) {
			configurator.configure(source, this);
		}
	}

	@Override
	public void configure(VirtualFile source, EnvironmentModel environment) {
		environment.addAll(this);
	}

	public void addAll(EnvironmentModel model) {
		this.types.putAll(model.types);
		this.variables.putAll(model.variables);
		this.staticFields.putAll(model.staticFields);
		this.instanceFields.putAll(model.instanceFields);
		this.keywords.putAll(model.keywords);
		this.memberKeywords.putAll(model.memberKeywords);
		this.functions.putAll(model.functions);
		this.staticMethods.putAll(model.staticMethods);
		this.instanceMethods.putAll(model.instanceMethods);
		this.casters.putAll(model.casters);
	}

	public void addType(TypeData type) {
		this.types.put(type.key(), type);
	}

	public void addVariable(VariableData variable) {
		this.variables.put(variable.key(), variable);
	}

	public void addStaticField(FieldData field) {
		this.staticFields.put(field.key(), field);
	}

	public void addInstanceField(FieldData field) {
		this.instanceFields.put(field.key(), field);
	}

	public void addKeyword(KeywordData keyword) {
		this.keywords.put(keyword.key(), keyword);
	}

	public void addMemberKeyword(MemberKeywordData memberKeyword) {
		this.memberKeywords.put(memberKeyword.key(), memberKeyword);
	}

	public void addFunction(FunctionData function) {
		this.functions.computeIfAbsent(function.key(), (FunctionData.Key $) -> new ArrayList<>()).add(function);
	}

	public void addStaticMethod(MethodData method) {
		this.staticMethods.computeIfAbsent(method.key(), (MethodData.Key $) -> new ArrayList<>()).add(method);
	}

	public void addInstanceMethod(MethodData method) {
		this.instanceMethods.computeIfAbsent(method.key(), (MethodData.Key $) -> new ArrayList<>()).add(method);
	}

	public void addCaster(CastData cast) {
		for (RawTypeModel to : cast.to.getAssignableTypes()) {
			if (!cast.from.isAssignableTo(to)) {
				this.casters.computeIfAbsent(new CastData.Key(cast.from, to), (CastData.Key $) -> new ArrayList<>()).add(cast);
			}
		}
	}

	public void addImportedValue(VariableData value) {
		this.addVariable(value);
		this.importedValues.put(value.key(), value);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static abstract class IdentifierData {

		public final String name;
		public final TextAttributesKey color;

		public IdentifierData(String name, TextAttributesKey color) {
			this.name = name;
			this.color = color;
		}

		public Token applyColor(Token identifier) {
			return identifier.withColor(this.color);
		}

		public abstract Object key();

		@Override
		public abstract String toString();
	}

	public static class TypeData extends IdentifierData {

		public final TokenInfo info;

		public TypeData(String name, TextAttributesKey color, TokenInfo info) {
			super(name, color);
			this.info = info;
		}

		@Override
		public String toString() {
			return this.name;
		}

		@Override
		public Key key() {
			return new Key(this.name);
		}

		public static record Key(String name) {}
	}

	public static class VariableData extends IdentifierData {

		public final TokenInfo info;

		public VariableData(String name, TextAttributesKey color, TokenInfo info) {
			super(name, color);
			this.info = info;
		}

		@Override
		public String toString() {
			return this.name;
		}

		@Override
		public Key key() {
			return new Key(this.name);
		}

		public static record Key(String name) {}
	}

	public static class FieldData extends IdentifierData {

		public final RawTypeModel receiverType;
		public final TokenInfo info;

		public FieldData(RawTypeModel receiverType, String name, TextAttributesKey color, TokenInfo info) {
			super(name, color);
			this.receiverType = receiverType;
			this.info = info;
		}

		@Override
		public String toString() {
			return this.receiverType.name + '.' + this.name;
		}

		@Override
		public Key key() {
			return new Key(this.receiverType, this.name);
		}

		public static record Key(RawTypeModel receiverType, String name) {}
	}

	public static abstract class KeywordData extends IdentifierData {

		public KeywordData(String name, TextAttributesKey color) {
			super(name, color);
		}

		public abstract Token handle(ExpressionParser parser, Token word);

		@Override
		public Key key() {
			return new Key(this.name);
		}

		public static record Key(String name) {}
	}

	public static abstract class MemberKeywordData extends IdentifierData {

		public final RawTypeModel receiverType;

		public MemberKeywordData(String name, TextAttributesKey color, RawTypeModel type) {
			super(name, color);
			this.receiverType = type;
		}

		public abstract Token handle(ExpressionParser parser, Token receiver, Token dot, Token word);

		@Override
		public Key key() {
			return new Key(this.receiverType, this.name);
		}

		public static record Key(RawTypeModel receiverType, String name) {}
	}

	public static class FunctionData extends IdentifierData {

		public final TokenInfo info;
		public final ParameterModel[] parameters;

		public FunctionData(String name, TextAttributesKey color, TokenInfo info, ParameterModel... types) {
			super(name, color);
			this.info = info;
			this.parameters = types;
		}

		public CallableEligibility getEligibility(ScriptEnvironment environment, List<Token> arguments) {
			return CallableEligibility.compute(environment, this.parameters, arguments);
		}

		@Override
		public String toString() {
			return Arrays.stream(this.parameters).map(ParameterModel::toString).collect(Collectors.joining(", ", this.name + "(", ")"));
		}

		@Override
		public Key key() {
			return new Key(this.name);
		}

		public static record Key(String name) {}
	}

	public static class MethodData extends IdentifierData {

		public final RawTypeModel receiverType;
		public final TokenInfo returnType;
		public final ParameterModel[] parameters;

		public MethodData(String name, TextAttributesKey color, RawTypeModel receiverType, TokenInfo returnType, ParameterModel... parameters) {
			super(name, color);
			this.receiverType = receiverType;
			this.returnType = returnType;
			this.parameters = parameters;
		}

		public CallableEligibility getEligibility(ScriptEnvironment environment, List<Token> arguments) {
			return CallableEligibility.compute(environment, this.parameters, arguments);
		}

		@Override
		public String toString() {
			return Arrays.stream(this.parameters).map(ParameterModel::toString).collect(Collectors.joining(", ", this.returnType + " " + this.receiverType.name + '.' + this.name + '(', ")"));
		}

		@Override
		public Key key() {
			return new Key(this.receiverType, this.name);
		}

		public static record Key(RawTypeModel receiverType, String name) {}
	}

	public static class ParameterModel {

		public static final ParameterModel[] EMPTY_ARRAY = {};

		public final String name;
		public final RawTypeModel type;
		public final boolean repeatable;

		public ParameterModel(String name, RawTypeModel type, boolean repeatable) {
			this.name = name;
			this.type = type;
			this.repeatable = repeatable;
		}

		@Override
		public String toString() {
			return this.type.name + (this.repeatable ? "... " : " ") + this.name;
		}
	}

	public static enum CallableEligibility {
		INVALID,
		REQUIRES_IMPLICIT_CAST,
		EXACT_MATCH;

		public static CallableEligibility compute(ScriptEnvironment environment, ParameterModel[] expectedParameters, List<Token> actualParameters) {
			int limit = expectedParameters.length - 1;
			boolean varargs = limit >= 0 && expectedParameters[limit].repeatable;
			if (actualParameters.size() < (varargs ? limit : limit + 1)) {
				return INVALID;
			}
			boolean requiresCast = false;
			for (int index = 0, size = actualParameters.size(); index < size; index++) {
				Token argument = actualParameters.get(index);
				if (index <= limit || varargs) {
					ParameterModel parameter = expectedParameters[Math.min(index, limit)];
					CallableEligibility eligibility = argument.info.getEligibility(environment, parameter.type, Plicity.IMPLICIT);
					switch (eligibility) {
						case INVALID -> { return INVALID; }
						case REQUIRES_IMPLICIT_CAST -> requiresCast = true;
						case EXACT_MATCH -> {}
					}
				}
				else {
					return INVALID;
				}
			}
			return requiresCast ? REQUIRES_IMPLICIT_CAST : EXACT_MATCH;
		}
	}

	public static class CastData {

		public final RawTypeModel from, to;
		public final Plicity plicity;

		public CastData(RawTypeModel from, RawTypeModel to, Plicity plicity) {
			this.from = from;
			this.to = to;
			this.plicity = plicity;
		}

		public Key key() {
			return new Key(this.from, this.to);
		}

		public static record Key(RawTypeModel from, RawTypeModel to) {}
	}
}