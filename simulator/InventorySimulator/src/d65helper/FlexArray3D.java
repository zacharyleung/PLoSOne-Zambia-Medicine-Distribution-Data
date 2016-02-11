package d65helper;

import java.util.ArrayList;

public class FlexArray3D<T> {
	private ArrayList<ArrayList<ArrayList<T>>> values;
	private int base1;
	private int base2;
	private int base3;
	private int ceil1;
	private int ceil2;
	private int ceil3;
	
	public FlexArray3D(int base1, int ceil1, int base2, int ceil2,
			int base3, int ceil3) {
		this.base1 = base1;
		this.base2 = base2;
		this.base3 = base3;
		this.ceil1 = ceil1;
		this.ceil2 = ceil2;
		this.ceil3 = ceil3;
		
		values = new ArrayList<>(ceil1 - base1);
		for (int i = 0; i < ceil1 - base1; ++i) {
			values.add(i, new ArrayList<ArrayList<T>>(ceil2 - base2));
			for (int j = 0; j < ceil2 - base2; ++j) {
				values.get(i).add(j, new ArrayList<T>(ceil3 - base3));
				for (int k = 0; k < ceil3 - base3; ++k) {
					values.get(i).get(j).add(k, null);
				}
			}
		}
	}
	

	public T get(int i, int j, int k) throws IllegalArgumentException {
		checkIndex(i, j, k);
		//System.out.println(i + " " + j + " " + k);
		return values.get(i - base1).get(j - base2).get(k - base3);
	}
	
	public void set(int i, int j, int k, T t) throws IllegalArgumentException {
		checkIndex(i, j, k);
		values.get(i - base1).get(j - base2).set(k - base3, t);
	}

	public void checkIndex(int i, int j, int k)
			throws IllegalArgumentException {
		if (i < base1 || i >= ceil1) {
			throw new IllegalArgumentException(
					"Index i = " + i + " is invalid in base1 = " + base1
					+ " ceil1 = " + ceil1);
		} else if (j < base2 || j >= ceil2) {
			throw new IllegalArgumentException(
					"Index j = " + j + " is invalid in base2 = " + base2
					+ " ceil2 = " + ceil2);
		} else if (k < base3 || k >= ceil3) {
			throw new IllegalArgumentException(
					"Index k = " + k + " is invalid in base3 = " + base3
					+ " ceil3 = " + ceil3);
		}
	}
//	
//	public static void main(String[] args) {
//		FlexArray3D<Integer> array3d = new FlexArray3D<>(-2, 0, -2, 0, -2, 0);
//		for (int i = -2; i < 0; ++i) {
//			for (int j = -2; j < 0; ++j) {
//				for (int k = -2; k < 0; ++k) {
//					array3d.set(i, j, k, -i * 100 + -j * 10 + -k);
//					System.out.println(i + " " + j + " " + k);
//					System.out.println(array3d.get(i, j, k));
//				}
//			}
//		}
//	}
	
}
