package builderb0y.globescript.util;

import org.jetbrains.annotations.Nullable;

public class RangedBitSet extends SegmentList<RangedBitSet.Segment> {

	public boolean contains(int value) {
		return this.getOverlappingSegment(value) != null;
	}

	public boolean containsAny(int min, int max) {
		return this.getSegmentIndex(min, false) <= this.getSegmentIndex(max, true);
	}

	public @Nullable RangedBitSet intersection(int min, int max) {
		int minIndex = this.getSegmentIndex(min, false);
		int maxIndex = this.getSegmentIndex(max, true);
		if (minIndex <= maxIndex) {
			RangedBitSet result = new RangedBitSet();
			for (int index = minIndex; index <= maxIndex; index++) {
				Segment segment = this.backingList.get(index);
				result.addSegment(Math.max(segment.min, min), Math.min(segment.max, max));
			}
			return result;
		}
		else {
			return null;
		}
	}

	@Override
	public Segment newSegment(int min, int max) {
		return new Segment(min, max);
	}

	public static class Segment extends SegmentList.Segment {

		public Segment(int min, int max) {
			super(min, max);
		}

		@Override
		public boolean canMergeWith(SegmentList.Segment that) {
			return true;
		}
	}
}