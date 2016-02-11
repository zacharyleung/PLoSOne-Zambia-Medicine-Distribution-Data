package d65helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class Helper {
	public static double getConfidenceIntervalWidth(
			SummaryStatistics summaryStatistics, double significance)
					throws MathException {
		TDistributionImpl tDist = new TDistributionImpl(summaryStatistics.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1.0 - significance/2);
		return a * summaryStatistics.getStandardDeviation() /
				Math.sqrt(summaryStatistics.getN());
	}


	/**
	 * The default Java modulo sets -3 % 4 = -3.  This modulo
	 * function returns 1. 
	 * @param a
	 * @param b
	 * @return a positive number in 0,...,b-1
	 */
	public static int modulo(int a, int b) {
		return (a % b + b) % b;
	}

	/**
	 * Generate a random number from the given pmf. 
	 * @param rand
	 * @param pmf
	 * @return
	 */
	public static int randomPmf(RandomDataImpl rand, double[] pmf) {
		double u = rand.nextUniform(0, 1);
		double sum = 0;
		// The minimum lead time is at least 1 week.
		for (int k = 0; k < pmf.length; ++k) {
			sum += pmf[k];
			if (u < sum) {
				return k;
			}
		}

		return -1;
	}

	public static long randomRound(RandomDataImpl rand, double d) {
		double r = rand.nextUniform(0, 1);
		// Integer part
		long iPart = Math.round(Math.floor(d));
		// Fractional part
		double fPart = d - iPart;
		return iPart + (r < fPart ? 1 : 0);
	}

	public static void randomRoundTest() {
		RandomDataImpl rand = new RandomDataImpl();
		int sum = 0;
		for (int i = 0; i < 100000; ++i)
			sum += Helper.randomRound(rand,0.9);
		System.out.println(sum + "/100000");
	}

	public static void waitForUser() {
		try {
			//Create a scanner object  
			//Scanner is a predefined class in java that will be use to scan text  
			//System.in is mean, we will receive input from standard input stream  
			Scanner readUserInput=new Scanner(System.in);  

			//WAIT FOR USER INPUT  
			//After user put it's name, he or she must press 'Enter'  
			//After user press 'Enter', all the input contents will read into 'myName'  
			String myName=readUserInput.nextLine();  

			//Print what it store in myName  
			System.out.println(myName);
		} catch (Exception e) {}
	}

	public static boolean[] copyArray(boolean[] array) {
		return Arrays.copyOf(array, array.length);
	}

	public static int[] copyArray(int[] array) {
		return Arrays.copyOf(array, array.length);
	}

	public static double[] copyArray(double[] array) {
		return Arrays.copyOf(array, array.length);
	}

	/**
	 * If one and only one entry of array is equal to value, then
	 * delete that value from the array.
	 * @param array
	 * @param value
	 * @return
	 */
	public static int[] delete(int[] array, int value) {
		int[] newArray = new int[array.length - 1];
		int count = 0;
		for (int i = 0; i < array.length; ++i) {
			if (array[i] != value) {
				newArray[count] = array[i]; 
				count++;
			}
		}
		return newArray;
	}

	public static int readInt(File file) throws Exception {
		Scanner s = null;

		// Read the number of periods in a year
		try {
			s = new Scanner(new BufferedReader(new FileReader(file)));
			return s.nextInt();
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

	public static int[][] readInt2D(File file, int nRows, int nCols)
			throws Exception{
		Scanner s = null;
		int[][] d = new int[nRows][nCols];

		// Read the number of periods in a year
		try {
			s = new Scanner(new BufferedReader(new FileReader(file)));
			for (int i = 0; i < nRows; ++i)
				for (int j = 0; j < nCols; ++j)
					d[i][j] = s.nextInt();
			return d;
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

}

