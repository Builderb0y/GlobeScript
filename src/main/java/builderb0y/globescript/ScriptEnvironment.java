package builderb0y.globescript;

import java.util.ArrayList;
import java.util.List;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.EnvironmentConfigurator;
import builderb0y.globescript.datadriven.EnvironmentModel;
import builderb0y.globescript.datadriven.GsEnv.StandardTypes;
import builderb0y.globescript.datadriven.RawTypeModel;

public class ScriptEnvironment extends EnvironmentModel {

	public final StandardTypes standardTypes;
	public final List<LoopLabel> loopLabels = new ArrayList<>();
	public int loopLabelCount = 0;

	public ScriptEnvironment(StandardTypes standardTypes, PsiElement source, EnvironmentConfigurator from) {
		super(source, from);
		this.standardTypes = standardTypes;
	}

	public ScriptEnvironment(StandardTypes standardTypes, PsiElement source, EnvironmentConfigurator... from) {
		super(source, from);
		this.standardTypes = standardTypes;
	}

	public void addUserLocal(String name, TokenInfo type) {
		this.addVariable(new VariableData(name, Colors.LOCAL, type));
	}

	public void addUserParameter(String name, TokenInfo type) {
		this.addVariable(new VariableData(name, Colors.PARAMETER, type));
	}

	public void addUserInstanceField(RawTypeModel owner, String name, TokenInfo type) {
		this.addInstanceField(new FieldData(owner, name, Colors.INSTANCE_FIELD, type));
	}

	public void addUserStaticField(RawTypeModel owner, String name, TokenInfo type) {
		this.addStaticField(new FieldData(owner, name, Colors.STATIC_FIELD, type));
	}

	public void addUserType(String name, TokenInfo type) {
		this.addType(new TypeData(name, Colors.TYPE, type));
	}

	public void addUserFunction(String name, TokenInfo returnType, ParameterModel... parameters) {
		this.addFunction(new FunctionData(name, Colors.FUNCTION, returnType, parameters));
	}

	public void addUserInstanceMethod(RawTypeModel owner, String name, TokenInfo returnType, ParameterModel... parameters) {
		this.addInstanceMethod(new MethodData(name, Colors.INSTANCE_METHOD, owner, returnType, parameters));
	}

	public void addUserStaticMethod(RawTypeModel owner, String name, TokenInfo returnType, ParameterModel... parameters) {
		this.addStaticMethod(new MethodData(name, Colors.STATIC_METHOD, owner, returnType, parameters));
	}

	public void addUserConstructor(RawTypeModel owner, String name, TokenInfo returnType, ParameterModel... parameters) {
		this.addStaticMethod(new MethodData(name, Colors.KEYWORD, owner, returnType, parameters));
	}

	public void addLoopLabel(@Nullable String name) {
		this.loopLabels.add(new LoopLabel(name, this.loopLabelCount));
	}

	public void push() {
		this.types.push();
		this.variables.push();
		this.staticFields.push();
		this.instanceFields.push();
		this.keywords.push();
		this.memberKeywords.push();
		this.functions.push();
		this.staticMethods.push();
		this.instanceMethods.push();
		this.casters.push();
		this.loopLabelCount++;
	}

	public void pop() {
		this.types.pop();
		this.variables.pop();
		this.staticFields.pop();
		this.instanceFields.pop();
		this.keywords.pop();
		this.memberKeywords.pop();
		this.functions.pop();
		this.staticMethods.pop();
		this.instanceMethods.pop();
		this.casters.pop();
		while (!this.loopLabels.isEmpty() && this.loopLabels.getLast().frame >= this.loopLabelCount) {
			this.loopLabels.removeLast();
		}
		this.loopLabelCount--;
	}

	public @Nullable TypeData getType(String name) {
		return this.types.get(new TypeData.Key(name));
	}

	public @Nullable VariableData getVariable(String name) {
		VariableData variable = this.variables.get(new VariableData.Key(name));
		if (variable != null) {
			return variable;
		}
		else {
			FieldData field = null;
			for (VariableData value : this.importedValues.values()) {
				FieldData field2 = this.getInstanceField(value.info.type(), name);
				if (field2 != null) {
					if (field == null) {
						field = field2;
					}
					else {
						FieldData field_ = field;
						return new VariableData(field.name, Colors.ERROR, TokenInfo.ERROR) {

							@Override
							public Token applyColor(Token identifier) {
								return super.applyColor(identifier).withTooltip("Ambiguous field on imported value could refer to " + field_ + " or " + field2 + ". Specify one by qualifying it.");
							}
						};
					}
				}
			}
			return field != null ? new VariableData(field.name, field.color, field.info) : null;
		}
	}

	public @Nullable FieldData getInstanceField(RawTypeModel receiver, String name) {
		for (RawTypeModel type : receiver.getAssignableTypes()) {
			FieldData data = this.instanceFields.get(new FieldData.Key(type, name));
			if (data != null) return data;
			data = this.instanceFields.get(new FieldData.Key(type, null));
			if (data != null) return data;
		}
		return null;
	}

	public @Nullable FieldData getStaticField(RawTypeModel receiver, String name) {
		FieldData data = this.staticFields.get(new FieldData.Key(receiver, name));
		if (data != null) return data;
		return this.staticFields.get(new FieldData.Key(receiver, null));
	}

	public @Nullable KeywordData getKeyword(String name) {
		return this.keywords.get(new KeywordData.Key(name));
	}

	public @Nullable MemberKeywordData getMemberKeyword(RawTypeModel receiver, String name) {
		for (RawTypeModel type : receiver.getAssignableTypes()) {
			MemberKeywordData data = this.memberKeywords.get(new MemberKeywordData.Key(type, name));
			if (data != null) return data;
		}
		return null;
	}

	public @Nullable FunctionData getFunction(String name, List<Token> parameters) {
		List<FunctionData> functions = this.functions.get(new FunctionData.Key(name));
		if (!functions.isEmpty()) {
			List<FunctionData> best = new ArrayList<>(4);
			CallableEligibility bestEligibility = CallableEligibility.INVALID;
			for (FunctionData function : functions) {
				CallableEligibility eligibility = function.getEligibility(this, parameters);
				if (eligibility.ordinal() > bestEligibility.ordinal()) {
					best.clear();
					bestEligibility = eligibility;
				}
				if (eligibility == bestEligibility) {
					best.add(function);
				}
			}
			switch (best.size()) {
				case 0 -> {}
				case 1 -> { return best.get(0); }
				default -> { return new AmbiguousFunctionData(name, parameters, best); }
			}
		}
		MethodData method = null;
		for (VariableData value : this.importedValues.values()) {
			MethodData method2 = this.getInstanceMethod(value.info.type(), name, parameters);
			if (method2 != null) {
				if (method == null) {
					method = method2;
				}
				else {
					MethodData method_ = method;
					return new FunctionData(method_.name, Colors.ERROR, TokenInfo.ERROR) {

						@Override
						public Token applyColor(Token identifier) {
							return super.applyColor(identifier).withTooltip("Ambiguous method on imported value could refer to " + method_ + " or " + method2 + ". Specify one by qualifying it.");
						}
					};
				}
			}
		}
		return method != null ? new FunctionData(method.name, method.color, method.returnType, method.parameters) : null;
	}

	public static class AmbiguousFunctionData extends FunctionData {

		public final List<FunctionData> candidates;

		public AmbiguousFunctionData(String name, List<Token> parameters, List<FunctionData> candidates) {
			super(
				name,
				Colors.ERROR,
				new TokenInfo(RawTypeModel.ERROR),
				parameters
				.stream()
				.map((Token token) -> new ParameterModel(
					"_",
					token.info.type(),
					false
				))
				.toArray(ParameterModel[]::new)
			);
			this.candidates = candidates;
		}

		@Override
		public Token applyColor(Token identifier) {
			super.applyColor(identifier).withTooltip("Ambiguous function call could refer to:");
			for (FunctionData function : this.candidates) {
				identifier.withTooltip(function.toString());
			}
			return identifier.withTooltip("Actual form: " + this);
		}
	}

	public @Nullable MethodData getInstanceMethod(RawTypeModel receiver, String name, List<Token> arguments) {
		List<MethodData> candidates = new ArrayList<>(4);
		String[] names = { name, null };
		for (RawTypeModel receiverType : receiver.getAssignableTypes()) {
			for (String actualName : names) {
				List<MethodData> methods = this.instanceMethods.get(new MethodData.Key(receiverType, actualName));
				if (!methods.isEmpty()) {
					CallableEligibility bestEligibility = CallableEligibility.INVALID;
					for (MethodData method : methods) {
						CallableEligibility eligibility = method.getEligibility(this, arguments);
						if (eligibility.ordinal() > bestEligibility.ordinal()) {
							candidates.clear();
							bestEligibility = eligibility;
						}
						if (eligibility == bestEligibility) {
							candidates.add(method);
						}
					}
					switch (candidates.size()) {
						case 0 -> candidates.clear();
						case 1 -> { return candidates.get(0); }
						default -> { return new AmbiguousMethodData(actualName, receiverType, arguments, candidates); }
					}
				}
			}
		}
		return null;
	}

	public static class AmbiguousMethodData extends MethodData {

		public final List<MethodData> candidates;

		public AmbiguousMethodData(
			String name,
			RawTypeModel receiverType,
			List<Token> arguments,
			List<MethodData> candidates
		) {
			super(
				name,
				Colors.ERROR,
				receiverType,
				new TokenInfo(RawTypeModel.ERROR),
				arguments
				.stream()
				.map((Token token) -> new ParameterModel(
					"_",
					token.info.type(),
					false
				))
				.toArray(ParameterModel[]::new)
			);
			this.candidates = candidates;
		}

		@Override
		public Token applyColor(Token identifier) {
			super.applyColor(identifier).withTooltip("Ambiguous method call could refer to:");
			for (MethodData method : this.candidates) {
				identifier.withTooltip(method.toString());
			}
			return identifier.withTooltip("Actual form: " + this);
		}
	}

	public @Nullable MethodData getStaticMethod(RawTypeModel receiverType, String name, List<Token> arguments) {
		List<MethodData> candidates = new ArrayList<>(4);
		String[] names = { name, null };
		for (String actualName : names) {
			List<MethodData> methods = this.staticMethods.get(new MethodData.Key(receiverType, actualName));
			if (!methods.isEmpty()) {
				CallableEligibility bestEligibility = CallableEligibility.INVALID;
				for (MethodData method : methods) {
					CallableEligibility eligibility = method.getEligibility(this, arguments);
					if (eligibility.ordinal() > bestEligibility.ordinal()) {
						candidates.clear();
						bestEligibility = eligibility;
					}
					if (eligibility == bestEligibility) {
						candidates.add(method);
					}
				}
				switch (candidates.size()) {
					case 0 -> candidates.clear();
					case 1 -> { return candidates.get(0); }
					default -> { return new AmbiguousMethodData(actualName, receiverType, arguments, candidates); }
				}
			}
		}
		return null;
	}

	public List<CastData> getCasts(RawTypeModel from, RawTypeModel to) {
		return this.casters.get(new CastData.Key(from, to));
	}

	public boolean hasLoopLabel(@Nullable String name) {
		if (name == null) return !this.loopLabels.isEmpty();
		for (LoopLabel label : this.loopLabels) {
			if (name.equals(label.name)) return true;
		}
		return false;
	}

	public static record LoopLabel(@Nullable String name, int frame) {}
}