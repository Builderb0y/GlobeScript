package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.datadriven.RawTypeModel;

public class WhileKeyword extends WhileOrRepeatKeyword {

	public WhileKeyword(String name, TextAttributesKey color) {
		super(name, color);
	}

	@Override
	public RawTypeModel getExpectedConditionType(ExpressionParser parser) {
		return parser.environment.standardTypes.boolean_;
	}

	@Override
	public String toString() {
		return this.name + " (condition: body)";
	}
}