package grondag.canvas.pipeline;

import net.minecraft.util.math.MathHelper;

import grondag.fermion.varia.Useful;

public class PipelineContext {
	public final PipelineTarget target;
	public final PipelineSubject subject;
	public final int index;

	private PipelineContext(PipelineTarget target, PipelineSubject subject, int index) {
		this.target = target;
		this.subject = subject;
		this.index = index;
	}

	private static final int MAX_TARGETS = MathHelper.smallestEncompassingPowerOfTwo(PipelineTarget.values().length);
	private static final int MAX_SUBJECTS = MathHelper.smallestEncompassingPowerOfTwo(PipelineSubject.values().length);
	private static final int SHIFT = Useful.bitLength(MAX_SUBJECTS);
	public static final int MAX_CONTEXTS = MAX_TARGETS * MAX_SUBJECTS;

	private static final PipelineContext[] VALUES = new PipelineContext[MAX_CONTEXTS];

	static {
		for (final PipelineTarget target : PipelineTarget.values()) {
			for (final PipelineSubject subject : PipelineSubject.values()) {
				final int index = index(target, subject);
				VALUES[index] = new PipelineContext(target, subject, index);
			}
		}
	}
	private static int index(PipelineTarget target, PipelineSubject subject) {
		return (target.ordinal() << SHIFT) | subject.ordinal();
	}

	public static PipelineContext get(PipelineTarget target, PipelineSubject subject) {
		return VALUES[index(target, subject)];
	}

	public PipelineContext get(int index) {
		return VALUES[index];
	}
}
