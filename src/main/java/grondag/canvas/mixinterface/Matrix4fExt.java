package grondag.canvas.mixinterface;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
}
