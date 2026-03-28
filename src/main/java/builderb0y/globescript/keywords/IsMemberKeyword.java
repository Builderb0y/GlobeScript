package builderb0y.globescript.keywords;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import builderb0y.globescript.ExpressionParser;
import builderb0y.globescript.Token;
import builderb0y.globescript.TokenInfo;
import builderb0y.globescript.datadriven.RawTypeModel;

public class IsMemberKeyword extends IsOrAsMemberKeyword {

	public IsMemberKeyword(String name, TextAttributesKey color, RawTypeModel type) {
		super(name, color, type);
	}

	@Override
	public TokenInfo getType(ExpressionParser parser, Token type) {
		return new TokenInfo(parser.environment.standardTypes.boolean_);
	}
}