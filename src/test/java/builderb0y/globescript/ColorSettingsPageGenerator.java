package builderb0y.globescript;

import java.util.stream.Stream;

public class ColorSettingsPageGenerator {

	/*
	public static void main(String[] args) {
		String text = """
		;line comment
		;;block comment;;
		;(scoped comment)

		class Box(int value)
		Box box = new(42)
		void Box.add(int value:
			if (true: print('string'))
			this.value += value
		)
		""";
		ExpressionParser parser = new ExpressionParser(
			new ExpressionReader(text, 0, text.length()),
			new ScriptEnvironment().addEnvironment(ScriptEnvironment.BUILTIN)
		);
		Token tree = parser.nextScript();
		Token prev = null;
		for (Token token : Stream.concat(parser.reader.comments.stream(), tree.streamLeaves()).toArray(Token[]::new)) {
			if (prev != null) System.out.print(text.substring(prev.range.getEndOffset(), token.range.getStartOffset()));
			String colorName = token.color.getExternalName().substring("GLOBESCRIPT_".length());
			System.out.print('<' + colorName + '>' + token.getText() + "</" + colorName + '>');
			prev = token;
		}
	}
	*/
}