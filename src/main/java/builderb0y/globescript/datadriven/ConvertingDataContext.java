package builderb0y.globescript.datadriven;

import java.util.Map;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import com.intellij.psi.PsiElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingEnvironment.*;

public class ConvertingDataContext {

	public final PendingDataContext pending;
	public final Map<String, RawTypeModel> types = new Object2ObjectOpenHashMap<>();
	public final Map<String, EnvironmentModel> environments = new Object2ObjectOpenHashMap<>();

	public final SequencedSet<String> typeStack = new ObjectLinkedOpenHashSet<>();
	public final SequencedSet<String> environmentStack = new ObjectLinkedOpenHashSet<>();

	public ConvertingDataContext(PendingDataContext pending) {
		this.pending = pending;
	}

	public @Nullable RawTypeModel getType(String name, PsiElement requester) {
		if (name == null) {
			return null;
		}
		else if (this.typeStack.add(name)) try {
			RawTypeModel info = this.types.get(name);
			if (info == null) {
				PendingType pending = this.pending.types.get(name);
				if (pending == null) {
					this.pending.addError(requester, "Unknown type: " + name);
					return null;
				}
				this.types.put(
					name,
					info = new RawTypeModel(
						pending.modifiers,
						name,
						this.getType(pending.superClass),
						this.getTypes(pending.superInterfaces),
						pending.enumConstants
					)
				);
			}
			return info;
		}
		catch (RuntimeException exception) {
			this.pending.addError(requester, exception);
			return null;
		}
		finally {
			this.typeStack.remove(name);
		}
		else {
			this.pending.addError(requester, this.typeStack.stream().collect(Collectors.joining(" -> ", "Cyclic type detected: ", " -> " + name)));
			return null;
		}
	}

	public RawTypeModel getType(PendingSuperType superType) {
		return superType == null ? null : this.getType(superType.name, superType.element);
	}

	public RawTypeModel[] getTypes(PendingSuperType[] names) {
		if (names == null) return RawTypeModel.EMPTY_ARRAY;
		int length = names.length;
		RawTypeModel[] types = new RawTypeModel[length];
		for (int index = 0; index < length; index++) {
			types[index] = this.getType(names[index]);
		}
		return types;
	}

	public EnvironmentModel getEnvironment(String name, PsiElement requester) {
		if (name == null) {
			return null;
		}
		else if (this.environmentStack.add(name)) try {
			EnvironmentModel environment = this.environments.get(name);
			if (environment == null) {
				PendingEnvironment pending = this.pending.environments.get(name);
				if (pending == null) {
					this.pending.addError(requester, "Unknown environment: " + name);
					return null;
				}
				environment = new EnvironmentModel(name);
				if (pending.includes         != null) for (PendingEnvironmentReference include  : pending.includes        ) environment.addAll           (include .resolve(this));
				if (pending.types            != null) for (PendingExposedType          type     : pending.types           ) environment.addType          (type    .resolve(this));
				if (pending.variables        != null) for (PendingVariable             variable : pending.variables       ) environment.addVariable      (variable.resolve(this));
				if (pending.instanceFields   != null) for (PendingInstanceField        field    : pending.instanceFields  ) environment.addInstanceField (field   .resolve(this));
				if (pending.staticFields     != null) for (PendingStaticField          field    : pending.staticFields    ) environment.addStaticField   (field   .resolve(this));
				if (pending.functions        != null) for (PendingFunction             function : pending.functions       ) environment.addFunction      (function.resolve(this));
				if (pending.instanceMethods  != null) for (PendingInstanceMethod       method   : pending.instanceMethods ) environment.addInstanceMethod(method  .resolve(this));
				if (pending.staticMethods    != null) for (PendingStaticMethod         method   : pending.staticMethods   ) environment.addStaticMethod  (method  .resolve(this));
				if (pending.keywords         != null) for (PendingKeyword              keyword  : pending.keywords        ) environment.addKeyword       (keyword .resolve(this));
				if (pending.instanceKeywords != null) for (PendingInstanceKeyword      keyword  : pending.instanceKeywords) environment.addMemberKeyword (keyword .resolve(this));
				if (pending.casters          != null) for (PendingCaster               caster   : pending.casters         ) environment.addCaster        (caster  .resolve(this));
				this.environments.put(name, environment);
			}
			return environment;
		}
		catch (RuntimeException exception) {
			this.pending.addError(requester, exception);
			return null;
		}
		finally {
			this.environmentStack.remove(name);
		}
		else {
			this.pending.addError(requester, this.environmentStack.stream().collect(Collectors.joining(" -> ", "Cyclic environment detected: ", " -> " + name)));
			return null;
		}
	}

	public EnvironmentModel[] getEnvironments(PendingEnvironmentReference[] references) {
		int length = references.length;
		EnvironmentModel[] environments = new EnvironmentModel[length];
		for (int index = 0; index < length; index++) {
			environments[index] = this.getEnvironment(references[index].name, references[index].element);
		}
		return environments;
	}
}