package builderb0y.globescript.datadriven;

import java.util.*;
import java.util.stream.Collectors;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.datadriven.PendingEnvironment.*;

public class ConvertingDataContext {

	public final PendingDataContext pending;
	public final Map<String, RawTypeModel> types = new Object2ObjectOpenHashMap<>();
	public final Map<String, EnvironmentConfigurator> environments = new Object2ObjectOpenHashMap<>();

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

	public EnvironmentConfigurator getEnvironment(String name, PsiElement requester) {
		class ColumnValueConfigurator extends EnvironmentConfigurator {

			public final int flags;

			public ColumnValueConfigurator(String name, int flags) {
				super(name);
				this.flags = flags;
			}

			@Override
			public void configure(VirtualFile source, EnvironmentModel environment) {
				PackData pack = ConvertingDataContext.this.pending.projectData.getPackData(source);
				if (pack != null) pack.columnValues.setupEnvironment(environment, source, this.flags);
			}
		}
		return switch (name) {
			case null -> null;
			case "custom_class" -> new EnvironmentConfigurator(name) {

				@Override
				public void configure(VirtualFile source, EnvironmentModel environment) {
					PackData pack = ConvertingDataContext.this.pending.projectData.getPackData(source);
					if (pack != null) pack.customClasses.setupEnvironment(environment);
				}
			};
			case "column_value/without_xyz" -> new ColumnValueConfigurator(name, 0);
			case "column_value/with_y"      -> new ColumnValueConfigurator(name, ColumnValueEnvironment.FLAG_Y_PROVIDED);
			case "column_value/with_xz"     -> new ColumnValueConfigurator(name, ColumnValueEnvironment.FLAG_XZ_PROVIDED);
			case "column_value/with_xyz"    -> new ColumnValueConfigurator(name, ColumnValueEnvironment.FLAG_XYZ_PROVIDED);
			default -> {
				if (this.environmentStack.add(name)) try {
					EnvironmentConfigurator configurator = this.environments.get(name);
					if (configurator == null) {
						PendingEnvironment pending = this.pending.environments.get(name);
						if (pending == null) {
							this.pending.addError(requester, "Unknown environment: " + name);
							yield null;
						}
						EnvironmentModel environment = new EnvironmentModel(name);
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
						if (pending.includes         != null) {
							List<EnvironmentConfigurator> includes = new ArrayList<>(pending.includes.length);
							for (PendingEnvironmentReference include : pending.includes) {
								includes.add(include.resolve(this));
							}
							configurator = new EnvironmentConfigurator(name) {

								@Override
								public void configure(VirtualFile source, EnvironmentModel toConfigure) {
									environment.configure(source, toConfigure);
									for (EnvironmentConfigurator include : includes) {
										include.configure(source, toConfigure);
									}
								}
							};
						}
						else {
							configurator = environment;
						}
						this.environments.put(name, configurator);
					}
					yield configurator;
				}
				catch (RuntimeException exception) {
					this.pending.addError(requester, exception);
					yield null;
				}
				finally {
					this.environmentStack.remove(name);
				}
				else {
					this.pending.addError(requester, this.environmentStack.stream().collect(Collectors.joining(" -> ", "Cyclic environment detected: ", " -> " + name)));
					yield null;
				}
			}
		};
	}

	public EnvironmentConfigurator[] getEnvironments(PendingEnvironmentReference[] references) {
		int length = references.length;
		EnvironmentConfigurator[] environments = new EnvironmentConfigurator[length];
		int writeIndex = 0;
		for (PendingEnvironmentReference reference : references) {
			EnvironmentConfigurator environment = this.getEnvironment(reference.name, reference.element);
			if (environment != null) environments[writeIndex++] = environment;
			else if (reference.element != null) this.pending.addError(reference.element, "Can't find environment named " + reference.name);
			else throw new RuntimeException("Can't find environment named " + reference.name);
		}
		if (writeIndex != length) environments = Arrays.copyOf(environments, writeIndex);
		return environments;
	}
}