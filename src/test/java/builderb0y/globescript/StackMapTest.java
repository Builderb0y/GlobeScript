package builderb0y.globescript;

import java.util.*;

public class StackMapTest {

	public static void main(String[] args) {
		StackMap<Object, Object> fastMap = new StackMap<>(Util.defaultHashStrategy());
		SlowMap<Object, Object> slowMap = new SlowMap<>();
		List<Object> pool = new ArrayList<>();
		int stackSize = 1;
		SplittableRandom random = new SplittableRandom(12345L);
		for (int attempt = 1; attempt <= 1_000_000; attempt++) {
			int action = random.nextInt(4);
			//System.out.print((char)(action + '0'));
			if (random.nextDouble() < Math.pow(attempt, -0.75D)) {
				pool.add(new Object());
				System.out.println("attempt " + attempt + "; new pool size: " + pool.size());
			}
			switch (action) {
				case 0 -> {
					fastMap.push();
					slowMap.push();
					stackSize++;
				}
				case 1 -> {
					if (stackSize > 1) {
						fastMap.pop();
						slowMap.pop();
						stackSize--;
					}
				}
				case 2, 3 -> {
					Object from = pool.get(random.nextInt(pool.size()));
					Object to = pool.get(random.nextInt(pool.size()));
					Object fast = fastMap.put(from, to);
					Object slow = slowMap.put(from, to);
					if (fast != slow) {
						throw new IllegalStateException();
					}
				}
			}
			for (Object key : pool) {
				if (fastMap.get(key) != slowMap.get(key)) {
					throw new IllegalStateException();
				}
			}
		}
	}

	public static class SlowMap<K, V> {

		public List<Map<K, V>> maps = new ArrayList<>();

		public SlowMap() {
			this.maps.add(new HashMap<>());
		}

		public void push() {
			this.maps.add(new HashMap<>(this.maps.getLast()));
		}

		public void pop() {
			this.maps.removeLast();
		}

		public V get(K key) {
			return this.maps.getLast().get(key);
		}

		public V put(K key, V value) {
			return this.maps.getLast().put(key, value);
		}
	}
}