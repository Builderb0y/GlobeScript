package builderb0y.globescript;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.globescript.ConstantValue.NonConstantValue;
import builderb0y.globescript.datadriven.EnvironmentModel.CallableEligibility;
import builderb0y.globescript.datadriven.EnvironmentModel.CastData;
import builderb0y.globescript.datadriven.Plicity;
import builderb0y.globescript.datadriven.RawTypeModel;

public record TokenInfo(ConstantValue constant, byte flags) {

	public static final int
		FLAG_GENERIC    = 1 << 0,
		FLAG_STATEMENT  = 1 << 1,
		FLAG_JUMPS      = 1 << 2,
		FLAG_ASSIGNABLE = 1 << 3;

	public static final TokenInfo
		NON_VALUE = new TokenInfo(RawTypeModel.NON_VALUE, 0),
		UNKNOWN = new TokenInfo(RawTypeModel.UNKNOWN, 0),
		ERROR = new TokenInfo(RawTypeModel.ERROR, FLAG_STATEMENT);

	public TokenInfo(ConstantValue constant, int flags) {
		this(constant, (byte)(flags));
	}

	public TokenInfo(ConstantValue constant) {
		this(constant, 0);
	}

	public TokenInfo(RawTypeModel type) {
		this(new NonConstantValue(type), 0);
	}

	public TokenInfo(RawTypeModel type, int flags) {
		this(new NonConstantValue(type), flags);
	}

	public RawTypeModel type() {
		return this.constant().type();
	}

	public CallableEligibility getEligibility(ScriptEnvironment environment, RawTypeModel type, Plicity plicity) {
		if (this.type().isAssignableTo(type)) return CallableEligibility.EXACT_MATCH;
		return this.canCastTo(environment, type, plicity) ? CallableEligibility.REQUIRES_IMPLICIT_CAST : CallableEligibility.INVALID;
	}

	public boolean isAssignableToOrCanCast(ScriptEnvironment environment, RawTypeModel type, Plicity plicity) {
		return this.type().isAssignableTo(type) || this.canCastTo(environment, type, plicity);
	}

	public boolean canCastTo(ScriptEnvironment environment, RawTypeModel to, Plicity plicity) {
		if (to == environment.standardTypes.void_) {
			return this.statement();
		}
		if (this.jumps()) {
			return true;
		}
		if (this.generic()) {
			plicity = Plicity.EXPLICIT;
		}
		List<CastData> casts = environment.getCasts(this.type(), to);
		if (!casts.isEmpty()) {
			if (plicity == Plicity.EXPLICIT) return true;
			for (CastData cast : casts) {
				if (cast.plicity == Plicity.IMPLICIT) return true;
			}
		}
		if (
			this.type().isAssignableTo(environment.standardTypes.object) &&
			to.isAssignableTo(environment.standardTypes.object) &&
			plicity == Plicity.EXPLICIT && (
				to.isAssignableTo(this.type()) ||
				(to.isInterface() && !this.type().isFinal())
			)
		) {
			return true;
		}
		return false;
	}

	public @Nullable TokenInfo tryCastIfNotAssignable(ScriptEnvironment environment, RawTypeModel type, Plicity plicity) {
		return this.type().isAssignableTo(type) ? this : this.tryCastTo(environment, type, plicity);
	}

	public @Nullable TokenInfo tryCastTo(ScriptEnvironment environment, RawTypeModel type, Plicity plicity) {
		if (type == environment.standardTypes.void_) {
			return this.statement() ? new TokenInfo(type, this.flags & (FLAG_STATEMENT | FLAG_JUMPS)) : null;
		}
		if (this.jumps()) {
			return this;
		}
		if (this.generic()) {
			plicity = Plicity.EXPLICIT;
		}
		List<CastData> casts = environment.getCasts(this.type(), type);
		for (CastData cast : casts) {
			if (plicity == Plicity.EXPLICIT || cast.plicity == Plicity.IMPLICIT) {
				return new TokenInfo(cast.to, this.flags);
			}
		}
		return null;
	}

	public TokenInfo flags(int flags) {
		return this.flags == flags ? this : new TokenInfo(this.constant(), flags);
	}

	public boolean generic() {
		return (this.flags & FLAG_GENERIC) != 0;
	}

	public TokenInfo generic(boolean generic) {
		return this.flags(generic ? this.flags | FLAG_GENERIC : this.flags & ~FLAG_GENERIC);
	}

	public boolean statement() {
		return (this.flags & FLAG_STATEMENT) != 0;
	}

	public TokenInfo statement(boolean statement) {
		return this.flags(statement ? this.flags | FLAG_STATEMENT : this.flags & ~FLAG_STATEMENT);
	}

	public boolean jumps() {
		return (this.flags & FLAG_JUMPS) != 0;
	}

	public TokenInfo jumps(boolean jumps) {
		return this.flags(jumps ? this.flags | FLAG_JUMPS : this.flags & ~FLAG_JUMPS);
	}

	public boolean assignable() {
		return (this.flags & FLAG_ASSIGNABLE) != 0;
	}

	public TokenInfo assignable(boolean assignable) {
		return this.flags(assignable ? this.flags | FLAG_ASSIGNABLE : this.flags & ~FLAG_ASSIGNABLE);
	}
}