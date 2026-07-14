package builderb0y.globescript.datadriven;

import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class ReferenceModel {

	public final TargetModel reference, declaration;
	public final String defaultNamespace;
	public final PendingReference.Type type;

	public ReferenceModel(TargetModel reference, TargetModel declaration, String defaultNamespace, PendingReference.Type type) {
		this.reference = reference;
		this.declaration = declaration;
		this.defaultNamespace = defaultNamespace;
		this.type = type;
	}

	public static class TargetModel {

		public final ID registry;
		public final JsonPath jsonPath;
		public final When condition;

		public TargetModel(ID registry, JsonPath jsonPath, When condition) {
			this.registry  = registry;
			this.jsonPath  = jsonPath;
			this.condition = condition;
		}
	}
}