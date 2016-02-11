package invsim;

import java.util.Arrays;

import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import d65helper.FlexArray1D;

public class MultMmfe1Period {
	private double[] var;
	/** Length of forecast horizon */
	private int M;
	private FlexArray1D<Double> z;

	/**
	 * Multiplicative MMFE demand model to forecast the demand
	 * at period 0.
	 * @param var var[m] is the variance of the normal random
	 * variable which is revealed m periods in advance.
	 */
	public MultMmfe1Period(double[] var, int randomSeed) {
		this.var = Arrays.copyOf(var, var.length);
		M = var.length;

		RandomDataImpl rand = new RandomDataImpl();
		rand.reSeed(randomSeed);

		z = new FlexArray1D<>(-M+1, 1);
		for (int u = -M+1; u <= 0; ++u) {
			double sigma = Math.sqrt(var[-u]);
			double mu = -var[-u] / 2;
			double d = rand.nextGaussian(mu, sigma);
			z.set(u, d);
		}
	}

	/**
	 * Get the demand forecast at the beginning of period t.
	 * @param t
	 */
	public double getForecast(int t) {
		double logmean = 0;
		double logvar = 0;
		double demandMean = 100;
		int tOld = t;
		// If the t is before -M + 1, the forecast is the same
		// as that at period -M + 1 as no info has been revealed
		if (t < -M + 1) {
			t = -M + 1;
		}
		for (int u = -M + 1; u < t; ++u) {
			logmean += z.get(u);
		}
		for (int u = t; u <= 0; ++u) {
			logvar += var[-u];
		}
		System.out.println("Period " + tOld);
		System.out.format("Demand forecast mean = %.1f\n",
				demandMean * Math.exp(logmean));
		System.out.format("Demand forecast std = %.1f\n",
				demandMean * Math.sqrt(Math.exp(logvar) - 1));
		System.out.println();

		return demandMean * Math.exp(logmean);
	}

	/**
	 * Test that the forecast generation works correctly.
	 * @param args
	 */
	public static void main(String[] args) {

		double[] var = {0.25, 0.16, 0.09};
		int M = var.length;
		int S = 1000;

		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int s = 0; s < S; ++s) {
			MultMmfe1Period mmfe = new MultMmfe1Period(var, s);

			for (int t = -M; t <= 1; ++t) {
				mmfe.getForecast(t);
			}

			// Add the data from the array
			stats.addValue(
					Math.log(mmfe.getForecast(1) / mmfe.getForecast(0)));
		}

		// Compute some statistics
		double mean = stats.getMean();
		double std = stats.getStandardDeviation();
		double median = stats.getPercentile(50);
		
		System.out.println("Mean = " + mean);
		System.out.println("Std = " + std);
		System.out.println("Theoretical std = " + Math.sqrt(0.25));
	}



}
