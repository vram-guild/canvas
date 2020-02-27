package grondag.canvas.material;

import net.minecraft.util.math.MathHelper;

import grondag.fermion.varia.Useful;

public class MaterialContext {
	public final MaterialTarget target;
	public final MaterialSubject subject;
	public final int index;

	private MaterialContext(MaterialTarget target, MaterialSubject subject, int index) {
		this.target = target;
		this.subject = subject;
		this.index = index;
	}

	private static final int MAX_TARGETS = MathHelper.smallestEncompassingPowerOfTwo(MaterialTarget.values().length);
	private static final int MAX_SUBJECTS = MathHelper.smallestEncompassingPowerOfTwo(MaterialSubject.values().length);
	private static final int SHIFT = Useful.bitLength(MAX_SUBJECTS);
	public static final int MAX_CONTEXTS = MAX_TARGETS * MAX_SUBJECTS;

	private static final MaterialContext[] VALUES = new MaterialContext[MAX_CONTEXTS];

	static {
		for (final MaterialTarget target : MaterialTarget.values()) {
			for (final MaterialSubject subject : MaterialSubject.values()) {
				final int index = index(target, subject);
				VALUES[index] = new MaterialContext(target, subject, index);
			}
		}
	}
	private static int index(MaterialTarget target, MaterialSubject subject) {
		return (target.ordinal() << SHIFT) | subject.ordinal();
	}

	public static MaterialContext get(MaterialTarget target, MaterialSubject subject) {
		return VALUES[index(target, subject)];
	}

	public MaterialContext get(int index) {
		return VALUES[index];
	}
}
