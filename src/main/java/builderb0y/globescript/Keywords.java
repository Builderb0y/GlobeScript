package builderb0y.globescript;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class Keywords {

	public static Token parseClass(ExpressionParser parser, Token clazz, boolean member) {
		if (member) {
			clazz.color = Colors.ERROR;
			return clazz;
		}
		clazz.color = Colors.KEYWORD;
		Token name = parser.reader.nextIdentifierAfterWhitespace();
		if (name == null) {
			return new Token(parser.reader.input, clazz, parser.error("Expected class name"));
		}
		name.color = Colors.TYPE;
		parser.environment.addType(name.getIdentifierText().toString());
		Token open = parser.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open == null) {
			return new Token(parser.reader.input, clazz, name, parser.error("Expected '('"));
		}
		Token body = parseClassMembers(parser);
		Token close = parser.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close == null) close = parser.abort(null, "Expected ')'");
		return new Token(parser.reader.input, Token.filterNulls(clazz, name, open, body, close));
	}

	public static @Nullable Token parseClassMembers(ExpressionParser parser) {
		List<Token> tokens = new ArrayList<>();
		while (true) {
			Token parameter = parser.nextParameter(true);
			if (parameter != null) {
				tokens.add(parameter);
				parser.reader.hasOperatorAfterWhitespace(ExpressionParser.COMMAS, Colors.OPERATOR, tokens);
			}
			else {
				break;
			}
		}
		return tokens.isEmpty() ? null : new Token(parser.reader.input, tokens);
	}

	public static Token parseIf(ExpressionParser parser, Token if_, boolean member) {
		if_.color = Colors.KEYWORD;
		Token open = parser.tryOpenGroup();
		if (open != null) {
			Token condition = parser.nextScript();
			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			Token ifBody = parser.nextScript();
			Token ifClose = parser.closeGroup();
			Token else_ = parser.reader.hasIdentifierAfterWhitespace("else", Colors.KEYWORD);
			if (else_ != null) {
				Token elseOpen = parser.tryOpenGroup();
				Token elseBody = elseOpen != null ? parser.nextScript() : parser.nextSingleExpression();
				Token elseClose = parser.closeGroup();
				return new Token(parser.reader.input, Token.filterNulls(if_, open, condition, colon, ifBody, ifClose, else_, elseOpen, elseBody, elseClose));
			}
			else {
				return new Token(parser.reader.input, Token.filterNulls(if_, open, condition, colon, ifBody, ifClose));
			}
		}
		else {
			return if_;
		}
	}

	public static Token parseWhile(ExpressionParser parser, Token while_, boolean member) {
		if (member) {
			while_.color = Colors.ERROR;
			return while_;
		}
		while_.color = Colors.KEYWORD;
		Token name = parser.reader.nextIdentifierAfterWhitespace();
		Token open = parser.tryOpenGroup();
		if (open != null) {
			Token condition = parser.nextScript();
			Token colon = parser.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			Token body = parser.nextScript();
			Token close = parser.closeGroup();
			return new Token(parser.reader.input, Token.filterNulls(while_, name, open, condition, colon, body, close));
		}
		else {
			return while_;
		}
	}

	public static Token parseDo(ExpressionParser parser, Token do_, boolean member) {
		if (member) {
			do_.color = Colors.ERROR;
			return do_;
		}
		Token while_ = parser.reader.hasIdentifierAfterWhitespace("while", Colors.KEYWORD);
		if (while_ == null) {
			while_ = parser.reader.hasIdentifierAfterWhitespace("until", Colors.KEYWORD);
			if (while_ == null) {
				return do_.withColor(Colors.ERROR).withTooltip("'do' should be followed by 'while' or 'until'.");
			}
		}
		do_.color = Colors.KEYWORD;
		while_ = parseWhile(parser, while_, false);
		return new Token(parser.reader.input, do_, while_);
	}
}