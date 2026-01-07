package builderb0y.globescript;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ScriptEnvironment.IdentifierHandler;
import builderb0y.globescript.ScriptEnvironment.IdentifierTypes;
import builderb0y.globescript.ScriptEnvironment.Keyword;

public class ExpressionParser {

	public final ExpressionReader reader;
	public final ScriptEnvironment environment;

	public ExpressionParser(ExpressionReader reader, ScriptEnvironment environment) {
		this.reader = reader;
		this.environment = environment;
	}

	public Token parseEntireInput() {
		Token script = this.nextScript();
		if (this.reader.peekAfterWhitespace() != -1) {
			List<Token> tokens = new ArrayList<>();
			tokens.add(script);
			do {
				tokens.add(this.nextAnything(false));
			}
			while (this.reader.peekAfterWhitespace() != -1);
			return new Token(this.reader.input, tokens);
		}
		return script;
	}

	public static final Set<CharSequence> END_OF_SCRIPT = Util.charSequenceSet(",", ":");

	public Token nextScript() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextCompoundExpression());
		while (true) {
			int peek = this.reader.peekAfterWhitespace();
			if (peek == -1 || peek == ')' || peek == ']' || peek == '}') break;
			CharSequence operator = this.reader.peekOperatorAfterWhitespace();
			if (operator != null) {
				if (END_OF_SCRIPT.contains(operator)) break;
				if (!PREFIXES.contains(operator)) {
					int start = this.reader.cursor;
					this.reader.skip(operator.length());
					int end = this.reader.cursor;
					tokens.add(new Token(this.reader.input, start, end, Colors.ERROR).withTooltip("Unknown or unexpected operator"));
				}
			}
			tokens.add(this.nextCompoundExpression());
		}
		return new Token(this.reader.input, tokens);
	}

	public Token nextCompoundExpression() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextSingleExpression());
		while (this.reader.hasOperatorAfterWhitespace(",,", Colors.OPERATOR, tokens)) {
			tokens.add(this.nextSingleExpression());
		}
		return new Token(this.reader.input, tokens);
	}

	public Token nextSingleExpression() {
		return this.nextAssignment();
	}

	public static final Set<CharSequence> ASSIGNMENTS = Util.charSequenceSet(
		"=", ":=", "=:",
		"+=", ":+", "+:",
		"-=", ":-", "-:",
		"*=", ":*", "*:",
		"/=", ":/", "/:",
		"%=", ":%", "%:",
		"^=", ":^", "^:",
		"&=", ":&", "&:",
		"|=", ":|", "|:",
		"#=", ":#", "#:",
		"&&=", ":&&", "&&:",
		"||=", ":||", "||:",
		"##=", ":##", "##:",
		"<<=", ":<<", "<<:",
		">>=", ":>>", ">>:",
		"<<<=", ":<<<", "<<<:",
		">>>=", ":>>>", ">>>:"
	);

	public Token nextAssignment() {
		Token left = this.nextTernary();
		Token assignOp = this.reader.hasOperatorAfterWhitespace(ASSIGNMENTS, Colors.OPERATOR);
		if (assignOp != null) {
			Token right = this.nextSingleExpression();
			return new Token(this.reader.input, left, assignOp, right);
		}
		return left;
	}

	public Token nextTernary() {
		Token left = this.nextLogical();
		Token question = this.reader.hasOperatorAfterWhitespace("?", Colors.OPERATOR);
		if (question != null) {
			Token middle = this.nextSingleExpression();
			Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon != null) {
				Token right = this.nextSingleExpression();
				return new Token(this.reader.input, left, question, middle, colon, right);
			}
			else {
				return new Token(this.reader.input, left, question, middle);
			}
		}
		else {
			return left;
		}
	}

	public static final Set<CharSequence> LOGICALS = Util.charSequenceSet(
		"&&", "!&&",
		"||", "!||",
		"##", "!##"
	);

	public Token nextLogical() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextCompare());
		while (this.reader.hasOperatorAfterWhitespace(LOGICALS, Colors.OPERATOR, tokens)) {
			tokens.add(this.nextCompare());
		}
		return tokens.size() == 1 ? tokens.get(0) : new Token(this.reader.input, tokens);
	}

	public static final Set<CharSequence> COMPARES = Util.charSequenceSet(
		"<",   ".<",   "<.",
		">",   ".>",   ">.",
		"<=",  ".<=",  "<=.",
		">=",  ".>=",  ">=.",
		"==",  ".==",  "==.",
		"!=",  ".!=",  "!=.",
		"===", ".===", "===.",
		"!==", ".!==", "!==.",
		"!>",  ".!>",  "!>.",
		"!<",  ".!<",  "!<.",
		"!>=", ".!>=", "!>=.",
		"!<=", ".!<=", "!<=."
	);

	public Token nextCompare() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextSum());
		if (this.reader.hasOperatorAfterWhitespace(COMPARES, Colors.OPERATOR, tokens)) {
			tokens.add(this.nextSum());
		}
		return tokens.size() == 1 ? tokens.get(0) : new Token(this.reader.input, tokens);
	}

	public static final Set<CharSequence> SUMS = Util.charSequenceSet(
		"+", "-", "&", "|", "#"
	);

	public Token nextSum() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextProduct());
		while (this.reader.hasOperatorAfterWhitespace(SUMS, Colors.OPERATOR, tokens)) {
			tokens.add(this.nextProduct());
		}
		return tokens.size() == 1 ? tokens.get(0) : new Token(this.reader.input, tokens);
	}

	public static final Set<CharSequence> PRODUCTS = Util.charSequenceSet(
		"*", "/", "<<", ">>", "<<<", ">>>"
	);

	public Token nextProduct() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextExponent());
		while (this.reader.hasOperatorAfterWhitespace(PRODUCTS, Colors.OPERATOR, tokens)) {
			tokens.add(this.nextExponent());
		}
		return tokens.size() == 1 ? tokens.get(0) : new Token(this.reader.input, tokens);
	}

	public Token nextExponent() {
		Token left = this.nextElvis();
		Token power = this.reader.hasOperatorAfterWhitespace("^", Colors.OPERATOR);
		if (power != null) {
			return new Token(this.reader.input, left, power, this.nextExponent());
		}
		else {
			return left;
		}
	}

	public Token nextElvis() {
		Token left = this.nextPrefix();
		Token elvis = this.reader.hasOperatorAfterWhitespace("?:", Colors.OPERATOR);
		if (elvis != null) {
			return new Token(this.reader.input, left, elvis, this.nextElvis());
		}
		else {
			return left;
		}
	}

	public static final Set<CharSequence> PREFIXES = Util.charSequenceSet(
		"+", "-", "~", "!",
		"++", ":++", "++:",
		"--", ":--", "--:"
	);

	public Token nextPrefix() {
		Token prefix = this.reader.hasOperatorAfterWhitespace(PREFIXES, Colors.OPERATOR);
		if (prefix != null) {
			return new Token(this.reader.input, prefix, this.nextMember());
		}
		else {
			return this.nextMember();
		}
	}

	public static final Set<CharSequence> MEMBERS = Util.charSequenceSet(
		".",
		".=", ".?", ".$",
		".=?", ".=$", ".?=", ".?$", ".$=", ".$?",
		".=?$", ".=$?", ".?=$", ".?$=", ".$=?", ".$?="
	);

	public Token nextMember() {
		Token left = this.nextTerm();
		while (true) {
			Token middle = this.reader.hasOperatorAfterWhitespace(MEMBERS, Colors.OPERATOR);
			if (middle != null) {
				Token memberName = this.reader.nextIdentifierAfterWhitespace();
				if (memberName == null) memberName = this.error("Expected member name");
				left = new Token(this.reader.input, left, middle, this.nextIdentifier(memberName, memberName.getIdentifierText(), true));
			}
			else {
				break;
			}
		}
		return left;
	}

	public Token nextTerm() {
		return this.nextAnything(true);
	}

	@SuppressWarnings("deprecation")
	public Token nextAnything(boolean requireTerm) {
		int first = this.reader.peekAfterWhitespace();
		return switch (first) {
			case -1 -> {
				yield this.error("Unexpected end of file");
			}
			case
				'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
				'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
				'_', '`'
			-> {
				Token identifier = this.reader.nextIdentifier();
				if (requireTerm) identifier = this.nextIdentifier(identifier, identifier.getIdentifierText(), false);
				yield identifier;
			}
			case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
				yield this.reader.nextNumber();
			}
			case '\'', '"' -> {
				yield this.nextString(first);
			}
			case '!', '#', '$', '%', '&', '*', '+', ',', '-', '.', '/', ':', '<', '=', '>', '?', '@', '\\', '^', '|', '~' -> {
				yield this.reader.nextOperator().withColor(Colors.ERROR).withTooltip("Expected identifier, number, or string");
			}
			case '(' -> {
				Token open = this.tryOpenGroup();
				if (requireTerm) {
					Token body = this.nextScript();
					Token close = this.closeGroup();
					yield new Token(this.reader.input, Token.filterNulls(open, body, close));
				}
				else {
					yield open;
				}
			}
			case ')', '[', ']', '{', '}' -> {
				this.reader.skip();
				if (requireTerm) {
					yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, Colors.ERROR).withTooltip("Expected identifier, number, or string");
				}
				else {
					yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, Colors.GROUP);
				}
			}
			default -> {
				this.reader.skip();
				yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, Colors.ERROR).withTooltip("Expected identifier, number, or string");
			}
		};
	}

	public Token nextIdentifier(Token identifier, CharSequence name, boolean member) {
		IdentifierHandler handler = this.environment.identifierTypes.get(name);
		if (!(handler instanceof Keyword)) {
			Token open = this.tryOpenGroup();
			if (open != null) {
				identifier.color = member ? Colors.METHOD : Colors.FUNCTION;
				Token close = this.tryCloseGroup();
				if (close != null) {
					return new Token(this.reader.input, identifier, open, close);
				}
				List<Token> tokens = new ArrayList<>();
				tokens.add(identifier);
				tokens.add(open);
				tokens.add(this.nextScript());
				while (this.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, tokens)) {
					tokens.add(this.nextScript());
				}
				tokens.add(this.closeGroup());
				return new Token(this.reader.input, tokens);
			}
		}
		return handler.handle(this, identifier, member);
	}

	public static final Set<CharSequence> DECLARE_ASSIGNMENTS = Util.charSequenceSet("=", ":=");
	public static final Set<CharSequence> COMMAS = Util.charSequenceSet(",", ",,");

	public Token handleTypeIdentifier(Token type) {
		type.color = Colors.TYPE;
		//multi-declaration:
		//int*(x = 1, y = 2, z = 3)
		Token star = this.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
		if (star != null) {
			Token open = this.reader.hasAfterWhitespace('(', Colors.GROUP);
			if (open != null) {
				List<Token> tokens = new ArrayList<>();
				tokens.add(type);
				tokens.add(star);
				tokens.add(open);
				while (true) {
					Token declaration = this.nextMultiDeclarationPart();
					if (declaration != null) {
						tokens.add(declaration);
						this.reader.hasOperatorAfterWhitespace(COMMAS, Colors.OPERATOR, tokens);
					}
					else {
						break;
					}
				}
				if (!this.reader.hasAfterWhitespace(')', Colors.GROUP, tokens)) {
					tokens.add(this.abort(null, "Expected ')'"));
				}
				return new Token(this.reader.input, tokens);
			}
			else {
				return new Token(this.reader.input, type, star, this.error("Expected '('"));
			}
		}
		//nullable cast:
		//Block?('modid:example')
		Token question = this.reader.hasOperatorAfterWhitespace("?", Colors.OPERATOR);
		if (question != null) {
			Token open = this.tryOpenGroup();
			if (open != null) {
				Token contents = this.nextScript();
				Token close = this.closeGroup();
				return new Token(this.reader.input, type, question, open, contents, close);
			}
			else {
				open = this.error("Expected '('");
				return new Token(this.reader.input, type, question, open);
			}
		}
		//non-null cast:
		//Block('modid:example')
		Token open = this.tryOpenGroup();
		if (open != null) {
			Token contents = this.nextScript();
			Token close = this.closeGroup();
			return new Token(this.reader.input, Token.filterNulls(type, open, contents, close));
		}
		//more stuff:
		//Block name ...
		Token name = this.reader.nextIdentifierAfterWhitespace();
		if (name == null) {
			return type;
		}
		//declaration:
		//Block example = 'modid:example'
		Token assign = this.reader.hasOperatorAfterWhitespace(DECLARE_ASSIGNMENTS, Colors.OPERATOR);
		if (assign != null) {
			name.color = Colors.LOCAL;
			this.environment.addLocal(name.getIdentifierText().toString());
			Token value = this.nextSingleExpression();
			return new Token(this.reader.input, type, name, assign, value);
		}
		//function declaration:
		//Block get(...)
		open = this.tryOpenGroup();
		if (open != null) {
			name.color = Colors.FUNCTION;
			Token parameters = this.nextParameters();
			Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			this.environment.addParameter("this");
			Token body = this.nextScript();
			Token close = this.closeGroup();
			return new Token(this.reader.input, Token.filterNulls(type, name, open, parameters, colon, body, close));
		}
		//extension method declaration:
		//int Block.getWhatever(...)
		Token dot = this.reader.hasOperatorAfterWhitespace(".", Colors.OPERATOR);
		if (dot != null) {
			name.color = this.environment.identifierTypes.get(name.getIdentifierText()) == IdentifierTypes.TYPE ? Colors.TYPE : Colors.ERROR;
			Token methodName = this.reader.nextIdentifierAfterWhitespace();
			if (methodName == null) {
				Token error = this.error("Expected method name");
				return new Token(this.reader.input, type, name, dot, error);
			}
			methodName.color = Colors.METHOD;
			open = this.tryOpenGroup();
			if (open == null) {
				Token error = this.error("Expected '('");
				return new Token(this.reader.input, type, name, dot, methodName, error);
			}
			Token parameters = this.nextParameters();
			Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			this.environment.addParameter("this");
			Token body = this.nextScript();
			Token close = this.closeGroup();
			return new Token(this.reader.input, Token.filterNulls(type, name, dot, methodName, open, parameters, colon, body, close));
		}
		//???
		return type;
	}

	public @Nullable Token nextParameters() {
		Token first = this.nextParameter(false);
		if (first == null) return null;
		List<Token> tokens = new ArrayList<>();
		tokens.add(first);
		while (this.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, tokens)) {
			Token next = this.nextParameter(false);
			tokens.add(next != null ? next : this.abort(COMMA, "Expected parameter"));
		}
		return new Token(this.reader.input, tokens);
	}

	public @Nullable Token nextParameter(boolean forFields) {
		Token type = this.reader.nextIdentifierAfterWhitespace();
		if (type == null) {
			return null;
		}
		type.color = this.environment.identifierTypes.get(type.getIdentifierText()) == IdentifierTypes.TYPE ? Colors.TYPE : Colors.ERROR;
		Token star = this.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
		if (star != null) {
			Token open = this.reader.hasAfterWhitespace('(', Colors.GROUP);
			if (open == null) {
				return new Token(this.reader.input, type, star, this.error("Expected '('"));
			}
			Token close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
			if (close != null) {
				return new Token(this.reader.input, type, star, open, close);
			}
			Token name = this.reader.nextIdentifierAfterWhitespace();
			if (name == null) {
				name = this.abort(COMMA, "Expected name");
				close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
				return new Token(this.reader.input, Token.filterNulls(type, star, open, name, close));
			}
			if (forFields) {
				name.color = Colors.FIELD;
				this.environment.addField(name.getIdentifierText().toString());
			}
			else {
				name.color = Colors.PARAMETER;
				this.environment.addParameter(name.getIdentifierText().toString());
			}
			List<Token> tokens = new ArrayList<>();
			tokens.add(type);
			tokens.add(star);
			tokens.add(open);
			tokens.add(name);
			while (this.reader.hasOperatorAfterWhitespace(",", Colors.OPERATOR, tokens)) {
				name = this.reader.nextIdentifierAfterWhitespace();
				if (name != null) {
					if (forFields) {
						name.color = Colors.FIELD;
						this.environment.addField(name.getIdentifierText().toString());
					}
					else {
						name.color = Colors.PARAMETER;
						this.environment.addParameter(name.getIdentifierText().toString());
					}
					tokens.add(name);
				}
				else {
					tokens.add(this.abort(COMMA, "Expected name"));
					break;
				}
			}
			this.reader.hasAfterWhitespace(')', Colors.GROUP, tokens);
			return new Token(this.reader.input, tokens);
		}
		else {
			Token name = this.reader.nextIdentifierAfterWhitespace();
			if (name == null) {
				Token anything = this.abort(COMMA, "Expected name");
				return new Token(this.reader.input, type, anything);
			}
			if (forFields) {
				name.color = Colors.FIELD;
				this.environment.addField(name.getIdentifierText().toString());
			}
			else {
				name.color = Colors.PARAMETER;
				this.environment.addParameter(name.getIdentifierText().toString());
			}
			return new Token(this.reader.input, type, name);
		}
	}

	public static final Set<CharSequence> COMMA = Util.charSequenceSet(",");

	public @Nullable Token nextMultiDeclarationPart() {
		Token name = this.reader.nextIdentifierAfterWhitespace();
		if (name != null) {
			name.color = Colors.LOCAL;
			this.environment.addLocal(name.getIdentifierText().toString());
			Token operator = this.reader.hasOperatorAfterWhitespace(DECLARE_ASSIGNMENTS, Colors.OPERATOR);
			Token value = this.nextSingleExpression();
			return new Token(this.reader.input, Token.filterNulls(name, operator, value));
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public Token nextString(int quote) {
		int start = this.reader.cursor;
		this.reader.skip();
		List<Token> parts = new ArrayList<>(4);
		loop:
		while (true) {
			int read = this.reader.peek();
			if (read == -1) {
				Token token = new Token(this.reader.input, start, this.reader.cursor, Colors.STRING);
				if (parts.isEmpty()) return token;
				parts.add(token);
				return new Token(this.reader.input, parts);
			}
			else if (read == quote) {
				this.reader.skip();
				Token token = new Token(this.reader.input, start, this.reader.cursor, Colors.STRING);
				if (parts.isEmpty()) return token;
				parts.add(token);
				return new Token(this.reader.input, parts);
			}
			else if (read == '$') {
				int operatorStart = this.reader.cursor;
				String operator = this.reader.readOperator().toString();
				boolean member;
				switch (operator) {
					case "$",  "$:"  -> member = false;
					case "$.", "$:." -> member = true;
					case "$$" -> {
						continue loop;
					}
					default -> {
						parts.add(new Token(this.reader.input, start, operatorStart, Colors.STRING));
						parts.add(new Token(this.reader.input, operatorStart, this.reader.cursor, Colors.OPERATOR));
						start = this.reader.cursor;
						continue loop;
					}
				}
				parts.add(new Token(this.reader.input, start, operatorStart, Colors.STRING));
				parts.add(new Token(this.reader.input, operatorStart, this.reader.cursor, Colors.OPERATOR));
				Token interpolation = member ? this.nextElvis() : this.nextTerm();
				parts.add(interpolation);
				this.reader.rollback(start = interpolation.range.getEndOffset());
			}
			else if (read == '\n' || read == '\r') {
				if (this.reader.splitNewLinesInMultiLineTokens()) {
					if (this.reader.cursor > start) {
						parts.add(new Token(this.reader.input, start, this.reader.cursor, Colors.STRING));
						this.reader.skip();
						start = this.reader.cursor;
					}
					else {
						this.reader.skip();
					}
				}
				else {
					this.reader.skip();
				}
			}
			else {
				this.reader.skip();
			}
		}
	}

	public @Nullable Token tryOpenGroup() {
		Token open = this.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open != null) this.environment.push();
		return open;
	}

	public @Nullable Token tryCloseGroup() {
		Token close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close != null) this.environment.push();
		return close;
	}

	public @Nullable Token closeGroup() {
		this.environment.pop();
		Token close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close != null) {
			return close;
		}
		Token abortion = this.abort(null, "Expected ')'");
		close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close != null) {
			return new Token(this.reader.input, abortion, close);
		}
		else {
			return abortion;
		}
	}

	public Token error(String tooltip) {
		return new Token(this.reader.input, this.reader.cursor, this.reader.cursor, Colors.ERROR).withTooltip(tooltip);
	}

	public Token abort(Set<CharSequence> stopAt, String message) {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.error(message));
		int depth = 0;
		loop:
		while (this.reader.canReadAfterWhitespace()) {
			Token anything = this.nextAnything(false);
			CharSequence sequence = anything.getText();
			if (sequence.length() == 1) {
				switch (sequence.charAt(0)) {
					case '(', '[', '{' -> {
						depth++;
					}
					case ')', ']', '}' -> {
						if (--depth < 0) {
							this.reader.skip(-1);
							break loop;
						}
					}
				}
			}
			if (depth == 0 && stopAt != null && stopAt.contains(sequence)) {
				this.reader.rollback(anything.range.getStartOffset());
				break;
			}
			tokens.add(anything);
		}
		return new Token(this.reader.input, tokens);
	}
}