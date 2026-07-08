package builderb0y.globescript.datadriven;

import java.util.Objects;
import java.util.regex.Pattern;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import builderb0y.globescript.ScriptEnvironment;
import builderb0y.globescript.datadriven.GsEnv.StandardTypes;
import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class SchemaModel {

	public final Pattern filePath;
	public final JsonPath jsonPath;
	public final When when;
	public final EnvironmentConfigurator[] environments;

	public SchemaModel(Pattern filePath, JsonPath jsonPath, When when, EnvironmentConfigurator... environments) {
		this.filePath = filePath;
		this.jsonPath = jsonPath;
		this.when = when;
		this.environments = environments;
	}

	public boolean matches(String filePath, PsiElement self) {
		PsiElement start;
		return (
			this.filePath.matcher(filePath).find() &&
			(start = this.jsonPath.getRootFor(self)) != null &&
			this.when.test(start)
		);
	}

	public ScriptEnvironment copyEnvironment(StandardTypes standardTypes, VirtualFile source) {
		return new ScriptEnvironment(standardTypes, source, this.environments);
	}
}