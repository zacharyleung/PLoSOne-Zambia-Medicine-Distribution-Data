package d65helper;

import java.util.ArrayList;

/**
 * A Flexible Array allows the base of the array to be something
 * other than zero.
 * The base and the ceiling of the array has to be defined at the
 * time the FlexArray is created.
 * 
 * @author Zack
 *
 * @param <T> the type of value stored in the FlexArray.
 */
public class FlexArray<T> {
	private final int base;
	private final int ceiling;
	
	/**
	 * Intuitively, array[0] is FlexArray[base].
	 * Though this is not correct syntax.
	 */
	private ArrayList<T> array;
	
	/**
	 * The FlexArray stores entries from base up to ceiling-1
	 * inclusive.
	 * 
	 * @param base
	 * @param ceiling
	 * @throws IllegalArgumentException
	 */
	public FlexArray (int base, int ceiling) throws IllegalArgumentException {
		if (ceiling < base)
			throw new IllegalArgumentException(
					"ceiling " + ceiling + " is less than base " + base);
		
		this.base = base;
		this.ceiling = ceiling;
		array = new ArrayList<T>(ceiling - base);
		for (int i = 0; i < ceiling - base; ++i)
			array.add(null);
	}
	
	public void set(int i, T t) throws IllegalArgumentException {
//		//System.out.println(i + " " + base);
		checkIndex(i);
		array.set(i-base, t);
	}
	
	public T get(int i) throws IllegalArgumentException {
		checkIndex(i);
		return array.get(i-base);
	}
	
	private void checkIndex(int i) throws IllegalArgumentException {
		if (i < base)
			throw new IllegalArgumentException(
					"i " + i + " is less than base " + base + "!");
		else if (i >= ceiling )
			throw new IllegalArgumentException(
					"i " + i + " is more than ceiling " + ceiling + "!");
	}
}
