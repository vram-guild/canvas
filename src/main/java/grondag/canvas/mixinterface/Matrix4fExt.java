package grondag.canvas.mixinterface;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public interface Matrix4fExt {
	float a00();
	float a01();
	float a02();
	float a03();
	float a10();
	float a11();
	float a12();
	float a13();
	float a20();
	float a21();
	float a22();
	float a23();
	float a30();
	float a31();
	float a32();
	float a33();

	void a00(float val);
	void a01(float val);
	void a02(float val);
	void a03(float val);
	void a10(float val);
	void a11(float val);
	void a12(float val);
	void a13(float val);
	void a20(float val);
	void a21(float val);
	void a22(float val);
	void a23(float val);
	void a30(float val);
	void a31(float val);
	void a32(float val);
	void a33(float val);

	default void multiply(Matrix4fExt val) {
		((Matrix4f)(Object) this).multiply((Matrix4f)(Object) val);
	}

	default void loadIdentity() {
		((Matrix4f)(Object) this).loadIdentity();
	}

	default void set(Matrix4fExt val) {
		a00(val.a00());
		a01(val.a01());
		a02(val.a02());
		a03(val.a03());

		a10(val.a10());
		a11(val.a11());
		a12(val.a12());
		a13(val.a13());

		a20(val.a20());
		a21(val.a21());
		a22(val.a22());
		a23(val.a23());

		a30(val.a30());
		a31(val.a31());
		a32(val.a32());
		a33(val.a33());
	}

	default void set(Matrix4f val) {
		set((Matrix4fExt)(Object) val);
	}

	default boolean matches(Matrix4fExt val) {
		return a00() == val.a00()
				&& a01() == val.a01()
				&& a02() == val.a02()
				&& a03() == val.a03()

				&& a10() == val.a10()
				&& a11() == val.a11()
				&& a12() == val.a12()
				&& a13() == val.a13()

				&& a20() == val.a20()
				&& a21() == val.a21()
				&& a22() == val.a22()
				&& a23() == val.a23()

				&& a30() == val.a30()
				&& a31() == val.a31()
				&& a32() == val.a32()
				&& a33() == val.a33();
	}

	default boolean matches(Matrix4f val) {
		return matches((Matrix4fExt)(Object) val);
	}
}
