package d65helper;

/**
 * A two-dimensional flexible array that stores longs.
 * A flexible array allows the base of the array to be a
 * number rather than 0.
 * 
 * @author Zack
 *
 */
public class FlexArrayLong {
	public final int base1;
	public final int ceil1;
	public final int base2;
	public final int ceil2;
	private long[][] array;
	
	public FlexArrayLong(int base1, int ceil1, int base2, int ceil2) {
		this.base1 = base1;
		this.ceil1 = ceil1;
		this.base2 = base2;
		this.ceil2 = ceil2;
		
		if (base1 > ceil1 || base2 > ceil2)
			throw new IllegalArgumentException(
					base1 + " " + ceil1 + " " + base2 + " " + ceil2);
		
		array = new long[ceil1-base1][ceil2-base2];
	}
	
	public long get(int i, int j) {
		return array[i-base1][j-base2];
	}
	
	public void set(int i, int j, long value) {
		array[i-base1][j-base2] = value;
	}
}
