package builderb0y.globescript.datadriven;

import java.util.regex.Pattern;

import com.intellij.json.psi.JsonElement;
import com.intellij.psi.PsiElement;

import builderb0y.globescript.ScriptEnvironment;
import builderb0y.globescript.datadriven.DataContext.StandardTypes;
import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class SchemaModel {

	public final Pattern filePath;
	public final JsonPath jsonPath;
	public final When when;
	public final EnvironmentModel environment;

	public SchemaModel(Pattern filePath, JsonPath jsonPath, When when, EnvironmentModel... environments) {
		this.filePath = filePath;
		this.jsonPath = jsonPath;
		this.when = when;
		this.environment = new EnvironmentModel(environments);
	}

	public boolean matches(String filePath, PsiElement self) {
		PsiElement start;
		return (
			this.filePath.matcher(filePath).find() &&
			(start = this.jsonPath.getRootFor(self)) != null &&
			this.when.test(start)
		);
	}

	public ScriptEnvironment copyEnvironment(StandardTypes standardTypes) {
		return new ScriptEnvironment(standardTypes, this.environment);
	}
}