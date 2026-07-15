package builderb0y.globescript.datadriven;

import com.intellij.psi.PsiElement;

import builderb0y.globescript.ScriptEnvironment;
import builderb0y.globescript.datadriven.GsEnv.StandardTypes;
import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class SchemaModel {

	public final ID registry;
	public final JsonPath jsonPath;
	public final When when;
	public final EnvironmentConfigurator[] environments;

	public SchemaModel(ID registry, JsonPath jsonPath, When when, EnvironmentConfigurator... environments) {
		this.registry = registry;
		this.jsonPath = jsonPath;
		this.when = when;
		this.environments = environments;
	}

	public boolean matches(PsiElement self) {
		PsiElement start;
		return (
			(start = this.jsonPath.getRootFor(self)) != null &&
			this.when.test(start)
		);
	}

	public ScriptEnvironment copyEnvironment(StandardTypes standardTypes, PsiElement source) {
		return new ScriptEnvironment(standardTypes, source, this.environments);
	}
}