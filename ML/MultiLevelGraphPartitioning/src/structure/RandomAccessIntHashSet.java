package structure;

import java.util.Random;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

public class RandomAccessIntHashSet {

	IntArrayList dta = new IntArrayList();
	Int2IntOpenHashMap idx = new Int2IntOpenHashMap();

	public RandomAccessIntHashSet() {
		idx.defaultReturnValue(-1);
	}

	public RandomAccessIntHashSet(IntArrayList items) {
		idx.defaultReturnValue(-1);
		for (int item : items) {
			idx.put(item, dta.size());
			dta.add(item);
		}
	}

	public boolean add(int item) {
		if (idx.containsKey(item)) {
			return false;
		}
		idx.put(item, dta.size());
		dta.add(item);
		return true;
	}

	public boolean contains(int item) {
		return idx.containsKey(item);
	}

	public int removeAt(int id) {
		if (id >= dta.size()) {
			throw new IndexOutOfBoundsException("Index " + id + " is out of bounds!");
		}
		int res = dta.getInt(id);
		idx.remove(res);
		int last = dta.removeInt(dta.size() - 1);
		// skip filling the hole if last is removed
		if (id < dta.size()) {
			idx.put(last, id);
			dta.set(id, last);
		}
		return res;
	}

	public boolean remove(int item) {
		int id = idx.get(item);
		if (id == -1) {
			return false;
		}
		removeAt(id);
		return true;
	}

	public int get(int i) {
		return dta.getInt(i);
	}

	public int pollRandom(Random rnd) {
		if (dta.isEmpty()) {
			return -1;
		}
		int id = rnd.nextInt(dta.size());
		return removeAt(id);
	}

	public int size() {
		return dta.size();
	}

	public IntListIterator iterator() {
		return dta.listIterator();
	}

	public void addAll(RandomAccessIntHashSet randomAccessIntHashSet) {
		IntListIterator it = randomAccessIntHashSet.iterator();
		while (it.hasNext()) {
			int intValue = it.next();
			this.add(intValue);
		}

	}
}
