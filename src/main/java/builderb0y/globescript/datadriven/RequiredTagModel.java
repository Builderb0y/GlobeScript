package builderb0y.globescript.datadriven;

import java.util.regex.Pattern;

import builderb0y.globescript.datadriven.PendingSchema.When;

public class RequiredTagModel {

	public final Pattern filePath;
	public final When when;

	public RequiredTagModel(Pattern filePath, When when) {
		this.filePath = filePath;
		this.when = when;
	}
}