package builderb0y.globescript.datadriven;

import builderb0y.globescript.datadriven.PendingSchema.When;

public class RequiredTagModel {

	public final ID registry;
	public final When when;

	public RequiredTagModel(ID registry, When when) {
		this.registry = registry;
		this.when = when;
	}
}