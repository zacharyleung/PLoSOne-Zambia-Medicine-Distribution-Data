package invsim3;

import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.distribution.LogNormalDistribution;

import d65helper.FlexArray1D;

public class CalibrateForecasts {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		changeVariance();
		//plotPdf();
	}
	
	public static void changeVariance() {
		int nReps = 10000;

		double[] mean = {10000};
		double[] forecastVariance = {
		        0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
		        0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
		        0.0425, 0.045, 0.0475, 0.065};
		double[] forecastAccuracy = {0.0, 1.0};
		int forecastLevel = 1;

		// Scale down forecast variance so the sum is 0.2231
		for (int i = 0; i < forecastVariance.length; ++i)
			forecastVariance[i] *= 0.2231 / 0.5000;
		
		double sumVar = 0;
		for (int i = 0; i < forecastVariance.length; ++i)
			sumVar += forecastVariance[i];
		System.out.format("Sum of forecast variance = %.4f", sumVar);
		System.out.println();
		
		MultMmfeDemand demand = new  MultMmfeDemand(mean, forecastVariance,
				forecastAccuracy, forecastLevel);

		FlexArray1D<Double> flex = new FlexArray1D<>(-3, 2);
		
		for (int m = -3; m <= 0; ++m) {
			SummaryStatistics stats = new SummaryStatistics();
			for (int r = 0; r < nReps; ++r) {
				demand.generate(-12, 4, r);
				double f = 0;
				for (int t = 0; t < 4; ++t)
					f += demand.getForecast(4 * m, t + 1 - 4 * m).mean[t - 4 * m];
				double d = 0;
				for (int t = 0; t < 4; ++t)
					d += demand.getDemand(t, 1)[0];
				stats.addValue(Math.log(f / d));
			}
			double var = stats.getVariance();
			flex.set(m, var);
			System.out.format("%d-month ahead variance = %.4f", -m, var);
			System.out.println();
		}
		
		flex.set(1, 0.0);
		for (int m = -3; m <= 0; ++m) {
			double d = (flex.get(m) - flex.get(m + 1)) / (flex.get(-3));
			System.out.format("Percentage resolved %d months ahead = %.1f%%",
					-m, 100 * d);
			System.out.println();
		}
	}
	
	public static void plotPdf() throws Exception {
		double variance = 0.2231;
		double mean = -variance / 2;
		double std = Math.sqrt(variance);
		
		PrintStream out = new PrintStream(new FileOutputStream("lognormal.txt"));
		
		LogNormalDistribution lognorm = new LogNormalDistribution(mean, std);
		
		out.println("# invsim3.CalibrateForecasts.plotPdf()");
		out.println("# Plotting pdf of lognormal distribution");
		out.format("# variance of normal = %.4f", variance);
		out.println();
		out.format("# mean of normal = %.4f", mean);
		out.println();
		out.format("# mean of lognormal = %.4f", lognorm.getNumericalMean());
		out.println();
		out.format("# std dev of lognormal = %.4f", Math.sqrt(lognorm.getNumericalVariance()));
		out.println();
		out.println("x\tPDF\tCDF");
		for (double d = 0.0; d < 3.0; d += 0.01) {
			out.format("%.4f\t%.3e\t%.3e", d, lognorm.density(d),
					lognorm.cumulativeProbability(d));
			out.println();
		}
		out.close();
	}
	
	public static double[] getForecastVariance() {
		double[] forecastVariance = {
		        0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
		        0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
		        0.0425, 0.045, 0.0475, 0.065};

		// Scale down forecast variance so the sum is 0.2231
		for (int i = 0; i < forecastVariance.length; ++i)
			forecastVariance[i] *= 0.2231 / 0.5000;

		
		return forecastVariance;
	}
	

}
