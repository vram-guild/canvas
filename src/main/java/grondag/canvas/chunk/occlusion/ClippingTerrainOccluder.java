package grondag.canvas.chunk.occlusion;

public abstract class ClippingTerrainOccluder extends AbstractTerrainOccluder {
	private final ProjectionVector4f vNearClipA = new ProjectionVector4f();
	private final ProjectionVector4f vNearClipB = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowXA = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowXB = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowYA = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowYB = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighXA = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighXB = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighYA = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighYB = new ProjectionVector4f();

	@Override
	protected final void drawQuad(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f v3) {
		final int split = v0.needsNearClip() | (v1.needsNearClip() << 1) | (v2.needsNearClip() << 2) | (v3.needsNearClip() << 3);

		switch (split) {

		// nominal case, all inside
		case 0b0000:
			drawTri(v0, v1, v2);
			drawTri(v0, v2, v3);
			break;

			// missing one corner, three tris
		case 0b0001:
			drawSplitOne(v1, v2, v3, v0);
			break;
		case 0b0010:
			drawSplitOne(v2, v3, v0, v1);
			break;
		case 0b0100:
			drawSplitOne(v3, v0, v1, v2);
			break;
		case 0b1000:
			drawSplitOne(v0, v1, v2, v3);
			break;

			// missing two corners, two tris
		case 0b0011:
			drawSplitTwo(v1, v2, v3, v0);
			break;
		case 0b0110:
			drawSplitTwo(v2, v3, v0, v1);
			break;
		case 0b1100:
			drawSplitTwo(v3, v0, v1, v2);
			break;
		case 0b1001:
			drawSplitTwo(v0, v1, v2, v3);
			break;

			// missing three corner, one tri
		case 0b0111:
			drawSplitThree(v2, v3, v0);
			break;
		case 0b1110:
			drawSplitThree(v3, v0, v1);
			break;
		case 0b1101:
			drawSplitThree(v0, v1, v2);
			break;
		case 0b1011:
			drawSplitThree(v1, v2, v3);
			break;

		default:
		case 0b1111:
			// all external, draw nothing
			break;
		}
	}

	private final void drawSplitThree(ProjectionVector4f extA, ProjectionVector4f internal, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal, extA);
		vb.clipNear(internal, extB);

		drawTri(va, internal, vb);
	}

	private final void drawSplitTwo(ProjectionVector4f extA, ProjectionVector4f internal0, ProjectionVector4f internal1, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal0, extA);
		vb.clipNear(internal1, extB);

		drawTri(va, internal0, internal1);
		drawTri(va, internal1, vb);
	}

	private final void drawSplitOne(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f ext) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(v2, ext);
		vb.clipNear(v0, ext);

		drawTri(v0, v1, v2);
		drawTri(v0, v2, va);
		drawTri(v0, va, vb);
	}

	@Override
	protected final boolean testQuad(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f v3) {
		final int split = v0.needsNearClip() | (v1.needsNearClip() << 1) | (v2.needsNearClip() << 2) | (v3.needsNearClip() << 3);

		switch (split) {

		// nominal case, all inside
		case 0b0000:
			return testTri(v0, v1, v2) || testTri(v0, v2, v3);

			// missing one corner, three tris
		case 0b0001:
			return testSplitOne(v1, v2, v3, v0);
		case 0b0010:
			return testSplitOne(v2, v3, v0, v1);
		case 0b0100:
			return testSplitOne(v3, v0, v1, v2);
		case 0b1000:
			return testSplitOne(v0, v1, v2, v3);

			// missing two corners, two tris
		case 0b0011:
			return testSplitTwo(v1, v2, v3, v0);
		case 0b0110:
			return testSplitTwo(v2, v3, v0, v1);
		case 0b1100:
			return testSplitTwo(v3, v0, v1, v2);
		case 0b1001:
			return testSplitTwo(v0, v1, v2, v3);

			// missing three corner, one tri
		case 0b0111:
			return testSplitThree(v2, v3, v0);
		case 0b1110:
			return testSplitThree(v3, v0, v1);
		case 0b1101:
			return testSplitThree(v0, v1, v2);
		case 0b1011:
			return testSplitThree(v1, v2, v3);

		default:
		case 0b1111:
			// all external, not in view
			return false;
		}
	}

	private final boolean testSplitThree(ProjectionVector4f extA, ProjectionVector4f internal, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal, extA);
		vb.clipNear(internal, extB);

		return testTri(va, internal, vb);
	}

	private final boolean testSplitTwo(ProjectionVector4f extA, ProjectionVector4f internal0, ProjectionVector4f internal1, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal0, extA);
		vb.clipNear(internal1, extB);

		return testTri(va, internal0, internal1) || testTri(va, internal1, vb);
	}

	private final boolean testSplitOne(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f ext) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(v2, ext);
		vb.clipNear(v0, ext);

		return testTri(v0, v1, v2) || testTri(v0, v2, va) || testTri(v0, va, vb);
	}

	protected final void drawClippedLowX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowX() | (v1.needsClipLowX() << 1) | (v0.needsClipLowX() << 2);

		switch (split) {
		case 0b000:
			drawClippedLowY(v0, v1, v2);
			break;

		case 0b100:
			drawClippedLowXOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedLowXOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedLowXOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedLowXTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedLowXTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedLowXTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedLowXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v1, v0ext);
		vClipLowXB.clipLowX(v2, v0ext);
		drawClippedLowY(vClipLowXA, v1, vClipLowXB);
		drawClippedLowY(vClipLowXB, v1, v2);
	}

	private void drawClippedLowXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v2, v0ext);
		vClipLowXB.clipLowX(v2, v1ext);
		drawClippedLowY(v2, vClipLowXA, vClipLowXB);
	}

	private void drawClippedLowY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowY() | (v1.needsClipLowY() << 1) | (v0.needsClipLowY() << 2);

		switch (split) {
		case 0b000:
			drawClippedHighX(v0, v1, v2);
			break;

		case 0b100:
			drawClippedLowYOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedLowYOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedLowYOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedLowYTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedLowYTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedLowYTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedLowYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v1, v0ext);
		vClipLowYB.clipLowY(v2, v0ext);
		drawClippedHighX(vClipLowYA, v1, vClipLowYB);
		drawClippedHighX(vClipLowYB, v1, v2);
	}

	private void drawClippedLowYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v2, v0ext);
		vClipLowYB.clipLowY(v2, v1ext);
		drawClippedHighX(v2, vClipLowYA, vClipLowYB);
	}

	private void drawClippedHighX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighX() | (v1.needsClipHighX() << 1) | (v0.needsClipHighX() << 2);

		switch (split) {
		case 0b000:
			drawClippedHighY(v0, v1, v2);
			break;

		case 0b100:
			drawClippedHighXOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedHighXOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedHighXOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedHighXTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedHighXTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedHighXTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedHighXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v1, v0ext);
		vClipHighXB.clipHighX(v2, v0ext);
		drawClippedHighY(vClipHighXA, v1, vClipHighXB);
		drawClippedHighY(vClipHighXB, v1, v2);
	}

	private void drawClippedHighXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v2, v0ext);
		vClipHighXB.clipHighX(v2, v1ext);
		drawClippedHighY(v2, vClipHighXA, vClipHighXB);
	}

	private void drawClippedHighY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighY() | (v1.needsClipHighY() << 1) | (v0.needsClipHighY() << 2);

		switch (split) {
		case 0b000:
			drawTri(v0, v1, v2);
			break;

		case 0b100:
			drawClippedHighYOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedHighYOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedHighYOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedHighYTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedHighYTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedHighYTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedHighYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v1, v0ext);
		vClipHighYB.clipHighY(v2, v0ext);
		drawTri(vClipHighYA, v1, vClipHighYB);
		drawTri(vClipHighYB, v1, v2);
	}

	private void drawClippedHighYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v2, v0ext);
		vClipHighYB.clipHighY(v2, v1ext);
		drawTri(v2, vClipHighYA, vClipHighYB);
	}

	protected final boolean testClippedLowX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowX() | (v1.needsClipLowX() << 1) | (v0.needsClipLowX() << 2);

		switch (split) {
		case 0b000:
			return testClippedLowY(v0, v1, v2);

		case 0b100:
			return testClippedLowXOne(v0, v1, v2);

		case 0b010:
			return testClippedLowXOne(v1, v2, v0);

		case 0b001:
			return testClippedLowXOne(v2, v0, v1);

		case 0b110:
			return testClippedLowXTwo(v0, v1, v2);

		case 0b011:
			return testClippedLowXTwo(v1, v2, v0);

		case 0b101:
			return testClippedLowXTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedLowXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v1, v0ext);
		vClipLowXB.clipLowX(v2, v0ext);
		return testClippedLowY(vClipLowXA, v1, vClipLowXB)
				|| testClippedLowY(vClipLowXB, v1, v2);
	}

	private boolean testClippedLowXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v2, v0ext);
		vClipLowXB.clipLowX(v2, v1ext);
		return testClippedLowY(v2, vClipLowXA, vClipLowXB);
	}

	private boolean testClippedLowY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowY() | (v1.needsClipLowY() << 1) | (v0.needsClipLowY() << 2);

		switch (split) {
		case 0b000:
			return testClippedHighX(v0, v1, v2);

		case 0b100:
			return testClippedLowYOne(v0, v1, v2);

		case 0b010:
			return testClippedLowYOne(v1, v2, v0);

		case 0b001:
			return testClippedLowYOne(v2, v0, v1);

		case 0b110:
			return testClippedLowYTwo(v0, v1, v2);

		case 0b011:
			return testClippedLowYTwo(v1, v2, v0);

		case 0b101:
			return testClippedLowYTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private final boolean testClippedLowYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v1, v0ext);
		vClipLowYB.clipLowY(v2, v0ext);
		return testClippedHighX(vClipLowYA, v1, vClipLowYB)
				|| testClippedHighX(vClipLowYB, v1, v2);
	}

	private boolean testClippedLowYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v2, v0ext);
		vClipLowYB.clipLowY(v2, v1ext);
		return testClippedHighX(v2, vClipLowYA, vClipLowYB);
	}

	private boolean testClippedHighX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighX() | (v1.needsClipHighX() << 1) | (v0.needsClipHighX() << 2);

		switch (split) {
		case 0b000:
			return testClippedHighY(v0, v1, v2);

		case 0b100:
			return testClippedHighXOne(v0, v1, v2);

		case 0b010:
			return testClippedHighXOne(v1, v2, v0);

		case 0b001:
			return testClippedHighXOne(v2, v0, v1);

		case 0b110:
			return testClippedHighXTwo(v0, v1, v2);

		case 0b011:
			return testClippedHighXTwo(v1, v2, v0);

		case 0b101:
			return testClippedHighXTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedHighXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v1, v0ext);
		vClipHighXB.clipHighX(v2, v0ext);
		return testClippedHighY(vClipHighXA, v1, vClipHighXB)
				|| testClippedHighY(vClipHighXB, v1, v2);
	}

	private boolean testClippedHighXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v2, v0ext);
		vClipHighXB.clipHighX(v2, v1ext);
		return testClippedHighY(v2, vClipHighXA, vClipHighXB);
	}

	private boolean testClippedHighY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighY() | (v1.needsClipHighY() << 1) | (v0.needsClipHighY() << 2);

		switch (split) {
		case 0b000:
			return testTri(v0, v1, v2);

		case 0b100:
			return testClippedHighYOne(v0, v1, v2);

		case 0b010:
			return testClippedHighYOne(v1, v2, v0);

		case 0b001:
			return testClippedHighYOne(v2, v0, v1);

		case 0b110:
			return testClippedHighYTwo(v0, v1, v2);

		case 0b011:
			return testClippedHighYTwo(v1, v2, v0);

		case 0b101:
			return testClippedHighYTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedHighYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v1, v0ext);
		vClipHighYB.clipHighY(v2, v0ext);
		return testTri(vClipHighYA, v1, vClipHighYB)
				|| testTri(vClipHighYB, v1, v2);
	}

	private boolean testClippedHighYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v2, v0ext);
		vClipHighYB.clipHighY(v2, v1ext);
		return testTri(v2, vClipHighYA, vClipHighYB);
	}
}
