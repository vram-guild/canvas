package grondag.canvas.collision;

/**
 * All possible contiguous sections of the Z-axis into 1/8 units.<p>
 *
 * In retrospect, would probably be better if Slice were simply an 8-bit word instead of an enum
 * but code is working and is "fast enough" for now. Could matter, tho, with LOR.
 */
public enum Slice
{
	D1_0(1, 0),
	D1_1(1, 1),
	D1_2(1, 2),
	D1_3(1, 3),
	D1_4(1, 4),
	D1_5(1, 5),
	D1_6(1, 6),
	D1_7(1, 7),
	D2_0(2, 0),
	D2_1(2, 1),
	D2_2(2, 2),
	D2_3(2, 3),
	D2_4(2, 4),
	D2_5(2, 5),
	D2_6(2, 6),
	D3_0(3, 0),
	D3_1(3, 1),
	D3_2(3, 2),
	D3_3(3, 3),
	D3_4(3, 4),
	D3_5(3, 5),
	D4_0(4, 0),
	D4_1(4, 1),
	D4_2(4, 2),
	D4_3(4, 3),
	D4_4(4, 4),
	D5_0(5, 0),
	D5_1(5, 1),
	D5_2(5, 2),
	D5_3(5, 3),
	D6_0(6, 0),
	D6_1(6, 1),
	D6_2(6, 2),
	D7_0(7, 0),
	D7_1(7, 1),
	D8_0(8, 0);

	public final int depth;
	public final int min;

	/**
	 * INCLUSIVE
	 */
	public final int max;

	/**
	 * Bits are set if Z layer is included.
	 * Used for fast intersection testing.
	 */
	public final int layerBits;

	private Slice(int depth, int min)
	{
		this.depth = depth;
		this.min = min;
		max = min + depth - 1;

		int flags = 0;
		for(int i = 0; i < depth; i++)
		{
			flags |= (1 << (min + i));
		}

		layerBits = flags;
	}
}