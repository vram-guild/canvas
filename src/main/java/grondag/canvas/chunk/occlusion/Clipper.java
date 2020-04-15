package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.clipHighX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.clipHighY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.clipLowX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.clipLowY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.clipNear;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsClipHighX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsClipHighY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsClipLowX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsClipLowY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.needsNearClip;
import static grondag.canvas.chunk.occlusion.Rasterizer.drawTri;
import static grondag.canvas.chunk.occlusion.Rasterizer.testTri;
import static grondag.canvas.chunk.occlusion.Data.V_HIGH_X_CLIP_A;
import static grondag.canvas.chunk.occlusion.Data.V_HIGH_X_CLIP_B;
import static grondag.canvas.chunk.occlusion.Data.V_HIGH_Y_CLIP_A;
import static grondag.canvas.chunk.occlusion.Data.V_HIGH_Y_CLIP_B;
import static grondag.canvas.chunk.occlusion.Data.V_LOW_X_CLIP_A;
import static grondag.canvas.chunk.occlusion.Data.V_LOW_X_CLIP_B;
import static grondag.canvas.chunk.occlusion.Data.V_LOW_Y_CLIP_A;
import static grondag.canvas.chunk.occlusion.Data.V_LOW_Y_CLIP_B;
import static grondag.canvas.chunk.occlusion.Data.V_NEAR_CLIP_A;
import static grondag.canvas.chunk.occlusion.Data.V_NEAR_CLIP_B;
import static grondag.canvas.chunk.occlusion.Data.vertexData;

abstract class Clipper {
	private Clipper() { }

	static final void drawQuad(int v0, int v1, int v2, int v3) {
		final int split = needsNearClip(vertexData, v0) | (needsNearClip(vertexData, v1) << 1) | (needsNearClip(vertexData, v2) << 2) | (needsNearClip(vertexData, v3) << 3);

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

	static final void drawSplitThree(int extA, int internal, int extB) {
		clipNear(vertexData, V_NEAR_CLIP_A, internal, extA);
		clipNear(vertexData, V_NEAR_CLIP_B, internal, extB);

		//  TODO: remove
		//		if (!isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal + PV_PX], vertexData[internal + PV_PY],
		//				vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY])) {
		//
		//			System.out.println("TRI");
		//			System.out.println(String.format("extA: %d \t %d \t %f \t %f", vertexData[extA + PV_PX], vertexData[extA + PV_PY], Float.intBitsToFloat(vertexData[extA + PV_Z]), Float.intBitsToFloat(vertexData[extA + PV_W])));
		//			System.out.println(String.format("int0: %d \t %d \t %f \t %f", vertexData[internal + PV_PX], vertexData[internal + PV_PY], Float.intBitsToFloat(vertexData[internal + PV_Z]), Float.intBitsToFloat(vertexData[internal + PV_W])));
		//			System.out.println(String.format("extB: %d \t %d \t %f \t %f", vertexData[extB + PV_PX], vertexData[extB + PV_PY], Float.intBitsToFloat(vertexData[extB + PV_Z]), Float.intBitsToFloat(vertexData[extB + PV_W])));
		//			System.out.println();
		//			System.out.println(String.format("clipA: %d \t %d", vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY]));
		//			System.out.println(String.format("clipB: %d \t %d", vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY]));
		//			System.out.println();
		//		}
		//
		//		assert (isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal + PV_PX], vertexData[internal + PV_PY],
		//				vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY])) : "Triangle not counter-clockwise";

		drawTri(V_NEAR_CLIP_A, internal, V_NEAR_CLIP_B);
	}

	static final void drawSplitTwo(int extA, int internal0, int internal1, int extB) {
		clipNear(vertexData, V_NEAR_CLIP_A, internal0, extA);
		clipNear(vertexData, V_NEAR_CLIP_B, internal1, extB);

		//  TODO: remove
		//		if (!isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal0 + PV_PX], vertexData[internal0 + PV_PY],
		//				vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY])
		//				||  !isCcw(
		//						vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//						vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY],
		//						vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY])) {
		//
		//			System.out.println("QUAD");
		//			System.out.println(String.format("extA: %d \t %d \t %f \t %f", vertexData[extA + PV_PX], vertexData[extA + PV_PY], Float.intBitsToFloat(vertexData[extA + PV_Z]), Float.intBitsToFloat(vertexData[extA + PV_W])));
		//			System.out.println(String.format("int0: %d \t %d \t %f \t %f", vertexData[internal0 + PV_PX], vertexData[internal0 + PV_PY], Float.intBitsToFloat(vertexData[internal0 + PV_Z]), Float.intBitsToFloat(vertexData[internal0 + PV_W])));
		//			System.out.println(String.format("int1: %d \t %d \t %f \t %f", vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY], Float.intBitsToFloat(vertexData[internal1 + PV_Z]), Float.intBitsToFloat(vertexData[internal1 + PV_W])));
		//			System.out.println(String.format("extB: %d \t %d \t %f \t %f", vertexData[extB + PV_PX], vertexData[extB + PV_PY], Float.intBitsToFloat(vertexData[extB + PV_Z]), Float.intBitsToFloat(vertexData[extB + PV_W])));
		//			System.out.println();
		//			System.out.println(String.format("clipA: %d \t %d", vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY]));
		//			System.out.println(String.format("clipB: %d \t %d", vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY]));
		//			System.out.println();
		//		}
		//
		//		assert (isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal0 + PV_PX], vertexData[internal0 + PV_PY],
		//				vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY])) : "Triangle not counter-clockwise";
		//
		//		assert (isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY],
		//				vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY])) : "Triangle not counter-clockwise";

		drawTri(V_NEAR_CLIP_A, internal0, internal1);
		drawTri(V_NEAR_CLIP_A, internal1, V_NEAR_CLIP_B);
	}

	static final void drawSplitOne(int v0, int v1, int v2, int ext) {
		clipNear(vertexData, V_NEAR_CLIP_A, v2, ext);
		clipNear(vertexData, V_NEAR_CLIP_B, v0, ext);

		drawTri(v0, v1, v2);
		drawTri(v0, v2, V_NEAR_CLIP_A);
		drawTri(v0, V_NEAR_CLIP_A, V_NEAR_CLIP_B);
	}

	static boolean testQuad(int v0, int v1, int v2, int v3) {

		final int split = needsNearClip(vertexData, v0) | (needsNearClip(vertexData, v1) << 1) | (needsNearClip(vertexData, v2) << 2) | (needsNearClip(vertexData, v3) << 3);

		switch (split) {

		// nominal case, all inside
		case 0b0000: {
			return testTri(v0, v1, v2) || testTri(v0, v2, v3);
		}

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

	static final boolean testSplitThree(int extA, int internal, int extB) {

		clipNear(vertexData, V_NEAR_CLIP_A, internal, extA);
		clipNear(vertexData, V_NEAR_CLIP_B, internal, extB);

		return testTri(V_NEAR_CLIP_A, internal, V_NEAR_CLIP_B);
	}

	static final boolean testSplitTwo(int extA, int internal0, int internal1, int extB) {

		clipNear(vertexData, V_NEAR_CLIP_A, internal0, extA);
		clipNear(vertexData, V_NEAR_CLIP_B, internal1, extB);

		//  TODO: remove
		//		if (!isCcw(
		//				vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//				vertexData[internal0 + PV_PX], vertexData[internal0 + PV_PY],
		//				vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY])
		//				||  !isCcw(
		//						vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY],
		//						vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY],
		//						vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY])) {
		//
		//			System.out.println("QUAD");
		//			System.out.println(String.format("extA: %d \t %d \t %f \t %f", vertexData[extA + PV_PX], vertexData[extA + PV_PY], Float.intBitsToFloat(vertexData[extA + PV_Z]), Float.intBitsToFloat(vertexData[extA + PV_W])));
		//			System.out.println(String.format("int0: %d \t %d \t %f \t %f", vertexData[internal0 + PV_PX], vertexData[internal0 + PV_PY], Float.intBitsToFloat(vertexData[internal0 + PV_Z]), Float.intBitsToFloat(vertexData[internal0 + PV_W])));
		//			System.out.println(String.format("int1: %d \t %d \t %f \t %f", vertexData[internal1 + PV_PX], vertexData[internal1 + PV_PY], Float.intBitsToFloat(vertexData[internal1 + PV_Z]), Float.intBitsToFloat(vertexData[internal1 + PV_W])));
		//			System.out.println(String.format("extB: %d \t %d \t %f \t %f", vertexData[extB + PV_PX], vertexData[extB + PV_PY], Float.intBitsToFloat(vertexData[extB + PV_Z]), Float.intBitsToFloat(vertexData[extB + PV_W])));
		//			System.out.println();
		//			System.out.println(String.format("clipA: %d \t %d", vertexData[V_NEAR_CLIP_A + PV_PX], vertexData[V_NEAR_CLIP_A + PV_PY]));
		//			System.out.println(String.format("clipB: %d \t %d", vertexData[V_NEAR_CLIP_B + PV_PX], vertexData[V_NEAR_CLIP_B + PV_PY]));
		//			System.out.println();
		//		}

		return testTri(V_NEAR_CLIP_A, internal0, internal1) || testTri(V_NEAR_CLIP_A, internal1, V_NEAR_CLIP_B);
	}

	static final boolean testSplitOne(int v0, int v1, int v2, int ext) {

		clipNear(vertexData, V_NEAR_CLIP_A, v2, ext);
		clipNear(vertexData, V_NEAR_CLIP_B, v0, ext);

		return testTri(v0, v1, v2) || testTri(V_NEAR_CLIP_B, v2, v0) || testTri(V_NEAR_CLIP_B, V_NEAR_CLIP_A, v2);
	}

	static final void drawClippedLowX(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipLowX(vertexData, v2) | (needsClipLowX(vertexData, v1) << 1) | (needsClipLowX(vertexData, v0) << 2);

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

	static void drawClippedLowXOne(int v0ext, int v1, int v2) {

		clipLowX(vertexData, V_LOW_X_CLIP_A, v1, v0ext);
		clipLowX(vertexData, V_LOW_X_CLIP_B, v2, v0ext);
		drawClippedLowY(V_LOW_X_CLIP_A, v1, V_LOW_X_CLIP_B);
		drawClippedLowY(V_LOW_X_CLIP_B, v1, v2);
	}

	static void drawClippedLowXTwo(int v0ext, int v1ext, int v2) {

		clipLowX(vertexData, V_LOW_X_CLIP_A, v2, v0ext);
		clipLowX(vertexData, V_LOW_X_CLIP_B, v2, v1ext);
		drawClippedLowY(v2, V_LOW_X_CLIP_A, V_LOW_X_CLIP_B);
	}

	static void drawClippedLowY(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipLowY(vertexData, v2) | (needsClipLowY(vertexData, v1) << 1) | (needsClipLowY(vertexData, v0) << 2);

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

	static void drawClippedLowYOne(int v0ext, int v1, int v2) {

		clipLowY(vertexData, V_LOW_Y_CLIP_A, v1, v0ext);
		clipLowY(vertexData, V_LOW_Y_CLIP_B, v2, v0ext);
		drawClippedHighX(V_LOW_Y_CLIP_A, v1, V_LOW_Y_CLIP_B);
		drawClippedHighX(V_LOW_Y_CLIP_B, v1, v2);
	}

	static void drawClippedLowYTwo(int v0ext, int v1ext, int v2) {

		clipLowY(vertexData, V_LOW_Y_CLIP_A, v2, v0ext);
		clipLowY(vertexData, V_LOW_Y_CLIP_B, v2, v1ext);
		drawClippedHighX(v2, V_LOW_Y_CLIP_A, V_LOW_Y_CLIP_B);
	}

	static void drawClippedHighX(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipHighX(vertexData, v2) | (needsClipHighX(vertexData, v1) << 1) | (needsClipHighX(vertexData, v0) << 2);

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

	static void drawClippedHighXOne(int v0ext, int v1, int v2) {

		clipHighX(vertexData, V_HIGH_X_CLIP_A, v1, v0ext);
		clipHighX(vertexData, V_HIGH_X_CLIP_B, v2, v0ext);
		drawClippedHighY(V_HIGH_X_CLIP_A, v1, V_HIGH_X_CLIP_B);
		drawClippedHighY(V_HIGH_X_CLIP_B, v1, v2);
	}

	static void drawClippedHighXTwo(int v0ext, int v1ext, int v2) {

		clipHighX(vertexData, V_HIGH_X_CLIP_A, v2, v0ext);
		clipHighX(vertexData, V_HIGH_X_CLIP_B, v2, v1ext);
		drawClippedHighY(v2, V_HIGH_X_CLIP_A, V_HIGH_X_CLIP_B);
	}

	static void drawClippedHighY(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipHighY(vertexData, v2) | (needsClipHighY(vertexData, v1) << 1) | (needsClipHighY(vertexData, v0) << 2);

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

	static void drawClippedHighYOne(int v0ext, int v1, int v2) {

		clipHighY(vertexData, V_HIGH_Y_CLIP_A, v1, v0ext);
		clipHighY(vertexData, V_HIGH_Y_CLIP_B, v2, v0ext);
		drawTri(V_HIGH_Y_CLIP_A, v1, V_HIGH_Y_CLIP_B);
		drawTri(V_HIGH_Y_CLIP_B, v1, v2);
	}

	static void drawClippedHighYTwo(int v0ext, int v1ext, int v2) {

		clipHighY(vertexData, V_HIGH_Y_CLIP_A, v2, v0ext);
		clipHighY(vertexData, V_HIGH_Y_CLIP_B, v2, v1ext);
		drawTri(v2, V_HIGH_Y_CLIP_A, V_HIGH_Y_CLIP_B);
	}

	static final boolean testClippedLowX(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipLowX(vertexData, v2) | (needsClipLowX(vertexData, v1) << 1) | (needsClipLowX(vertexData, v0) << 2);

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

	static boolean testClippedLowXOne(int v0ext, int v1, int v2) {

		clipLowX(vertexData, V_LOW_X_CLIP_A, v1, v0ext);
		clipLowX(vertexData, V_LOW_X_CLIP_B, v2, v0ext);
		return testClippedLowY(V_LOW_X_CLIP_A, v1, V_LOW_X_CLIP_B)
				|| testClippedLowY(V_LOW_X_CLIP_B, v1, v2);
	}

	static boolean testClippedLowXTwo(int v0ext, int v1ext, int v2) {
		clipLowX(vertexData, V_LOW_X_CLIP_A, v2, v0ext);
		clipLowX(vertexData, V_LOW_X_CLIP_B, v2, v1ext);
		return testClippedLowY(v2, V_LOW_X_CLIP_A, V_LOW_X_CLIP_B);
	}

	static boolean testClippedLowY(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipLowY(vertexData, v2) | (needsClipLowY(vertexData, v1) << 1) | (needsClipLowY(vertexData, v0) << 2);

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

	static final boolean testClippedLowYOne(int v0ext, int v1, int v2) {

		clipLowY(vertexData, V_LOW_Y_CLIP_A, v1, v0ext);
		clipLowY(vertexData, V_LOW_Y_CLIP_B, v2, v0ext);
		return testClippedHighX(V_LOW_Y_CLIP_A, v1, V_LOW_Y_CLIP_B)
				|| testClippedHighX(V_LOW_Y_CLIP_B, v1, v2);
	}

	static boolean testClippedLowYTwo(int v0ext, int v1ext, int v2) {

		clipLowY(vertexData, V_LOW_Y_CLIP_A, v2, v0ext);
		clipLowY(vertexData, V_LOW_Y_CLIP_B, v2, v1ext);
		return testClippedHighX(v2, V_LOW_Y_CLIP_A, V_LOW_Y_CLIP_B);
	}

	static boolean testClippedHighX(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipHighX(vertexData, v2) | (needsClipHighX(vertexData, v1) << 1) | (needsClipHighX(vertexData, v0) << 2);

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

	static boolean testClippedHighXOne(int v0ext, int v1, int v2) {

		clipHighX(vertexData, V_HIGH_X_CLIP_A, v1, v0ext);
		clipHighX(vertexData, V_HIGH_X_CLIP_B, v2, v0ext);
		return testClippedHighY(V_HIGH_X_CLIP_A, v1, V_HIGH_X_CLIP_B)
				|| testClippedHighY(V_HIGH_X_CLIP_B, v1, v2);
	}

	static boolean testClippedHighXTwo(int v0ext, int v1ext, int v2) {
		clipHighX(vertexData, V_HIGH_X_CLIP_A, v2, v0ext);
		clipHighX(vertexData, V_HIGH_X_CLIP_B, v2, v1ext);
		return testClippedHighY(v2, V_HIGH_X_CLIP_A, V_HIGH_X_CLIP_B);
	}

	static boolean testClippedHighY(int v0, int v1, int v2) {

		// NB: order here is lexical not bitwise
		final int split = needsClipHighY(vertexData, v2) | (needsClipHighY(vertexData, v1) << 1) | (needsClipHighY(vertexData, v0) << 2);

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

	static boolean testClippedHighYOne(int v0ext, int v1, int v2) {

		clipHighY(vertexData, V_HIGH_Y_CLIP_A, v1, v0ext);
		clipHighY(vertexData, V_HIGH_Y_CLIP_B, v2, v0ext);
		return testTri(V_HIGH_Y_CLIP_A, v1, V_HIGH_Y_CLIP_B)
				|| testTri(V_HIGH_Y_CLIP_B, v1, v2);
	}

	static boolean testClippedHighYTwo(int v0ext, int v1ext, int v2) {

		clipHighY(vertexData, V_HIGH_Y_CLIP_A, v2, v0ext);
		clipHighY(vertexData, V_HIGH_Y_CLIP_B, v2, v1ext);
		return testTri(v2, V_HIGH_Y_CLIP_A, V_HIGH_Y_CLIP_B);
	}
}
