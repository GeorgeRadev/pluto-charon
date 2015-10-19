package pluto.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Rotating queue of fixed size.
 */
public class RotatingQueue<T> implements Iterable<T> {

	private List<T> queue;
	private int mostRecentItem;
	private int size;

	public RotatingQueue(int capacity) {
		size = capacity + 1;
		queue = new ArrayList<T>(size);
		mostRecentItem = capacity - 1;
		for (int i = 0; i < size; i++) {
			queue.add(null);
		}
	}

	/**
	 * Inserts an element to the head of the queue, pushing all other elements
	 * one position forward.
	 * 
	 * @param element
	 */
	public synchronized void add(T element) {
		// Get index
		mostRecentItem = nextPointer(mostRecentItem, size);
		queue.set(mostRecentItem, element);
		queue.set(nextPointer(mostRecentItem, size), null);
	}

	public T get(int index) {
		// Normalize index to size of queue
		index = (mostRecentItem + size - index) % size;
		return queue.get(index);
	}

	public int getCapacity() {
		return size - 1;
	}

	private static final int nextPointer(int oldPointer, int size) {
		int pointer = oldPointer + 1;
		if (pointer < size) {
			return pointer;
		} else {
			return 0;
		}
	}

	public Iterator<T> iterator() {
		return new RotatingQueueIterator(mostRecentItem);
	}

	public final class RotatingQueueIterator implements Iterator<T> {
		int currentPointer;

		RotatingQueueIterator(int mostRecentItem) {
			currentPointer = mostRecentItem;
		}

		public boolean hasNext() {
			return queue.get(currentPointer) != null;
		}

		public T next() {
			T result = queue.get(currentPointer);
			currentPointer--;
			if (currentPointer < 0) {
				currentPointer += size;
			}
			return result;
		}

		public void remove() {}
	}
}
