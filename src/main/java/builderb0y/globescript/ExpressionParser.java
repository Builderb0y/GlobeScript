package builderb0y.globescript;

import java.util.*;
import java.util.function.IntPredicate;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ConstantFolding.*;
import builderb0y.globescript.keywords.BodyIfKeyword;
import builderb0y.globescript.StructureParser.*;
import builderb0y.globescript.datadriven.EnvironmentModel.*;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;
import builderb0y.globescript.util.Util;

public class ExpressionParser {

	public final ExpressionReader reader;
	public final ScriptEnvironment environment;

	public ExpressionParser(ExpressionReader reader, ScriptEnvironment environment) {
		this.reader = reader;
		this.environment = environment;
	}

	public Token parseEntireInput() {
		Token script = this.nextScript();
		if (this.reader.peekAfterWhitespace() >= 0) {
			List<Token> tokens = new ArrayList<>();
			tokens.add(script);
			do {
				tokens.add(this.nextAnything(false));
			}
			while (this.reader.peekAfterWhitespace() >= 0);
			return new Token(this.reader.input, script.info, tokens);
		}
		return script;
	}

	public boolean hasExpressionStart() {
		int peek = this.reader.peekAfterWhitespace();
		if (peek == -1 || peek == ')' || peek == ']' || peek == '}') return false;
		CharSequence operator = this.reader.peekOperatorAfterWhitespace();
		if (operator != null && !PREFIXES.containsKey(operator)) return false;
		return true;
	}

	public static final Set<CharSequence> END_OF_SCRIPT = Util.charSequenceSet(",", ":");

	public @Nullable Token nextNullableScript() {
		return this.hasExpressionStart() ? this.nextScript() : null;
	}

	public Token nextScript() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextCompoundExpression());
		while (true) {
			int peek = this.reader.peekAfterWhitespace();
			if (peek == -1 || peek == ')' || peek == ']' || peek == '}') break;
			CharSequence operator = this.reader.peekOperatorAfterWhitespace();
			if (operator != null) {
				if (END_OF_SCRIPT.contains(operator)) break;
				if (!PREFIXES.containsKey(operator)) {
					int start = this.reader.cursor;
					this.reader.skip(operator.length());
					int end = this.reader.cursor;
					tokens.add(new Token(this.reader.input, start, end, TokenInfo.ERROR).withColor(Colors.ERROR).withTooltip("Unknown or unexpected operator"));
				}
			}
			tokens.add(this.nextCompoundExpression());
		}
		return new Token(this.reader.input, tokens.getLast().info, tokens);
	}

	public @Nullable Token nextNullableCompoundExpression() {
		return this.hasExpressionStart() ? this.nextCompoundExpression() : null;
	}

	public Token nextCompoundExpression() {
		List<Token> tokens = new ArrayList<>();
		tokens.add(this.nextSingleExpression());
		while (this.reader.hasOperatorAfterWhitespace(",,", Colors.OPERATOR, tokens)) {
			tokens.add(this.nextSingleExpression());
		}
		return new Token(this.reader.input, tokens.getLast().info, tokens);
	}

	public @Nullable Token nextNullableSingleExpression() {
		return this.hasExpressionStart() ? this.nextSingleExpression() : null;
	}

	public Token nextSingleExpression() {
		return this.nextAssignment();
	}

	public static enum AssignmentType {
		VOID,
		PRE,
		POST;
	}

	public static final Map<CharSequence, AssignmentType> ASSIGNMENTS = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		ASSIGNMENTS.put("=", AssignmentType.VOID);
		ASSIGNMENTS.put("+=", AssignmentType.VOID);
		ASSIGNMENTS.put("-=", AssignmentType.VOID);
		ASSIGNMENTS.put("*=", AssignmentType.VOID);
		ASSIGNMENTS.put("/=", AssignmentType.VOID);
		ASSIGNMENTS.put("%=", AssignmentType.VOID);
		ASSIGNMENTS.put("^=", AssignmentType.VOID);
		ASSIGNMENTS.put("&=", AssignmentType.VOID);
		ASSIGNMENTS.put("|=", AssignmentType.VOID);
		ASSIGNMENTS.put("#=", AssignmentType.VOID);
		ASSIGNMENTS.put("&&=", AssignmentType.VOID);
		ASSIGNMENTS.put("||=", AssignmentType.VOID);
		ASSIGNMENTS.put("##=", AssignmentType.VOID);
		ASSIGNMENTS.put("<<=", AssignmentType.VOID);
		ASSIGNMENTS.put(">>=", AssignmentType.VOID);
		ASSIGNMENTS.put("<<<=", AssignmentType.VOID);
		ASSIGNMENTS.put(">>>=", AssignmentType.VOID);

		ASSIGNMENTS.put("=:", AssignmentType.PRE);
		ASSIGNMENTS.put("+:", AssignmentType.PRE);
		ASSIGNMENTS.put("-:", AssignmentType.PRE);
		ASSIGNMENTS.put("*:", AssignmentType.PRE);
		ASSIGNMENTS.put("/:", AssignmentType.PRE);
		ASSIGNMENTS.put("%:", AssignmentType.PRE);
		ASSIGNMENTS.put("^:", AssignmentType.PRE);
		ASSIGNMENTS.put("&:", AssignmentType.PRE);
		ASSIGNMENTS.put("|:", AssignmentType.PRE);
		ASSIGNMENTS.put("#:", AssignmentType.PRE);
		ASSIGNMENTS.put("&&:", AssignmentType.PRE);
		ASSIGNMENTS.put("||:", AssignmentType.PRE);
		ASSIGNMENTS.put("##:", AssignmentType.PRE);
		ASSIGNMENTS.put("<<:", AssignmentType.PRE);
		ASSIGNMENTS.put(">>:", AssignmentType.PRE);
		ASSIGNMENTS.put("<<<:", AssignmentType.PRE);
		ASSIGNMENTS.put(">>>:", AssignmentType.PRE);

		ASSIGNMENTS.put(":=", AssignmentType.POST);
		ASSIGNMENTS.put(":+", AssignmentType.POST);
		ASSIGNMENTS.put(":-", AssignmentType.POST);
		ASSIGNMENTS.put(":*", AssignmentType.POST);
		ASSIGNMENTS.put(":/", AssignmentType.POST);
		ASSIGNMENTS.put(":%", AssignmentType.POST);
		ASSIGNMENTS.put(":^", AssignmentType.POST);
		ASSIGNMENTS.put(":&", AssignmentType.POST);
		ASSIGNMENTS.put(":|", AssignmentType.POST);
		ASSIGNMENTS.put(":#", AssignmentType.POST);
		ASSIGNMENTS.put(":&&", AssignmentType.POST);
		ASSIGNMENTS.put(":||", AssignmentType.POST);
		ASSIGNMENTS.put(":##", AssignmentType.POST);
		ASSIGNMENTS.put(":<<", AssignmentType.POST);
		ASSIGNMENTS.put(":>>", AssignmentType.POST);
		ASSIGNMENTS.put(":<<<", AssignmentType.POST);
		ASSIGNMENTS.put(":>>>", AssignmentType.POST);
	}

	public Token nextAssignment() {
		Token left = this.nextTernary();
		Token assignOp = this.reader.hasOperatorAfterWhitespace(ASSIGNMENTS.keySet(), Colors.OPERATOR);
		if (assignOp != null) {
			if (!left.info.assignable()) {
				left.withTooltip("Non-assignable value");
			}
			Token right = this.nextSingleExpression();
			if (!right.info.isAssignableToOrCanCast(this.environment, left.info.type(), Plicity.IMPLICIT)) {
				assignOp.withTooltip("Can't implicitly cast " + right.info.type() + " to " + left.info.type() + " for assignment.");
			}
			TokenInfo info = switch (ASSIGNMENTS.get(assignOp.getText())) {
				case VOID -> new TokenInfo(this.environment.standardTypes.void_);
				case PRE  -> left.info;
				case POST -> right.info.type().isAssignableTo(left.info.type()) ? right.info : left.info;
			};
			info = info.statement(true);
			return new Token(this.reader.input, info, left, assignOp, right);
		}
		return left;
	}

	public Token nextTernary() {
		Token left = this.nextLogical();
		Token question = this.reader.hasOperatorAfterWhitespace("?", Colors.OPERATOR);
		if (question != null) {
			if (!left.info.isAssignableToOrCanCast(this.environment, this.environment.standardTypes.boolean_, Plicity.IMPLICIT)) {
				left.withTooltip("Can't implicitly cast " + left.info.type() + " to boolean for ternary");
			}
			Token middle = this.nextSingleExpression();
			Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
			if (colon != null) {
				Token right = this.nextSingleExpression();
				int flags = BodyIfKeyword.getFlags(left, middle, right) & ~TokenInfo.FLAG_STATEMENT;
				ConstantValue constantValue = BodyIfKeyword.mergeTypes(left, middle, right);
				return new Token(this.reader.input, new TokenInfo(constantValue, flags), left, question, middle, colon, right);
			}
			else {
				return new Token(this.reader.input, TokenInfo.ERROR, left, question, middle).withTooltip("Incomplete ternary");
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
		Token left = this.nextCompare();
		Token operator = this.reader.hasOperatorAfterWhitespace(LOGICALS, Colors.OPERATOR);
		if (operator != null) {
			List<Token> tokens = new ArrayList<>();
			tokens.add(left);
			tokens.add(operator);
			if (!left.info.isAssignableToOrCanCast(this.environment, this.environment.standardTypes.boolean_, Plicity.IMPLICIT)) {
				left.withTooltip("Can't implicitly cast " + left.info.type() + " to boolean for " + operator.getText() + " operation");
			}
			do {
				Token right = this.nextCompare();
				if (!right.info.isAssignableToOrCanCast(this.environment, this.environment.standardTypes.boolean_, Plicity.IMPLICIT)) {
					right.withTooltip("Can't implicitly cast " + right.info.type() + " to boolean for " + operator.getText() + " operation");
				}
				tokens.add(right);
			}
			while (this.reader.hasOperatorAfterWhitespace(LOGICALS, Colors.OPERATOR, tokens));
			return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.boolean_), tokens);
		}
		else {
			return left;
		}
	}

	public static enum CompareCastSide {
		NONE,
		LEFT,
		RIGHT;
	}

	public static enum CompareMode {
		NONE_EQUAL(CompareCastSide.NONE, false),
		NONE_COMPARABLE(CompareCastSide.NONE, true),
		LEFT_EQUAL(CompareCastSide.LEFT, false),
		LEFT_COMPARABLE(CompareCastSide.LEFT, true),
		RIGHT_EQUAL(CompareCastSide.RIGHT, false),
		RIGHT_COMPARABLE(CompareCastSide.RIGHT, true),
		;

		public final CompareCastSide side;
		public final boolean requireComparableOrNumber;

		CompareMode(CompareCastSide side, boolean requireComparableOrNumber) {
			this.side = side;
			this.requireComparableOrNumber = requireComparableOrNumber;
		}
	}

	public static final Map<CharSequence, CompareMode> COMPARES = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		COMPARES.put("==", CompareMode.NONE_EQUAL);
		COMPARES.put("!=", CompareMode.NONE_EQUAL);
		COMPARES.put("===", CompareMode.NONE_EQUAL);
		COMPARES.put("!==", CompareMode.NONE_EQUAL);

		COMPARES.put("<", CompareMode.NONE_COMPARABLE);
		COMPARES.put(">", CompareMode.NONE_COMPARABLE);
		COMPARES.put("<=", CompareMode.NONE_COMPARABLE);
		COMPARES.put(">=", CompareMode.NONE_COMPARABLE);
		COMPARES.put("!>", CompareMode.NONE_COMPARABLE);
		COMPARES.put("!<", CompareMode.NONE_COMPARABLE);
		COMPARES.put("!>=", CompareMode.NONE_COMPARABLE);
		COMPARES.put("!<=", CompareMode.NONE_COMPARABLE);

		COMPARES.put(".==", CompareMode.LEFT_EQUAL);
		COMPARES.put(".!=", CompareMode.LEFT_EQUAL);
		COMPARES.put(".===", CompareMode.LEFT_EQUAL);
		COMPARES.put(".!==", CompareMode.LEFT_EQUAL);

		COMPARES.put(".<", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".>", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".<=", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".>=", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".!>", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".!<", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".!>=", CompareMode.LEFT_COMPARABLE);
		COMPARES.put(".!<=", CompareMode.LEFT_COMPARABLE);

		COMPARES.put("==.", CompareMode.RIGHT_EQUAL);
		COMPARES.put("!=.", CompareMode.RIGHT_EQUAL);
		COMPARES.put("===.", CompareMode.RIGHT_EQUAL);
		COMPARES.put("!==.", CompareMode.RIGHT_EQUAL);

		COMPARES.put("<.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put(">.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put("<=.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put(">=.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put("!>.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put("!<.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put("!>=.", CompareMode.RIGHT_COMPARABLE);
		COMPARES.put("!<=.", CompareMode.RIGHT_COMPARABLE);
	}

	public Token nextCompare() {
		Token left = this.nextSum();
		Token operator = this.reader.hasOperatorAfterWhitespace(COMPARES.keySet(), Colors.OPERATOR);
		if (operator != null) {
			CompareMode mode = COMPARES.get(operator.getText());
			Token right = this.nextSum();
			TokenInfo leftInfo = left.info;
			TokenInfo rightInfo = right.info;
			switch (mode.side) {
				case NONE -> {}
				case LEFT -> {
					if (leftInfo.isAssignableToOrCanCast(this.environment, rightInfo.type(), Plicity.EXPLICIT)) {
						leftInfo = rightInfo;
					}
					else {
						left.withTooltip("Can't explicitly cast " + leftInfo.type() + " to " + rightInfo.type() + " for " + operator.getText() + " operation");
					}
				}
				case RIGHT -> {
					if (rightInfo.isAssignableToOrCanCast(this.environment, leftInfo.type(), Plicity.EXPLICIT)) {
						rightInfo = leftInfo;
					}
					else {
						right.withTooltip("Can't explicitly cast " + rightInfo.type() + " to " + leftInfo.type() + " for " + operator.getText() + " operation");
					}
				}
			}
			RawTypeModel requiredType = mode.requireComparableOrNumber ? this.environment.standardTypes.primitiveComparable : this.environment.standardTypes.value;
			if (
				! leftInfo.isAssignableToOrCanCast(this.environment, requiredType, Plicity.IMPLICIT) ||
				!rightInfo.isAssignableToOrCanCast(this.environment, requiredType, Plicity.IMPLICIT)
			) {
				operator.withTooltip("Can't compare " + leftInfo.type() + " to " + rightInfo.type());
			}
			return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.boolean_), left, operator, right);
		}
		else {
			return left;
		}
	}

	public static final Map<CharSequence, BinaryConstantFolder> SUMS = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		SUMS.put("+", ArithmeticConstantFolder.ADD);
		SUMS.put("-", ArithmeticConstantFolder.SUB);
		SUMS.put("&",    BitwiseConstantFolder.AND);
		SUMS.put("|",    BitwiseConstantFolder.OR );
		SUMS.put("#",    BitwiseConstantFolder.XOR);
	}

	public Token nextSum() {
		Token left = this.nextProduct();
		while (true) {
			Token operator = this.reader.hasOperatorAfterWhitespace(SUMS.keySet(), Colors.OPERATOR);
			if (operator == null) break;
			Token right = this.nextProduct();
			BinaryConstantFolder folder = SUMS.get(operator.getText());
			TokenInfo newInfo = folder.fold(this.environment, left, right);
			left = new Token(this.reader.input, newInfo, left, operator, right);
		}
		return left;
	}

	public static final Map<CharSequence, BinaryConstantFolder> PRODUCTS = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		PRODUCTS.put("*", ArithmeticConstantFolder.MUL);
		PRODUCTS.put("/", ArithmeticConstantFolder.DIV);
		PRODUCTS.put("%", ArithmeticConstantFolder.MOD);
		PRODUCTS.put("<<", SignedShiftConstantFolder.LEFT);
		PRODUCTS.put(">>", SignedShiftConstantFolder.RIGHT);
		PRODUCTS.put("<<<", UnsignedShiftConstantFolder.LEFT);
		PRODUCTS.put(">>>", UnsignedShiftConstantFolder.RIGHT);
	}

	public Token nextProduct() {
		Token left = this.nextExponent();
		while (true) {
			Token operator = this.reader.hasOperatorAfterWhitespace(PRODUCTS.keySet(), Colors.OPERATOR);
			if (operator == null) break;
			Token right = this.nextExponent();
			BinaryConstantFolder folder = PRODUCTS.get(operator.getText());
			TokenInfo newInfo = folder.fold(this.environment, left, right);
			left = new Token(this.reader.input, newInfo, left, operator, right);
		}
		return left;
	}

	public Token nextExponent() {
		Token left = this.nextElvis();
		Token power = this.reader.hasOperatorAfterWhitespace("^", Colors.OPERATOR);
		if (power != null) {
			Token right = this.nextExponent();
			TokenInfo info = PowerConstantFolder.INSTANCE.fold(this.environment, left, right);
			return new Token(this.reader.input, info, left, power, right);
		}
		else {
			return left;
		}
	}

	public Token nextElvis() {
		Token left = this.nextPrefix();
		Token elvis = this.reader.hasOperatorAfterWhitespace("?:", Colors.OPERATOR);
		if (elvis != null) {
			Token right = this.nextElvis();
			TokenInfo info = new TokenInfo(
				RawTypeModel.commonAncestor(left.info, right.info),
				(left.info.flags() | right.info.flags()) & (TokenInfo.FLAG_JUMPS | TokenInfo.FLAG_GENERIC)
			);
			return new Token(this.reader.input, info, left, elvis, right);
		}
		else {
			return left;
		}
	}

	public static final Map<CharSequence, UnaryConstantFolder> PREFIXES = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		PREFIXES.put("+", UnaryPlusConstantFolder.INSTANCE);
		PREFIXES.put("-", UnaryMinusConstantFolder.INSTANCE);
		PREFIXES.put("~", BitwiseNegateConstantFolder.INSTANCE);
		PREFIXES.put("!", BooleanNotConstantFolder.INSTANCE);
		PREFIXES.put("++", VoidUpdateConstantFolder.INSTANCE);
		PREFIXES.put(":++", PostUpdateConstantFolder.INSTANCE);
		PREFIXES.put("++:", PreUpdateConstantFolder.INSTANCE);
		PREFIXES.put("--", VoidUpdateConstantFolder.INSTANCE);
		PREFIXES.put(":--", PostUpdateConstantFolder.INSTANCE);
		PREFIXES.put("--:", PreUpdateConstantFolder.INSTANCE);
	}

	public Token nextPrefix() {
		Token prefix = this.reader.hasOperatorAfterWhitespace(PREFIXES.keySet(), Colors.OPERATOR);
		if (prefix != null) {
			Token member = this.nextMember();
			TokenInfo info = PREFIXES.get(prefix.getText()).fold(this.environment, member);
			return new Token(this.reader.input, info, prefix, member);
		}
		else {
			return this.nextMember();
		}
	}

	public static final int
		MEMBER_FLAG_SETTER   = 1 << 0,
		MEMBER_FLAG_NULLABLE = 1 << 1,
		MEMBER_FLAG_RECEIVER = 1 << 2;

	//intellij seems to be missing Object2ByteOpenCustomHashMap for some reason.
	//maybe their version of fastutil is outdated?
	public static final Object2ObjectMap<CharSequence, Byte> MEMBERS = new Object2ObjectOpenCustomHashMap<>(Util.CHAR_SEQUENCE_STRATEGY);
	static {
		MEMBERS.put(".",    (byte)(0));
		MEMBERS.put(".=",   (byte)(MEMBER_FLAG_SETTER  ));
		MEMBERS.put(".?",   (byte)(MEMBER_FLAG_NULLABLE));
		MEMBERS.put(".$",   (byte)(MEMBER_FLAG_RECEIVER));
		MEMBERS.put(".=?",  (byte)(MEMBER_FLAG_SETTER   | MEMBER_FLAG_NULLABLE));
		MEMBERS.put(".=$",  (byte)(MEMBER_FLAG_SETTER   | MEMBER_FLAG_RECEIVER));
		MEMBERS.put(".?=",  (byte)(MEMBER_FLAG_NULLABLE | MEMBER_FLAG_SETTER  ));
		MEMBERS.put(".?$",  (byte)(MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER));
		MEMBERS.put(".$=",  (byte)(MEMBER_FLAG_RECEIVER | MEMBER_FLAG_SETTER  ));
		MEMBERS.put(".$?",  (byte)(MEMBER_FLAG_RECEIVER | MEMBER_FLAG_NULLABLE));
		MEMBERS.put(".=?$", (byte)(MEMBER_FLAG_SETTER   | MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER));
		MEMBERS.put(".=$?", (byte)(MEMBER_FLAG_SETTER   | MEMBER_FLAG_RECEIVER | MEMBER_FLAG_NULLABLE));
		MEMBERS.put(".?=$", (byte)(MEMBER_FLAG_NULLABLE | MEMBER_FLAG_SETTER   | MEMBER_FLAG_RECEIVER));
		MEMBERS.put(".?$=", (byte)(MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER | MEMBER_FLAG_SETTER  ));
		MEMBERS.put(".$=?", (byte)(MEMBER_FLAG_RECEIVER | MEMBER_FLAG_SETTER   | MEMBER_FLAG_NULLABLE));
		MEMBERS.put(".$?=", (byte)(MEMBER_FLAG_RECEIVER | MEMBER_FLAG_NULLABLE | MEMBER_FLAG_SETTER  ));
	}

	public Token nextMember() {
		Token left = this.nextTerm();
		while (true) {
			Token dot = this.reader.hasOperatorAfterWhitespace(MEMBERS.keySet(), Colors.OPERATOR);
			if (dot != null) {
				Token memberName = this.reader.nextIdentifierAfterWhitespace();
				if (memberName == null) memberName = new Token(this.reader.input, this.reader.cursor, this.reader.cursor, TokenInfo.UNKNOWN);
				String memberNameText = memberName.getIdentifierText().toString();
				MemberKeywordData keyword = this.environment.getMemberKeyword(left.info.type(), memberNameText);
				if (keyword != null) {
					Token result = keyword.handle(this, left, dot, memberName);
					if (result != null) left = result;
				}
				else {
					int flags = MEMBERS.get(dot.getText());
					//can't do this in rust, heh.
					left = switch (flags) {
						case
							0, //.
							MEMBER_FLAG_NULLABLE, //.?
							MEMBER_FLAG_RECEIVER, //.$
							MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER //.?$
						-> {
							GroupStructure<MultiStructure<Token>> arguments = new GroupParser<>(true, new MultiParser<>(true, StructureParser.NULLABLE_SCRIPT, StructureParser.operator(","))).parse(this);
							if (arguments != null) { //object.method(args)
								MethodData method = this.environment.getInstanceMethod(left.info.type(), memberNameText, arguments.content() != null ? arguments.content().values() : Collections.emptyList());
								if (method != null) yield Token.builder().with(left).with(dot).with(method.applyColor(memberName).withInfo(TokenInfo.NON_VALUE)).withAll(arguments).build(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? left.info : method.returnType);
								else yield Token.builder().with(left).with(dot).with(memberName.error("Unknown method or incorrect arguments")).withAll(arguments).build(this.reader.input, TokenInfo.ERROR);
							}
							else { //object.field
								FieldData field = this.environment.getInstanceField(left.info.type(), memberNameText);
								if (field != null) yield new Token(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? left.info : field.info, left, dot, memberName.withColor(Colors.INSTANCE_FIELD).withInfo(TokenInfo.NON_VALUE));
								else yield new Token(this.reader.input, TokenInfo.ERROR, left, dot, memberName.error("Unknown field"));
							}
						}
						case
							MEMBER_FLAG_SETTER, //.=
							MEMBER_FLAG_SETTER | MEMBER_FLAG_NULLABLE, //.=?
							MEMBER_FLAG_SETTER | MEMBER_FLAG_RECEIVER, //.=$
							MEMBER_FLAG_SETTER | MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER //.=$?
						-> {
							GroupStructure<Token> arguments = new GroupParser<>(true, StructureParser.SCRIPT).parse(this);
							FieldData field = this.environment.getInstanceField(left.info.type(), memberNameText);
							if (field != null) {
								if (!field.info.assignable()) memberName.withTooltip("This field is not assignable");
								if (arguments != null) yield Token.builder().with(left).with(dot).with(field.applyColor(memberName).withInfo(TokenInfo.NON_VALUE)).withAll(arguments).build(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? left.info : new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
								else yield new Token(this.reader.input, field.info, left, dot, memberName.withColor(Colors.INSTANCE_FIELD).withInfo(field.info), this.error("Expected '(' for field setter syntax"));
							}
							else {
								if (arguments != null) yield Token.builder().with(left).with(dot).with(memberName.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip("Unknown field")).withAll(arguments).build(this.reader.input, TokenInfo.ERROR);
								else yield new Token(this.reader.input, TokenInfo.ERROR, left, dot, memberName.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip("Unknown field"), this.error("Expected '(' for field setter syntax"));
							}
						}
						default -> {
							throw new IllegalStateException("Unhandled flags: 0b" + Integer.toBinaryString(flags));
						}
					};
				}
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
				if (requireTerm) identifier = this.nextIdentifier(identifier);
				yield identifier;
			}
			case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
				yield this.reader.nextNumber(this.environment.standardTypes);
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
					yield new Token(this.reader.input, body.info, open, body, close);
				}
				else {
					yield open;
				}
			}
			case ')', '[', ']', '{', '}' -> {
				this.reader.skip();
				if (requireTerm) {
					yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, TokenInfo.ERROR).withColor(Colors.ERROR).withTooltip("Expected identifier, number, or string");
				}
				else {
					yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, TokenInfo.ERROR).withColor(Colors.GROUP);
				}
			}
			default -> {
				this.reader.skip();
				yield new Token(this.reader.input, this.reader.cursor - 1, this.reader.cursor, TokenInfo.ERROR).withColor(Colors.ERROR).withTooltip("Expected identifier, number, or string");
			}
		};
	}

	public Token nextIdentifier(Token word) {
		String wordText = word.getIdentifierText().toString();
		KeywordData keyword = this.environment.getKeyword(wordText);
		if (keyword != null) {
			Token result = keyword.handle(this, word);
			if (result != null) return result;
		}
		TypeData type = this.environment.getType(wordText);
		if (type != null) {
			type.applyColor(word).withInfo(type.info);
			{ //multi-declaration: int*(x = 1, y = 2, z = 3)
				Token star = this.reader.hasOperatorAfterWhitespace("*", Colors.OPERATOR);
				if (star != null) {
					return this.finishMultiDeclaration(word, star);
				}
			}
			{ //nullable cast: Block?('modid:example')
				Token question = this.reader.hasOperatorAfterWhitespace("?", Colors.OPERATOR);
				if (question != null) {
					GroupStructure<Token> valueToCast = new GroupParser<>(true, StructureParser.SCRIPT).parse(this);
					if (valueToCast != null) {
						return Token.builder().with(word).with(question).withAll(valueToCast).build(this.reader.input, type.info);
					}
					else {
						return new Token(this.reader.input, type.info, word, question, this.error("Expected parenthesized script"));
					}
				}
			}
			{ //non-null cast: Block('modid:example')
				GroupStructure<Token> valueToCast = new GroupParser<>(true, StructureParser.SCRIPT).parse(this);
				if (valueToCast != null) {
					return Token.builder().with(word).withAll(valueToCast).build(this.reader.input, type.info);
				}
			}
			{ //static field or method: Block.name or Block.new(...)
				Token dot = this.reader.hasOperatorAfterWhitespace(MEMBERS.keySet(), Colors.OPERATOR);
				if (dot != null) {
					return this.finishStaticMember(word, dot);
				}
			}
			{ //more stuff
				int nextWordRevert = this.reader.cursor;
				Token nextWord = this.reader.nextIdentifierAfterWhitespace();
				if (nextWord != null) {
					TypeData typeData = this.environment.getType(nextWord.getIdentifierText().toString());
					if (typeData != null) { //extension method declaration: int Block.getWhatever(args: body)
						typeData.applyColor(nextWord).withInfo(typeData.info);
						return this.finishMethodDeclaration(word, type, nextWord, typeData);
					}
					else {
						{ //function declaration: Block get(args: body)
							Token open = this.tryOpenGroup();
							if (open != null) {
								return this.finishFunctionDeclaration(word, nextWord, type, open);
							}
						}
						{ //declaration: Block example = 'modid:example'
							Token assign = this.reader.hasOperatorAfterWhitespace(DECLARE_ASSIGNMENTS, Colors.OPERATOR);
							if (assign != null) {
								nextWord.withColor(Colors.LOCAL).withInfo(type.info);
								this.environment.addUserLocal(nextWord.getIdentifierText().toString(), type.info.assignable(true));
								Token value = this.nextRhsOfAssignment(type.info);
								if (!value.info.isAssignableToOrCanCast(this.environment, type.info.type(), Plicity.IMPLICIT)) {
									assign.withTooltip("Can't implicitly cast " + value.info.type() + " to " + word.info.type() + " for assignment");
								}
								return new Token(
									this.reader.input,
									switch (assign.getText().length()) {
										case 1 -> new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT);
										case 2 -> type.info.statement(true);
										default -> null;
									},
									word,
									nextWord,
									assign,
									value
								);
							}
							else {
								this.reader.cursor = nextWordRevert;
							}
						}
					}
				}
			}
			return word;
		}
		else {
			GroupStructure<MultiStructure<Token>> arguments = new GroupParser<>(true, new MultiParser<>(true, StructureParser.NULLABLE_SCRIPT, StructureParser.operator(","))).parse(this);
			if (arguments != null) { //function call: someFunction(args)
				FunctionData function = this.environment.getFunction(word.getIdentifierText().toString(), arguments.content() != null ? arguments.content().values() : Collections.emptyList());
				if (function != null) return Token.builder().with(function.applyColor(word).withInfo(function.info)).withAll(arguments).build(this.reader.input, function.info);
				else return Token.builder().with(word.withColor(Colors.ERROR).withTooltip("Unknown function")).withAll(arguments).build(this.reader.input, TokenInfo.ERROR);
			}
			else { //variable usage: someVariable
				VariableData variable = this.environment.getVariable(word.getIdentifierText().toString());
				if (variable != null) return variable.applyColor(word).withInfo(variable.info);
				else return word.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip("Unknown variable");
			}
		}
	}

	public Token finishStaticMember(Token type, Token dot) {
		Token memberName = this.reader.nextIdentifierAfterWhitespace();
		if (memberName == null) memberName = this.error("Expected static member name");
		int flags = MEMBERS.get(dot.getText());
		String memberNameText = memberName.getIdentifierText().toString();
		return switch (flags) {
			case
				0, //.
				MEMBER_FLAG_NULLABLE, //.?
				MEMBER_FLAG_RECEIVER, //.$
				MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER //.?$
			-> {
				GroupStructure<MultiStructure<Token>> arguments = new GroupParser<>(true, new MultiParser<>(true, StructureParser.NULLABLE_SCRIPT, StructureParser.operator(","))).parse(this);
				if (arguments != null) { //Type.method(args)
					MethodData method = this.environment.getStaticMethod(type.info.type(), memberNameText, arguments.content() != null ? arguments.content().values() : Collections.emptyList());
					if (method != null) yield Token.builder().with(type).with(dot).with(method.applyColor(memberName).withInfo(TokenInfo.NON_VALUE)).withAll(arguments).build(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? type.info : method.returnType);
					else yield Token.builder().with(type).with(dot).with(memberName.error("Unknown static method or incorrect arguments")).withAll(arguments).build(this.reader.input, TokenInfo.ERROR);
				}
				else { //Type.field
					FieldData field = this.environment.getStaticField(type.info.type(), memberNameText);
					if (field != null) yield new Token(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? type.info : field.info, type, dot, memberName.withColor(Colors.INSTANCE_FIELD).withInfo(TokenInfo.NON_VALUE));
					else yield new Token(this.reader.input, TokenInfo.ERROR, type, dot, memberName.error("Unknown static field"));
				}
			}
			case
				MEMBER_FLAG_SETTER, //.=
				MEMBER_FLAG_SETTER | MEMBER_FLAG_NULLABLE, //.=?
				MEMBER_FLAG_SETTER | MEMBER_FLAG_RECEIVER, //.=$
				MEMBER_FLAG_SETTER | MEMBER_FLAG_NULLABLE | MEMBER_FLAG_RECEIVER //.=$?
			-> {
				GroupStructure<Token> arguments = new GroupParser<>(true, StructureParser.SCRIPT).parse(this);
				FieldData field = this.environment.getStaticField(type.info.type(), memberNameText);
				if (field != null) {
					if (!field.info.assignable()) memberName.withTooltip("This field is not assignable");
					if (arguments != null) {
						if (arguments.content() != null && !arguments.content().info.isAssignableToOrCanCast(this.environment, field.info.type(), Plicity.IMPLICIT)) {
							dot.withTooltip("Can't implicitly cast " + arguments.content().info.type() + " to " + field.info + " for assignment");
						}
						yield Token.builder().with(type).with(dot).with(field.applyColor(memberName).withInfo(TokenInfo.NON_VALUE)).withAll(arguments).build(this.reader.input, (flags & MEMBER_FLAG_RECEIVER) != 0 ? type.info : new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
					}
					else {
						yield new Token(this.reader.input, field.info, type, dot, memberName.withColor(Colors.STATIC_FIELD).withInfo(TokenInfo.NON_VALUE), this.error("Expected '(' for field setter syntax"));
					}
				}
				else {
					if (arguments != null) yield Token.builder().with(type).with(dot).with(memberName.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip("Unknown field")).withAll(arguments).build(this.reader.input, TokenInfo.ERROR);
					else yield new Token(this.reader.input, TokenInfo.ERROR, type, dot, memberName.withColor(Colors.ERROR).withInfo(TokenInfo.ERROR).withTooltip("Unknown field"), this.error("Expected '(' for field setter syntax"));
				}
			}
			default -> {
				throw new IllegalStateException("Unhandled flags: 0b" + Integer.toBinaryString(flags));
			}
		};
	}

	public Token finishMultiDeclaration(Token type, Token star) {
		GroupStructure<MultiStructure<NameEqualsValue>> variables = new GroupParser<>(
			false,
			new MultiParser<>(
				false,
				new NameEqualsValueParser() {

					public NameEqualsValue previous;

					@Override
					public NameEqualsValue parse(ExpressionParser parser) {
						NameEqualsValue declaration = super.parse(parser);
						if (declaration != null) {
							if (type.info.type() != RawTypeModel.NON_VALUE && declaration.value() != null && !declaration.value().info.isAssignableToOrCanCast(parser.environment, type.info.type(), Plicity.IMPLICIT)) {
								declaration.value().withTooltip("Can't implicitly cast " + declaration.value().info.type() + " to " + type.info.type());
							}
							declaration.name().withColor(Colors.LOCAL);
							TokenInfo newVariableType = type.info.type() != RawTypeModel.NON_VALUE ? type.info : declaration.value() != null ? declaration.value().info : null;
							if (newVariableType != null) {
								declaration.name().withInfo(newVariableType);
								parser.environment.addUserLocal(declaration.name().withColor(Colors.LOCAL).getIdentifierText().toString(), newVariableType.assignable(true));
								if (declaration.value() != null && !declaration.value().info.isAssignableToOrCanCast(ExpressionParser.this.environment, newVariableType.type(), Plicity.IMPLICIT)) {
									declaration.equals().withTooltip("Can't implicitly cast " + declaration.value().info.type() + " to " + newVariableType + " for assignment");
								}
							}
							if (this.previous != null && this.previous.equals() != null) {
								this.previous.equals().withColor(Colors.ERROR).withTooltip("Only the last declaration can use ':='");
								this.previous = null;
							}
							if (declaration.equals() != null && declaration.equals().range.getLength() == 2) {
								this.previous = declaration;
							}
						}
						return declaration;
					}
				}
				.withType(type.info.type() != RawTypeModel.NON_VALUE ? type.info : null),
				StructureParser.operators(COMMAS)
			)
		)
		.parse(this);
		if (variables == null) {
			return new Token(this.reader.input, TokenInfo.ERROR, type, star, this.error("Expected parenthesized declarations"));
		}
		Token result = Token.builder().with(type).with(star).withAll(variables).build(this.reader.input, new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT));
		if (variables.content() != null) {
			List<NameEqualsValue> values = variables.content().values();
			if (!values.isEmpty()) {
				NameEqualsValue last = values.getLast();
				if (last.equals() != null && last.equals().range.getLength() == 2) {
					result.info = (type.info.type() != RawTypeModel.NON_VALUE ? type.info : last.value() != null ? last.value().info : new TokenInfo(this.environment.standardTypes.void_)).statement(true);
				}
			}
		}
		return result;
	}

	public Token finishMethodDeclaration(Token returnTypeToken, TypeData returnType, Token extendedTypeToken, TypeData extendedType) {
		extendedType.applyColor(extendedTypeToken);
		Token dot = this.reader.hasOperatorAfterWhitespace(".", Colors.OPERATOR);
		if (dot == null) dot = this.error("Expected '.' for extension method declaration");
		Token actualName = this.reader.nextIdentifierAfterWhitespace();
		if (actualName == null) actualName = this.error("Expected method name");
		else actualName.withColor(Colors.INSTANCE_METHOD).withInfo(TokenInfo.NON_VALUE);
		Token open = this.tryOpenGroup();
		if (open == null) return new Token(this.reader.input, TokenInfo.ERROR, returnTypeToken, extendedTypeToken, dot, actualName, this.error("Expected '('"));
		this.environment.addVariable(new VariableData("this", Colors.KEYWORD, extendedTypeToken.info));
		MultiStructure<ParameterStructure> parameterStructures = new MultiParser<>(true, new ParameterParser(), StructureParser.operator(",")).parse(this);
		ParameterModel[] parameterModelArray = this.extractParameters(parameterStructures);
		MethodData method = new MethodData(actualName.getIdentifierText().toString(), Colors.INSTANCE_METHOD, extendedType.info.type(), returnType.info, parameterModelArray);
		this.environment.addInstanceMethod(method); //first add for recursive purposes.
		Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
		if (colon == null) colon = this.error("Expected ':'");
		Token body = this.nextNullableScript();
		if (body == null) body = this.error("Expected extension method body");
		Token close = this.closeGroup();
		this.environment.addInstanceMethod(method); //then add again because the scope is popped now.
		return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT), returnTypeToken, extendedTypeToken, dot, actualName, open, parameterStructures != null ? parameterStructures.group() : null, colon, body, close);
	}

	public Token finishFunctionDeclaration(Token returnTypeToken, Token functionNameToken, TypeData returnTypeData, Token open) {
		functionNameToken.withColor(Colors.FUNCTION).withInfo(TokenInfo.NON_VALUE);
		MultiStructure<ParameterStructure> parameterStructures = new MultiParser<>(true, new ParameterParser(), StructureParser.operator(",")).parse(this);
		ParameterModel[] parameterModelArray = this.extractParameters(parameterStructures);
		this.environment.addUserFunction(functionNameToken.getIdentifierText().toString(), returnTypeData.info, parameterModelArray); //first add for recursive purposes.
		Token colon = this.reader.hasOperatorAfterWhitespace(":", Colors.OPERATOR);
		if (colon == null) colon = this.error("Expected ':'");
		Token body = this.nextNullableScript();
		if (body == null) body = this.error("Expected function body");
		Token close = this.closeGroup();
		this.environment.addUserFunction(functionNameToken.getIdentifierText().toString(), returnTypeData.info, parameterModelArray); //then add again because the scope is popped now.
		return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.void_, TokenInfo.FLAG_STATEMENT), returnTypeToken, functionNameToken, open, parameterStructures != null ? parameterStructures.group() : null, colon, body, close);
	}

	public ParameterModel[] extractParameters(MultiStructure<ParameterStructure> parameterStructures) {
		List<ParameterModel> parameterModels = new ArrayList<>();
		if (parameterStructures != null) {
			for (ParameterStructure parameterStructure : parameterStructures.values()) {
				Token paramTypeToken = parameterStructure.type();
				for (Token nameToken : parameterStructure.names()) {
					nameToken.withInfo(paramTypeToken.info);
					String nameString = nameToken.withColor(Colors.PARAMETER).getIdentifierText().toString();
					this.environment.addUserParameter(nameString, paramTypeToken.info.assignable(true));
					parameterModels.add(new ParameterModel(nameString, paramTypeToken.info.type(), false));
				}
			}
		}
		return parameterModels.toArray(new ParameterModel[parameterModels.size()]);
	}

	public static final Set<CharSequence> DECLARE_ASSIGNMENTS = Util.charSequenceSet("=", ":=");
	public static final Set<CharSequence> COMMAS = Util.charSequenceSet(",", ",,");

	@SuppressWarnings("deprecation")
	public Token nextString(int quote) {
		int start = this.reader.cursor;
		this.reader.skip();
		List<Token> parts = new ArrayList<>(4);
		loop:
		while (true) {
			int read = this.reader.peek();
			if (read == -1) {
				Token token = new Token(this.reader.input, start, this.reader.cursor, new TokenInfo(this.environment.standardTypes.string)).withColor(Colors.STRING);
				if (parts.isEmpty()) return token;
				parts.add(token);
				return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.string), parts);
			}
			else if (read == quote) {
				this.reader.skip();
				Token token = new Token(this.reader.input, start, this.reader.cursor, new TokenInfo(this.environment.standardTypes.string)).withColor(Colors.STRING);
				if (parts.isEmpty()) return token;
				parts.add(token);
				return new Token(this.reader.input, new TokenInfo(this.environment.standardTypes.string), parts);
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
						parts.add(new Token(this.reader.input, start, operatorStart, new TokenInfo(this.environment.standardTypes.string)).withColor(Colors.STRING));
						parts.add(new Token(this.reader.input, operatorStart, this.reader.cursor, TokenInfo.NON_VALUE).withColor(Colors.ERROR));
						start = this.reader.cursor;
						continue loop;
					}
				}
				parts.add(new Token(this.reader.input, start, operatorStart, new TokenInfo(this.environment.standardTypes.string)).withColor(Colors.STRING));
				parts.add(new Token(this.reader.input, operatorStart, this.reader.cursor, TokenInfo.NON_VALUE).withColor(Colors.OPERATOR));
				Token interpolation = member ? this.nextElvis() : this.nextTerm();
				parts.add(interpolation);
				this.reader.rollback(start = interpolation.range.getEndOffset());
			}
			else if (read == '\n' || read == '\r') {
				if (this.reader.splitNewLinesInMultiLineTokens()) {
					if (this.reader.cursor > start) {
						parts.add(new Token(this.reader.input, start, this.reader.cursor, new TokenInfo(this.environment.standardTypes.string)).withColor(Colors.STRING));
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

	public Token nextRhsOfAssignment(TokenInfo type) {
		if (type != null) {
			Token new_ = this.reader.hasIdentifierAfterWhitespace("new", Colors.KEYWORD);
			if (new_ != null) {
				return this.nextNew(type, new_);
			}
		}
		return this.nextSingleExpression();
	}

	public Token nextNew(TokenInfo type, Token new_) {
		GroupStructure<MultiStructure<Token>> arguments = new GroupParser<>(true, new MultiParser<>(true, StructureParser.NULLABLE_SCRIPT, StructureParser.operator(","))).parse(this);
		if (arguments == null) return new Token(this.reader.input, type, new_, this.error("Expected '('"));
		MethodData method = this.environment.getStaticMethod(type.type(), "new", arguments.content() != null ? arguments.content().values() : Collections.emptyList());
		if (method != null) {
			return new Token(this.reader.input, method.returnType, method.applyColor(new_).withInfo(TokenInfo.NON_VALUE), arguments.group());
		}
		else {
			return new Token(this.reader.input, type, new_.error("Can't find matching constructor for type " + type.type()), arguments.group());
		}
	}

	public @Nullable Token tryOpenGroup() {
		Token open = this.reader.hasAfterWhitespace('(', Colors.GROUP);
		if (open != null) this.environment.push();
		return open;
	}

	public @Nullable Token tryOpenGroup(IntPredicate openers) {
		Token open = this.reader.hasAfterWhitespace(openers, Colors.GROUP);
		if (open != null) this.environment.push();
		return open;
	}

	public @Nullable Token tryCloseGroup() {
		Token close = this.reader.hasAfterWhitespace(')', Colors.GROUP);
		if (close != null) this.environment.pop();
		return close;
	}

	public Token closeGroup() {
		this.environment.pop();
		return this.closeUnscopedGroup();
	}

	public Token closeGroup(IntPredicate closers) {
		this.environment.pop();
		return this.closeUnscopedGroup(closers);
	}

	public Token closeUnscopedGroup() {
		return this.closeUnscopedGroup((int c) -> c == ')');
	}

	public Token closeUnscopedGroup(IntPredicate closers) {
		Token close = this.reader.hasAfterWhitespace(closers, Colors.GROUP);
		if (close != null) {
			return close;
		}
		Token abortion = this.abort(null, "Expected ')'");
		close = this.reader.hasAfterWhitespace(closers, Colors.GROUP);
		if (close != null) {
			return new Token(this.reader.input, TokenInfo.ERROR, abortion, close);
		}
		else {
			return abortion;
		}
	}

	public Token error(String tooltip) {
		return new Token(this.reader.input, this.reader.cursor, this.reader.cursor, TokenInfo.ERROR).withColor(Colors.ERROR).withTooltip(tooltip);
	}

	public Token orError(Token token, String tooltip) {
		return token != null ? token : this.error(tooltip);
	}

	public static final Set<CharSequence> COLONS = Util.charSequenceSet(":");

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
			if (anything.info.type() == RawTypeModel.UNKNOWN) {
				anything.info = TokenInfo.NON_VALUE;
			}
			tokens.add(anything);
		}
		return new Token(this.reader.input, TokenInfo.ERROR, tokens);
	}
}