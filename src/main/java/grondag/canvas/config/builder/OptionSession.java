/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.config.builder;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.gui.components.AbstractButton;
import org.jetbrains.annotations.Nullable;

public class OptionSession {
	private static final int BUCKET_SIZE = 64;
	private final LongArrayList flagBuckets = new LongArrayList();
	private int nextIndex = 0;
	private AbstractButton saveButton;

	public OptionSession() {
	}

	private int indexBucket(int index) {
		return index / BUCKET_SIZE;
	}

	private int indexFlag(int index) {
		return index % BUCKET_SIZE;
	}

	private int generateIndex() {
		final int nextBucketId = indexBucket(nextIndex);

		while (flagBuckets.size() < nextBucketId + 1) {
			flagBuckets.add(0L);
		}

		return nextIndex++;
	}

	private void refreshSave() {
		if (saveButton != null) {
			boolean changed = false;

			for (Long flag:flagBuckets) {
				changed |= flag != 0L;
			}

			saveButton.active = changed;
		}
	}

	private void detectChange(Object initial, Object current, int index) {
		final int bucketId = indexBucket(index);
		final long flagId = indexFlag(index);
		final long flag = (Objects.equals(initial, current) ? 0L : 1L) << flagId;
		flagBuckets.set(bucketId, (flagBuckets.getLong(bucketId) & ~(1L << flagId)) | flag);

		refreshSave();
	}

	public void setSaveButton(AbstractButton saveButton) {
		this.saveButton = saveButton;
		refreshSave();
	}

	private class OptionAttachment<T> implements Consumer<T> {
		private Option option;
		private final Consumer<T> setter;
		private final T initialValue;
		private final int index;

		private OptionAttachment(Supplier<T> getter, Consumer<T> setter, int index) {
			initialValue = getter.get();
			this.setter = setter;
			this.index = index;
		}

		private void attach(Option option) {
			this.option = option;
		}

		@Override
		public void accept(T t) {
			setter.accept(t);
			option.refreshResetButton();
			detectChange(initialValue, t, index);
		}
	}

	public Option booleanOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable String tooltipKey) {
		final var attachment = new OptionAttachment<>(getter, setter, generateIndex());
		final var created = new Toggle(key, getter, attachment, defaultVal, tooltipKey);
		attachment.attach(created);
		return created;
	}

	public <T extends Enum<T>> Option enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, Class<T> enumType, @Nullable String tooltipKey) {
		final var attachment = new OptionAttachment<>(getter, setter, generateIndex());
		final var created = new EnumButton<T>(key, getter, attachment, defaultVal, enumType.getEnumConstants(), tooltipKey);
		attachment.attach(created);
		return created;
	}

	public <T> Option enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable String tooltipKey) {
		final var attachment = new OptionAttachment<>(getter, setter, generateIndex());
		final var created = new EnumButton<T>(key, getter, attachment, defaultVal, values, tooltipKey);
		attachment.attach(created);
		return created;
	}

	public Option intOption(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable String tooltipKey) {
		final var attachment = new OptionAttachment<>(getter, setter, generateIndex());
		final var created = new Slider.IntSlider(key, min, max, step, getter, attachment, defaultVal, tooltipKey);
		attachment.attach(created);
		return created;
	}

	public Option floatOption(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable String tooltipKey) {
		final var attachment = new OptionAttachment<>(getter, setter, generateIndex());
		final var created = new Slider.FloatSlider(key, min, max, step, getter, attachment, defaultVal, tooltipKey);
		attachment.attach(created);
		return created;
	}
}
