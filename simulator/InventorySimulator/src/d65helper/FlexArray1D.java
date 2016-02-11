package d65helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class FlexArray1D<T> {
	private int base;
	private int ceil;
	private ArrayList<T> values;
	
	public FlexArray1D (int base, int ceil) {
		this.base = base;
		this.ceil = ceil;
		values = new ArrayList<T>(ceil - base);
		for (int i = 0; i < ceil - base; ++i) {
			values.add(i, null);
		}
	}

	public T get(int i) throws IllegalArgumentException {
		checkIndex(i);
		return values.get(i - base);
	}
	
	public void set(int i, T t) throws IllegalArgumentException {
		checkIndex(i);
		values.set(i - base, t);
	}

	public void checkIndex(int i) throws IllegalArgumentException {
		if (i < base || i >= ceil) {
			throw new IllegalArgumentException(
					"Index i = " + i + " is invalid in base = " + base
					+ " ceil = " + ceil);
		}
	}
	
	/**
	 * Test that the FlexArray1D class works correctly.
	 * @param args
	 */
	public static void main(String[] args) {
		LinkedList<Integer> list = new LinkedList<>();
		list.add(3);
		list.add(5);
		for (Iterator<Integer> itr = list.iterator(); itr.hasNext(); ) {
			int k = itr.next();
			System.out.println(k);
		}
		
		int iStart = -5;
		int iEnd = 10;
		FlexArray1D<Integer> array = new FlexArray1D<>(iStart, iEnd);
		for (int i = iStart; i < iEnd; ++i) {
			array.set(i, i);
			System.out.println(array.get(i));
		}
	}

}
