package com.zacleung.util;

public abstract class MyMath {
	public static double max(double d1, double d2, double d3) {
		return Math.max(d1, Math.max(d2, d3));
	}

	/**
	 * Take a modulo b and return a positive number.
	 * The default Java modulo sets -3 % 4 = -3.  This modulo
	 * function returns 1. 
	 * @param a
	 * @param b
	 * @return a positive number in 0,...,b-1
	 */
	public static int positiveModulo(int a, int b)
			throws IllegalArgumentException {
		if (b < 0) {
			throw new IllegalArgumentException("b = " + b + " is negative.");
		}
		return (a % b + b) % b;
	}

	public static int[] range(int start, int end, int increment) {
		if (start < end && increment < 0) throw new IllegalArgumentException();
		if (start > end && increment > 0) throw new IllegalArgumentException();

		int[] values = new int[Math.abs((end - start) / increment) + 1];
		boolean reverse = start > end;

		for (int i = start, index = 0; reverse ? (i >= end) : (i <= end); i+=increment, ++index)
		{
			values[index] = i;
		}
		return values;
	}
}
