package invsim;

import java.util.Arrays;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.correlation.Covariance;

import d65helper.FlexArray;
import d65helper.FlexArray1D;
import d65helper.Helper;


/**
 * This object generates demand according to the additive MMFE
 * demand model.
 *  
 * @author zacleung
 *
 */
public class AdditiveMmfeDemand extends Demand {
	private RealMatrix cMatrix;
	/** The number of periods in the forecast vector. */ 
	private int M;
	
	/** Generate demand for periods [tStart, tEnd). */
	private int tStart;
	/** Generate demand for periods [tStart, tEnd). */
	private int tEnd;
	
	private FlexArray<double[][]> epsilon;
	/** Normal random variables for rounding demand to integers. */
	private FlexArray1D<Double> zRound;
	/** Demand mean. */
	private FlexArray1D<Double> mean;
	
	private double[][] cov;
	
	/**
	 * forecastAccuracy[k] = forecast accuracy for level k,
	 * which is a number in [0,1], which gives the proportion of
	 * the epsilon which is known by the forecaster.
	 * The forecast accuracy levels are strictly increasing with k. 
	 * By convention, forecastAccuracy[0] = 0 and
	 * forecastAccuracy[K] = 1.
	 */
	private double[] forecastAccuracy;
	
	/**
	 * The weight of forecast variable k
	 *   = forecastAccuracy[k + 1] - forecastAccuracy[k];
	 * We need to scale C * Z by sqrt(forecastWeight[k]) to get the
	 * k-th random variable.
	 */
	private double[] forecastWeight;
	
	/**
	 * forecastLevel is an integer in {0, 1, ..., K}.
	 * For k < forecastAccuracy.length, forecastAccuracy[k]
	 * proportion of epsilon is known. For k = forecastAccuracy.length,
	 * the demand is perfectly known beforehand.
	 */
	private int forecastLevel;
	
	/**
	 * Number of random variables to generate to get demand forecasts
	 * of varying accuracy.
	 */
	private int K;
	
	private final int Y;

	public AdditiveMmfeDemand(double[] mean, double[][] cov,
			double[] forecastAccuracy, int forecastLevel) {
		this.tStart = tStart;
		this.tEnd = tEnd;
		this.cov = cov;
		this.Y = mean.length;
		M = cov.length;

		// In case the demand mean is not long enough, make it cycle
		this.mean = new FlexArray1D<>(tStart, tEnd);
		for (int t = tStart; t < tEnd; ++t)
			this.mean.set(t, mean[Helper.modulo(t, mean.length)]);
		
		// The splitTolerance argument is a dummy parameter
		double splitTolerance = 1e-5;
		EigenDecompositionImpl eigen = new EigenDecompositionImpl(
				new Array2DRowRealMatrix(cov), splitTolerance);
		
		RealMatrix v = eigen.getV();
		RealMatrix d = eigen.getD();
		
		// Construct the square root of the matrix d
		double[][] d2DArray = d.copy().getData();
		// Take the square root of the diagonal elements
		for (int t = 0; t < M; ++t)
			d2DArray[t][t] = Math.sqrt(d2DArray[t][t]);
		RealMatrix dsqrt = new Array2DRowRealMatrix(d2DArray);
		
		cMatrix = dsqrt.preMultiply(v);
		// I want C * z, but because Apache can only do z' * C',
		// I need to take the transpose of C
		cMatrix = cMatrix.transpose();

		this.forecastAccuracy = forecastAccuracy;
		this.forecastLevel = forecastLevel;
		this.K = forecastAccuracy.length - 1;

		// Check that the first element of forecastAccuracy is 0,
		// and the last element is 1.
		if (forecastAccuracy[0] != 0) {
			throw new IllegalArgumentException(
					"forecastAccuracy[0] must be 0!");
		}
		if (forecastAccuracy[K] != 1) {
			throw new IllegalArgumentException(
					"forecastAccuracy[K] must be 1!");			
		}
		
		forecastWeight = new double[K];
		for (int k = 0; k < K; ++k) {
			forecastWeight[k] = 
					Math.sqrt(forecastAccuracy[k + 1] - forecastAccuracy[k]);
		}		
	}

	public String toString() {
		String out = "AdditiveMmfeDemand\n";
		out += "forecast accuracy = ";
		for (int i = 0; i < forecastAccuracy.length; ++i) {
			out += forecastAccuracy[i] + " ";
		}
		out += "\n";
		for (int t = -M+1; t < tEnd; ++t) {
			out += "\nepsilon[" + t + "] = ";
			for (int m = 0; m < M; ++m)
				out += epsilon.get(t)[m];
		}
		return out;
	}
	
	@Override
	public Forecast getForecast(int t, int nPeriods) {
		double[] myMean = new double[nPeriods];
		for (int k = 0; k < nPeriods; ++k) {
			myMean[k] = mean.get(t + k);
		}
		
		// The variance when no information has been revealed
		double fullVar = 0;
		for (int m = 0; m < M; ++m) {
			fullVar += cov[m][m]; 
		}
		
		double[] var = new double[nPeriods];
		for (int u = 0; u < nPeriods; ++u) {
			var[u] = fullVar;
		}
		
		// The uncertainty that has been revealed is up to period t-1
		for (int u = t - M + 1; u < t; ++u) {
			for (int v = Math.max(t, u); v < Math.min(u + M, tEnd); ++v) {
				//System.out.println(v - t);
				//System.out.println(v - u);
				//System.out.println(u + " " + v);
				for (int k = 0; k < forecastLevel; ++k) {
					myMean[v - t] += epsilon.get(u)[k][v - u];
					var[v - t] -= forecastWeight[k] * cov[v - u][v - u];
				}
			}
		}

		return new Forecast(myMean, var);
	}

	@Override
	public int[] getDemand(int t, int nPeriods) {
		double[] myMean = getDoubleDemand(t, nPeriods);
		
		int[] d = new int[nPeriods];
		for (int u = 0; u < nPeriods; ++u)
			d[u] = (int) (myMean[u] + zRound.get(t + u));
		
		return d;
	}
	
	/**
	 * 
	 * @param t
	 * @param nPeriods
	 * @return Demand mean during periods [t, t + nPeriods)
	 */
	public double[] getDoubleDemand(int t, int nPeriods) throws
			IllegalArgumentException {
		//System.out.format("getDoubleDemand(%d,%d)\n", t, nPeriods);
		checkLastPeriod(t + nPeriods);
		
		double[] myMean = new double[nPeriods];
		for (int k = 0; k < nPeriods; ++k) {
			myMean[k] = mean.get(t + k);
		}

		for (int u = t - M + 1; u < t + nPeriods; ++u) {
			int vStart = Math.max(t, u);
			int vEnd = Math.min(u + M, t + nPeriods);
			for (int v = vStart; v < vEnd; ++v) {
				//System.out.println(v - t);
				//System.out.println(v - u);
				//System.out.println(v + " " + u);
				for (int k = 0; k < K; ++k) {
					//System.out.println(v-t);
					//System.out.println(epsilon.get(u)[k].length);
					myMean[v - t] += epsilon.get(u)[k][v - u];
				}
			}
		}

		return myMean;
	}

	private void checkLastPeriod(int t) throws IllegalArgumentException {
		if (t > tEnd) {
			throw new IllegalArgumentException("Last period exceeded!");
		}
	}
	
	public static void main(String[] args) {
		testCovariance();
		testForecast();
	}
	
	/**
	 * Test that the forecast accuracy random number generation works
	 * correctly, by comparing the variance-covariance matrix that was
	 * specified with that which was generated.
	 */
	private static void testCovariance() {
		int tStart = -5;
		int tEnd = 10000;
		double[] mean = {10, 10, 10, 10};
		//double[][] cov = {{4}};
		double[][] cov = {{9, 0}, {0, 4}};
		double[] forecastAccuracy = {0, 0.2, 0.6, 1};
		AdditiveMmfeDemand demand = new AdditiveMmfeDemand(mean, cov,
				forecastAccuracy, 0);
		System.out.println(demand.cMatrix);
		
		demand.generate(0, tStart, tEnd);
		int[] d = demand.getDemand(tStart, tEnd);
		for (int t = 0; t < d.length; ++t)
			System.out.println(d[t]);
	}
	
	/**
	 * Test to see that the quality of the forecast declines with the
	 * number of weeks that we are forecasting ahead of time.
	 */
	private static void testForecast() {
		int tStart = -5;
		int tEnd = 10;
		double[] mean = {10};
		double[][] cov = {{9, 0, 0}, {0, 4, 0}, {0, 0, 1}};
		double[] forecastAccuracy = {0, 1};
		AdditiveMmfeDemand demand = new AdditiveMmfeDemand(mean, cov,
				forecastAccuracy, 1);
		
		demand.generate(0, tStart, tEnd);
		Forecast f = demand.getForecast(0, 10);
		for (int u = 0; u < tEnd; ++u) {
			System.out.format("%.1f\t%.1f\n", f.mean[u], f.var[u]);
		}
	}

	@Override
	public void generate(int randomSeed, int tStart, int tEnd) {
		RandomDataImpl rand = new RandomDataImpl();
		rand.reSeed(randomSeed);
		epsilon = new FlexArray<>(tStart-M+1, tEnd);
		for (int t = tStart-M+1; t < tEnd; ++t) {
			double[][] z = new double[K][];
			for (int k = 0; k < K; ++k) {
				double[] x = new double[M];
				for(int m = 0; m < M; ++m) {
					x[m] = rand.nextGaussian(0, 1) * Math.sqrt(forecastWeight[k]);
				}
				z[k] = cMatrix.preMultiply(x);
			}
			epsilon.set(t, z);
		}
		
		zRound = new FlexArray1D<Double>(tStart, tEnd);
		for (int t = tStart; t < tEnd; ++t)
			zRound.set(t, rand.nextUniform(0, 1));
		
//		// Compute the covariance matrix to check that it works.
//		// The realizations of epsilon_1 + ... + epsilon_K
//		double[][] epsilonReal = new double[T][M];
//		for (int t = 0; t < T; ++t) {
//			for (int k = 0; k < K; ++k) {
//				for (int m = 0; m < M; ++m) {
//					epsilonReal[t][m] += epsilon.get(t)[k][m]; 
//				}
//			}
//		}
//		Covariance covReal = new Covariance(epsilonReal);
//		System.out.println(covReal.getCovarianceMatrix());
	}

	@Override
	public int getNumberOfPeriodsInYear() {
		return Y;
	}
}
