package com.zacleung.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public abstract class Vector {
	public static double[] readDouble(String inFile) throws Exception {
		Scanner s = null;
		double[] d = null;

		try {
			s = new Scanner(new BufferedReader(new FileReader(inFile)));
			int n = s.nextInt();
			d = new double[n];
			for (int i = 0; i < n; ++i)
				d[i] = s.nextDouble();
			return d;
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

	/**
	 * Scale a vector so that it becomes longer or shorter.
	 * 
	 * Example 1:
	 * vector = {2, 4};
	 * newLength = 4;
	 * keepSumConstant = false
	 * output = {2, 2, 4, 4};
	 * 
	 * Example 2:
	 * vector = {2, 4};
	 * newLength = 4;
	 * keepSumConstant = true
	 * output = {1, 1, 2, 2};
	 * 
	 * @param vector
	 * @param newLength
	 */
	public static double[] scaleVector(double[] vector, int newLength,
			boolean keepSumConstant) {
		int oldLength = vector.length;
		double[] cut = new double[oldLength * newLength];
		double[] out = new double[newLength];

		for (int i = 0, k = 0; i < oldLength; ++i) {
			for (int j = 0; j < newLength; ++j, ++k) {
				if (keepSumConstant) {
					cut[k] = vector[i] / newLength;
				} else {
					cut[k] = vector[i] / oldLength;
				}
			}
		}
		// In the example, cut should be {2 2 2 2 4 4 4 4}

		for (int i = 0, k = 0; i < newLength; ++i) {
			for (int j = 0; j < oldLength; ++j, ++k) {
				out[i] += cut[k];
			}
		}

		// Debug
		//		System.out.print("Input vector = ");
		//		for (int i = 0; i < vector.length; ++i) {
		//			System.out.printf("%.2f\t", vector[i]);
		//		}
		//		System.out.println();
		//		System.out.println("keep sum constant = " + keepSumConstant);
		//		System.out.print("Scaled vector = ");
		//		for (int i = 0; i < out.length; ++i) {
		//			System.out.printf("%.2f\t", out[i]);
		//		}
		//		System.out.println();

		return out;
	}

	/**
	 * A circular shift is the operation of rearranging the entries
	 * in a tuple, either by moving the final entry to the first
	 * position, while shifting all other entries to the next position.
	 * http://en.wikipedia.org/wiki/Circular_shift
	 * @param array
	 * @param shift in [0, array.length]
	 * @return
	 */
	public static double[] circularShiftLeft(double[] array, int shift) {
		int N = array.length;

		if (shift < 0 || shift > N)
			throw new IllegalArgumentException(
					String.format("Shift = %d is not valid for array of length = %d!",
							shift, N));

		double[] result = new double[N];
		for (int n = 0; n < N - shift; ++n)
			result[n] = array[n + shift];
		for (int n = N - shift; n < N; ++n)
			result[n] = array[n + shift - N];

		return result;
	}

	public static void printVector(int[] array) {
		for (int i = 0; i < array.length; ++i)
			System.out.print(array[i] + "\t");
		System.out.println();
	}

	public static void printVector(double[] array) {
		for (int i = 0; i < array.length; ++i) {
			System.out.printf("%d\t%.2f%n", i, array[i]);
		}
	}

	public static boolean isNonDecreasing(int[] array) {
		for (int i = 0; i < array.length - 1; ++i)
			if (array[i] > array[i + 1])
				return false;
		return true;
	}

	/**
	 * Scale the seasonality of a vector.
	 * @param in double array to be scaled
	 * @param seasonality desired ratio of max_i array[i] / min_i array[i]
	 * @return scaled double array
	 * @throws IllegalArgumentException
	 */
	public static double[] scaleSeasonality(double[] in, double seasonality)
			throws IllegalArgumentException {
		if (seasonality < 1.0)
			throw new IllegalArgumentException(
					String.format("Seasonality %.2f cannot be less than 1.0!", seasonality));

		RealVector v = new ArrayRealVector(in);

		// Do affine combintion to get demand seasonality equal to
		// desired factor
		// Average demand per period
		RealVector e = v.copy();
		e.set(1);
		double d0 = v.dotProduct(e) / v.getDimension();
		double dmax = v.getMaxValue();
		double dmin = v.getMinValue();
		double sigma = seasonality;
		double alpha = (sigma - 1) * d0 / 
				(dmax + (sigma - 1) * d0 - sigma * dmin);
		RealVector d0vector = v.copy();
		d0vector.set(d0);
		v = v.mapMultiply(alpha).add(d0vector.mapMultiply(1 - alpha));

		return v.toArray();
	}
}
