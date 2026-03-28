package builderb0y.globescript;

import java.util.List;

public interface OneOrMoreTokens {

	public abstract void addTo(List<Token> list);

	public abstract Token group();

	public default Token group(TokenInfo info) {
		return this.group().withInfo(info);
	}
}