package builderb0y.globescript;

import java.util.Map;
import java.util.Objects;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StackMap<K, V> {

	public final Hash.Strategy<K> strategy;
	@SuppressWarnings("unchecked")
	public @Nullable Node<K, V> @NotNull [] table = new Node[16];
	public @Nullable Node<K, V> leftMost, rightMost;
	public int frameCount = 1, nodeCount = 0, bucketOccupancy = 0;

	public StackMap() {
		this.strategy = Util.defaultHashStrategy();
	}

	public StackMap(Hash.Strategy<K> strategy) {
		this.strategy = strategy;
	}

	public void putAll(StackMap<K, V> that) {
		if (that.frameCount != 1) throw new IllegalArgumentException("Iteration only supported on fresh maps");
		for (Node<K, V> node : that.table) {
			while (node != null) {
				this.put(node.getKey(), node.getValue());
				node = node.up;
			}
		}
	}

	public static <K, V> StackMap<K, V> withFallback(Hash.Strategy<K> strategy, V fallback) {
		return new StackMap<>(strategy) {

			@Override
			public V getFallback(K key) {
				return fallback;
			}
		};
	}

	public void checkIntegrity() {
		int nodeCount = 0, nodeOccupancy = 0;
		for (Node<K, V> node : this.table) {
			if (node != null) {
				nodeOccupancy++;
				while (true) {
					nodeCount++;
					assert node.stackFrame <= this.frameCount;
					Node<K, V> up = node.up;
					if (up != null) {
						assert up.down == node;
						node = up;
					}
					else {
						break;
					}
				}
			}
		}
		assert this.nodeCount == nodeCount;
		assert this.bucketOccupancy == nodeOccupancy;
		if (this.leftMost != null) {
			assert this.rightMost != null;
			assert nodeCount != 0;
			assert nodeOccupancy != 0;
			assert this.leftMost.left == null;
			assert this.rightMost.right == null;
			Node<K, V> node = this.leftMost;
			int nodeCount2 = 0;
			while (true) {
				nodeCount2++;
				Node<K, V> right = node.right;
				if (right != null) {
					assert right.left == node;
					node = right;
				}
				else {
					break;
				}
			}
			assert node == this.rightMost;
			assert nodeCount == nodeCount2;
		}
		else {
			assert this.rightMost == null;
			assert nodeCount == 0;
			assert nodeOccupancy == 0;
		}
	}

	public void push() {
		this.frameCount++;
		//this.checkIntegrity();
	}

	public void pop() {
		while (true) {
			Node<K, V> toRemove = this.rightMost;
			assert toRemove.right == null;
			if (toRemove.stackFrame < this.frameCount) break;
			assert toRemove.stackFrame == this.frameCount;
			int bucket = toRemove.keyHash & (this.table.length - 1);
			this.rightMost = toRemove.left;
			if (toRemove.left != null) {
				toRemove.left.right = null;
			}
			else {
				assert toRemove == this.leftMost;
				this.leftMost = null;
			}
			if (toRemove.down != null) {
				toRemove.down.up = toRemove.up;
			}
			else {
				this.table[bucket] = toRemove.up;
			}
			if (toRemove.up != null) {
				toRemove.up.down = toRemove.down;
			}
			toRemove.up   = null;
			toRemove.down = null;
			toRemove.left = null;

			this.nodeCount--;
			if (this.table[bucket] == null) this.bucketOccupancy--;
			//this.checkIntegrity();
		}
		this.frameCount--;
	}

	public void rehash() {
		for (Node<K, V> node = this.leftMost; node != null; node = node.right) {
			node.up = node.down = null;
		}
		for (Node<K, V> node = this.leftMost; node != null; node = node.right) {
			int bucket = node.keyHash & (this.table.length - 1);
			Node<K, V> curr = this.table[bucket];
			if (curr != null) {
				node.up = curr;
				curr.down = node;
			}
			else {
				this.bucketOccupancy++;
			}
			this.table[bucket] = node;
		}
	}

	public Node<K, V> getNode(K key) {
		int hashCode = HashCommon.mix(this.strategy.hashCode(key));
		int bucket = hashCode & (this.table.length - 1);
		return this.getNode(key, hashCode, bucket);
	}

	public Node<K, V> getNode(K key, int hashCode, int bucket) {
		Node<K, V> result = null;
		for (Node<K, V> node = this.table[bucket]; node != null; node = node.up) {
			if (result == null || node.stackFrame > result.stackFrame) {
				if (node.keyHash == hashCode && this.strategy.equals(node.key, key)) {
					if ((result = node).stackFrame == this.frameCount) {
						break;
					}
				}
			}
		}
		return result;
	}

	public V get(K key) {
		Node<K, V> node = this.getNode(key);
		return node != null ? node.getValue() : this.getFallback(key);
	}

	public V put(K key, V value) {
		int hashCode = HashCommon.mix(this.strategy.hashCode(key));
		int bucket = hashCode & (this.table.length - 1);
		Node<K, V> existing = this.getNode(key, hashCode, bucket);
		if (existing != null && existing.stackFrame == this.frameCount) {
			return existing.setValue(value);
		}
		Node<K, V> newNode = new Node<>(key, value, hashCode, this.frameCount);
		newNode.left = this.rightMost;
		if (this.rightMost != null) this.rightMost.right = newNode;
		else this.leftMost = newNode;
		this.rightMost = newNode;
		Node<K, V> bottom = this.table[bucket];
		newNode.up = bottom;
		if (bottom != null) bottom.down = newNode;
		this.table[bucket] = newNode;
		this.nodeCount++;
		if (bottom == null && ++this.bucketOccupancy > (this.table.length * 3) >> 2) {
			//this.checkIntegrity();
			this.table = new Node[this.table.length << 1];
			this.bucketOccupancy = 0;
			this.rehash();
		}
		//this.checkIntegrity();
		return existing != null ? existing.getValue() : this.getFallback(key);
	}

	public V getFallback(K key) {
		return null;
	}

	public static class Node<K, V> implements Map.Entry<K, V> {

		public final K key;
		public V value;
		public final int keyHash, stackFrame;
		public Node<K, V> up, down, left, right;

		public Node(K key, V value, int keyHash, int stackFrame) {
			this.key = key;
			this.value = value;
			this.keyHash = keyHash;
			this.stackFrame = stackFrame;
		}

		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(V newValue) {
			V oldValue = this.value;
			this.value = newValue;
			return oldValue;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (
				obj instanceof Map.Entry<?, ?> that &&
				Objects.equals(this.getKey(), that.getKey()) &&
				Objects.equals(this.getValue(), that.getValue())
			);
		}

		@Override
		public String toString() {
			return this.key + " -> " + this.value;
		}
	}
}