package structure;

import java.util.ArrayList;
import java.util.HashSet;

public class FixedSizeHashSet<T> {
	private int queueSize;
	private HashSet<T> queueSearcher;
	private ArrayList<T> queueModifier;

	public FixedSizeHashSet(int queueSize) {
		// queue Size must be greater than zero
		if (queueSize < 1) {
			throw new IndexOutOfBoundsException("Custom Exception: Queue Size must be positive number.");
		}
		this.queueSize = queueSize;
		this.queueSearcher = new HashSet<T>(queueSize);
		this.queueModifier = new ArrayList<>(queueSize);
	}

	public boolean addItem(T item) {
		if (queueSearcher.contains(item)) {
			return false;
		}
		queueSearcher.add(item);
		queueModifier.add(0, item);

		if (queueModifier.size() > this.queueSize) {
			// remove first added item
			T removedItem = queueModifier.get(0);
			queueSearcher.remove(removedItem);
			queueModifier.remove(0);
		}
		return true;
	}

	public boolean itemExists(T item) {
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
