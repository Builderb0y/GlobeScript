package builderb0y.globescript.datadriven;

import builderb0y.globescript.datadriven.PendingSchema.JsonPath;
import builderb0y.globescript.datadriven.PendingSchema.When;

public class ReferenceModel {

	public final JsonPath jsonPath;
	public final When when;
	public final String registry, defaultNamespace;
	public final PendingReference.Type type;

	public ReferenceModel(
		String registry,
		String defaultNamespace,
		JsonPath jsonPath,
		When when,
		PendingReference.Type type
	) {
		this.registry = registry;
		this.defaultNamespace = defaultNamespace;
		this.jsonPath = jsonPath;
		this.when = when;
		this.type = type;
	}
}