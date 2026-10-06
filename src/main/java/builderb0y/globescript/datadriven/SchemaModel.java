package builderb0y.globescript.datadriven;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonElement;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
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

	public boolean matches(JsonElement self) {
		PsiElement start = self;
		if (start instanceof JsonStringLiteral) start = start.getParent();
		if (start instanceof JsonArray) start = start.getParent();
		if (start instanceof JsonProperty property && property.getName().equals("source")) {
			start = start.getParent();
		}
		else {
			start = self;
		}
		start = this.jsonPath.getRootFor(start);
		return start != null && this.when.test(start);
	}

	public ScriptEnvironment copyEnvironment(StandardTypes standardTypes, PsiElement source) {
		return new ScriptEnvironment(standardTypes, source, this.environments);
	}
}