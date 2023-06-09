package grondag.canvas.light.api;

/**
 * BlockLight API draft.
 * <p>
 * Similar to Material, this needs to be constructed by a factory provided by implementation.
 */
public interface BlockLight {
	/**
	 * The light level. Typically, this represents the light radius after multiplied with the
	 * highest color component, but also affects maximum brightness.
	 * <p>
	 * Implementation may choose whether to prioritize the radius aspect or brightness aspect.
	 * <p>
	 * Typical value is in range 0-15. Value outside of this range is implementation-specific.
	 * <p>
	 * In JSON format, defaults to the vanilla registered light level when missing.
	 * Importantly, light level is attached to blocks, so for fluid states
	 * (not their block counterpart) the default is always 0.
	 *
	 * @return Raw light level value
	 */
	float lightLevel();

	/**
	 * Red intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 *
	 * @return Raw red intensity
	 */
	float red();

	/**
	 * Green intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 *
	 * @return Raw green intensity
	 */
	float green();

	/**
	 * Blue intensity. Behavior of values outside of range 0-1 is undefined.
	 * In JSON format, defaults to 0 when missing.
	 *
	 * @return Raw blue intensity
	 */
	float blue();
}
