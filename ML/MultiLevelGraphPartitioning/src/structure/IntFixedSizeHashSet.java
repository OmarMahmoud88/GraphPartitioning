package structure;

import java.util.ArrayList;
import java.util.HashSet;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class IntFixedSizeHashSet{
	private int queueSize;
	private IntOpenHashSet queueSearcher;
	private IntArrayList queueModifier;

	public IntFixedSizeHashSet(int queueSize) {
		// queue Size must be greater than zero
		if (queueSize < 1) {
			throw new IndexOutOfBoundsException("Custom Exception: Queue Size must be positive number.");
		}
		this.queueSize = queueSize;
		this.queueSearcher = new IntOpenHashSet(queueSize);
		this.queueModifier = new IntArrayList(queueSize);
	}

	public boolean addItem(int item) {
		if (queueSearcher.contains(item)) {
			return false;
		}
		queueSearcher.add(item);
		queueModifier.add(0, item);

		if (queueModifier.size() > this.queueSize) {
			// remove first added item
			int removedItem = queueModifier.getInt(0);
			queueSearcher.remove(removedItem);
			queueModifier.removeInt(0);
		}
		return true;
	}

	public boolean itemExists(int item) {
		return queueSearcher.contains(item);
	}

	public int getQueueSize() {
		return queueModifier.size();
	}

	public void clear() {
		this.queueSearcher.clear();
		this.queueModifier.clear();
	}

}
