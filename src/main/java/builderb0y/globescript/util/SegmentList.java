package builderb0y.globescript.util;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

/** copy-pasted the methods I care about from big globe. */
public abstract class SegmentList<T_Segment extends SegmentList.Segment> {

	public static final boolean ASSERTS = false;

	public final ObjectArrayList<T_Segment> backingList = new ObjectArrayList<>();

	public @Nullable T_Segment addSegment(int min, int max) {
		T_Segment result;
		if (max >= min) {
			if (this.backingList.isEmpty()) {
				this.backingList.add(result = this.newSegment(min, max));
			}
			else {
				T_Segment highest = this.backingList.get(this.backingList.size() - 1);
				T_Segment lowest  = this.backingList.get(0);
				if (min > highest.max) {
					//new segment is above all other segments.
					this.backingList.add(result = this.newSegment(min, max));
					this.mergeAt(this.backingList.size() - 1);
				}
				else if (min > lowest.min) {
					if (max >= highest.max) {
						//new segment contains highest.max only.
						int index = this.getSegmentIndex(min, false);
						T_Segment segment = this.backingList.get(index);
						if (segment.min < min) {
							segment.max = min - 1;
							index++;
						}
						if (index < this.backingList.size()) this.backingList.size(index);
						this.backingList.add(result = this.newSegment(min, max));
						this.mergeAt(this.backingList.size() - 1);
					}
					else {
						//new segment is in the middle of all other segments.
						int minIndex = this.getSegmentIndex(min, false);
						int maxIndex = this.getSegmentIndex(max, true);
						if (maxIndex < minIndex) {
							assert maxIndex == minIndex - 1;
							//new segment is between 2 other segments.
							this.backingList.add(minIndex, result = this.newSegment(min, max));
							this.mergeAt(minIndex);
						}
						else if (maxIndex == minIndex) {
							//new segment is inside another segment.
							T_Segment segment = this.backingList.get(minIndex);

							if (min <= segment.min) {
								if (max >= segment.max) {
									//new segment contains existing segment.
									this.backingList.set(minIndex, result = this.newSegment(min, max));
									this.mergeAt(minIndex);
								}
								else {
									//new segment covers the bottom of an existing segment.
									segment.min = max + 1;
									this.backingList.add(minIndex, result = this.newSegment(min, max));
									this.mergeAt(minIndex);
								}
							}
							else {
								if (max >= segment.max) {
									//new segment covers the top of an existing segment.
									segment.max = min - 1;
									this.backingList.add(minIndex + 1, result = this.newSegment(min, max));
									this.mergeAt(minIndex + 1);
								}
								else {
									//new segment is completely inside existing segment.
									@SuppressWarnings("unchecked")
									T_Segment clone = (T_Segment)(segment.clone());
									segment.max = min - 1;
									clone.min = max + 1;
									this.backingList.addAll(minIndex + 1, List.of(
										result = this.newSegment(min, max),
										clone
									));
									this.mergeAt(minIndex + 1);
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							T_Segment lowSegment = this.backingList.get(minIndex);
							T_Segment highSegment = this.backingList.get(maxIndex);
							if (lowSegment.min < min) {
								//partial intersection.
								lowSegment.max = min - 1;
								minIndex++;
							}
							if (highSegment.max > max) {
								//partial intersection.
								highSegment.min = max + 1;
								maxIndex--;
							}
							if (maxIndex >= minIndex) this.backingList.removeElements(minIndex, maxIndex + 1 /* convert to exclusive */);
							this.backingList.add(minIndex, result = this.newSegment(min, max));
							this.mergeAt(minIndex);
						}
					}
				}
				else {
					if (max >= highest.max) {
						//segment contains all other segments.
						this.backingList.clear();
						this.backingList.add(result = this.newSegment(min, max));
					}
					else if (max >= lowest.min) {
						//segment contains bottom of list (and possibly the middle segments).
						int index = this.getSegmentIndex(max, true);
						T_Segment segment = this.backingList.get(index);
						if (segment.max > max) {
							segment.min = max + 1;
							index--;
						}
						if (index >= 0) this.backingList.removeElements(0, index + 1);
						this.backingList.add(0, result = this.newSegment(min, max));
						this.mergeAt(0);
					}
					else {
						//segment is below all other segments.
						this.backingList.add(0, result = this.newSegment(min, max));
						this.mergeAt(0);
					}
				}
			}
			if (ASSERTS) this.checkIntegrity();
		}
		else {
			result = null;
		}
		return result;
	}

	public void removeSegment(int min, int max) {
		if (max >= min) {
			if (this.backingList.isEmpty()) {
				//no segments to remove.
			}
			else {
				T_Segment highest = this.backingList.get(this.backingList.size() - 1);
				T_Segment lowest  = this.backingList.get(0);
				if (min > highest.max) {
					//new segment is above all other segments.
					//nothing to do in this case.
				}
				else if (min > lowest.min) {
					if (max >= highest.max) {
						//new segment contains highest.max only.
						int index = this.getSegmentIndex(min, false);
						T_Segment segment = this.backingList.get(index);
						if (segment.min < min) {
							segment.max = min - 1;
							index++;
						}
						if (index < this.backingList.size()) this.backingList.size(index);
					}
					else {
						//new segment is in the middle of all other segments.
						int minIndex = this.getSegmentIndex(min, false);
						int maxIndex = this.getSegmentIndex(max, true);
						if (maxIndex < minIndex) {
							assert maxIndex == minIndex - 1;
							//new segment is between 2 other segments.
						}
						else if (maxIndex == minIndex) {
							//new segment is inside another segment.
							T_Segment segment = this.backingList.get(minIndex);

							if (min <= segment.min) {
								if (max >= segment.max) {
									//new segment contains existing segment.
									this.backingList.remove(minIndex);
								}
								else {
									//new segment covers the bottom of an existing segment.
									segment.min = max + 1;
								}
							}
							else {
								if (max >= segment.max) {
									//new segment covers the top of an existing segment.
									segment.max = min - 1;
								}
								else {
									//new segment is completely inside existing segment.
									@SuppressWarnings("unchecked")
									T_Segment clone = (T_Segment)(segment.clone());
									segment.max = min - 1;
									clone.min = max + 1;
									this.backingList.add(minIndex + 1, clone);
								}
							}
						}
						else {
							//new segment intersects multiple existing segments.
							T_Segment lowSegment = this.backingList.get(minIndex);
							T_Segment highSegment = this.backingList.get(maxIndex);
							if (lowSegment.min < min) {
								//partial intersection.
								lowSegment.max = min - 1;
								minIndex++;
							}
							if (highSegment.max > max) {
								//partial intersection.
								highSegment.min = max + 1;
								maxIndex--;
							}
							if (maxIndex >= minIndex) this.backingList.removeElements(minIndex, maxIndex + 1 /* convert to exclusive */);
						}
					}
				}
				else {
					if (max >= highest.max) {
						//segment contains all other segments.
						this.backingList.clear();
					}
					else if (max >= lowest.min) {
						//segment contains lowest.min only.
						int index = this.getSegmentIndex(max, true);
						T_Segment segment = this.backingList.get(index);
						if (segment.max > max) {
							segment.min = max + 1;
							index--;
						}
						if (index >= 0) this.backingList.removeElements(0, index + 1);
					}
					else {
						//segment is below all other segments.
						//nothing to do in this case either.
					}
				}
			}
			if (ASSERTS) this.checkIntegrity();
		}
	}

	public void mergeAt(int index) {
		T_Segment current = this.backingList.get(index);
		T_Segment other;
		if (index + 1 < this.backingList.size() && (other = this.backingList.get(index + 1)).min == current.max + 1 && other.canMergeWith(current)) {
			current.max = other.max;
			this.backingList.remove(index + 1);
		}
		if (index - 1 >= 0 && (other = this.backingList.get(index - 1)).max == current.min - 1 && other.canMergeWith(current)) {
			current.min = other.min;
			this.backingList.remove(index - 1);
		}
	}

	@SuppressWarnings({ "AssertWithSideEffects", "ConstantValue" })
	public static void checkAssertsEnabled() {
		boolean asserts = false;
		assert asserts = true;
		if (!asserts) throw new AssertionError("asserts not enabled. run with -ea");
	}

	public void checkIntegrity() {
		checkAssertsEnabled();
		if (!this.backingList.isEmpty()) {
			for (int index = 0, size = this.backingList.size(); index < size; index++) {
				T_Segment lowSegment = this.backingList.get(index);
				assert lowSegment.max >= lowSegment.min;
				if (index + 1 < size) {
					T_Segment highSegment = this.backingList.get(index + 1);
					assert highSegment.min > lowSegment.max;
					assert highSegment.min != lowSegment.max + 1 || !highSegment.canMergeWith(lowSegment);
				}
			}
		}
	}

	public int getSegmentIndex(int pos, boolean low) {
		int minIndex = 0, maxIndex = this.backingList.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			T_Segment segment = this.backingList.get(midIndex);
			if (pos < segment.min) {
				maxIndex = midIndex - 1;
			}
			else if (pos > segment.max) {
				minIndex = midIndex + 1;
			}
			else {
				return midIndex;
			}
		}
		return low ? maxIndex : minIndex;
	}

	public @Nullable T_Segment getOverlappingSegment(int pos) {
		int minIndex = 0, maxIndex = this.backingList.size() - 1;
		while (maxIndex >= minIndex) {
			int midIndex = (minIndex + maxIndex) >>> 1;
			T_Segment segment = this.backingList.get(midIndex);
			if (pos < segment.min) {
				maxIndex = midIndex - 1;
			}
			else if (pos > segment.max) {
				minIndex = midIndex + 1;
			}
			else {
				return segment;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return this.backingList.toString();
	}

	public abstract T_Segment newSegment(int min, int max);

	public static abstract class Segment implements Cloneable {

		public int min, max;

		public Segment(int min, int max) {
			this.min = min;
			this.max = max;
		}

		public abstract boolean canMergeWith(Segment that);

		@Override
		public Segment clone() {
			try {
				return (Segment)(super.clone());
			}
			catch (CloneNotSupportedException exception) {
				throw new AssertionError(exception);
			}
		}

		@Override
		public String toString() {
			return "[" + this.min + ", " + this.max + "]";
		}
	}
}