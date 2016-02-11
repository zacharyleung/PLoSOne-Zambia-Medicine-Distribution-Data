package com.zacleung.util;

import java.util.Arrays;

public abstract class ArrayUtils {
	/**
	 * Reverse sort a double array in place.
	 * @param d
	 * @return
	 */
	public static void reverseSort(double[] d) {
		Arrays.sort(d);
		double temp;
		for (int i = 0; i < Math.floor(d.length / 2); i++) {
			temp = d[i];
			d[i] = d[d.length - 1 - i];
			d[d.length - 1 - i] = temp;
		}
	}
	
	/**
	 * Return the sum of the array.
	 * @param d
	 * @return
	 */
	public static double sum(double[] d) {
		double sum = 0;
		for (int i = 0; i < d.length; ++i)
			sum += d[i];
		return sum;
	}
	
	/**
	 * Return the sum of two arrays.
	 * @param a
	 * @param b
	 * @return
	 */
	public static double[] sum(double[] a, double[] b) {
		double[] d = new double[a.length];
		for (int i = 0; i < a.length; ++i)
			d[i] = a[i] + b[i];
		return d;
	}
	
	/**
	 * Check if an array is in ascending order.
	 * Example: {0, 1, 1, 2} returns true
	 * Example: {2, 1, 0} returns false
	 * @param a
	 * @return
	 */
	public static boolean isAscending(int[] a) {
		for (int i = 0; i <= a.length - 2; ++i) {
			if (a[i] > a[i + 1]) {
				return false;
			}
		}
		return true;
	}
	
	public static double[][] copyOf(double[][] d) {
		double[][] a = new double[d.length][];
		for (int i = 0; i < a.length; ++i) {
			a[i] = copyOf(d[i]);
		}
		
		return a;
	}
	
	public static double[] copyOf(double[] d) {
		return Arrays.copyOf(d, d.length);
	}
	
	public static String toString(double[] d, String format) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		String pre = "";
		for (int i = 0; i < d.length; ++i) {
			sb.append(pre);
			sb.append(String.format(format, d[i]));
			pre = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
}
