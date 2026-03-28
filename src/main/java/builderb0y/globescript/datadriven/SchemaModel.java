package builderb0y.globescript.datadriven;

import java.util.regex.Pattern;

import com.intellij.json.psi.JsonElement;

import builderb0y.globescript.ScriptEnvironment;
import builderb0y.globescript.datadriven.DataContext.StandardTypes;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class SchemaModel {

	public final Pattern filePath, jsonPath;
	public final When when;
	public final EnvironmentModel environment;

	public SchemaModel(Pattern filePath, Pattern jsonPath, When when, EnvironmentModel... environments) {
		this.filePath = filePath;
		this.jsonPath = jsonPath;
		this.when = when;
		this.environment = new EnvironmentModel(environments);
	}

	public boolean matches(String filePath, String jsonPath, JsonElement root) {
		return (
			this.filePath.matcher(filePath).find() &&
			this.jsonPath.matcher(jsonPath).find() &&
			this.when.test(root)
		);
	}

	public ScriptEnvironment copyEnvironment(StandardTypes standardTypes) {
		return new ScriptEnvironment(standardTypes, this.environment);
	}
}