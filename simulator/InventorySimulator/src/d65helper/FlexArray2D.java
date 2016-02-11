package d65helper;

import java.util.ArrayList;

public class FlexArray2D<T> {
	private ArrayList<ArrayList<T>> values;
	private int base1;
	private int base2;
	private int ceil1;
	private int ceil2;

	public FlexArray2D(int base1, int ceil1, int base2, int ceil2) {
		this.base1 = base1;
		this.base2 = base2;
		this.ceil1 = ceil1;
		this.ceil2 = ceil2;

		values = new ArrayList<>(ceil1 - base1);
		for (int i = 0; i < ceil1 - base1; ++i) {
			values.add(i, new ArrayList<T>(ceil2 - base2));
			for (int j = 0; j < ceil2 - base2; ++j) {
				values.get(i).add(j, null);
			}
		}
	}


	public T get(int i, int j) throws IllegalArgumentException {
		checkIndex(i, j);
		//System.out.println(i + " " + j + " " + k);
		return values.get(i - base1).get(j - base2);
	}

	public void set(int i, int j, T t) throws IllegalArgumentException {
		checkIndex(i, j);
		values.get(i - base1).set(j - base2, t);
	}

	public void checkIndex(int i, int j)
			throws IllegalArgumentException {
		if (i < base1 || i >= ceil1) {
			throw new IllegalArgumentException(
					"Index i = " + i + " is invalid in base1 = " + base1
					+ " ceil1 = " + ceil1);
		} else if (j < base2 || j >= ceil2) {
			throw new IllegalArgumentException(
					"Index j = " + j + " is invalid in base2 = " + base2
					+ " ceil2 = " + ceil2);
		}
	}

	public static void main(String[] args) {
		FlexArray2D<Integer> array2d = new FlexArray2D<>(-3, 0, -3, 0);
		for (int i = -3; i < 0; ++i) {
			for (int j = -3; j < 0; ++j) {
				array2d.set(i, j, -i * 10 + -j * 1);
				System.out.println(i + " " + j);
				System.out.println(array2d.get(i, j));
			}
		}
	}

}
