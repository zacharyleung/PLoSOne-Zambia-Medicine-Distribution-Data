package invsim3;

import java.util.Arrays;

import org.apache.commons.math.random.RandomDataImpl;

import d65helper.FlexArray1D;
import d65helper.FlexArray3D;
import d65helper.Helper;

/**
 * Multiplicative MMFE demand model.
 * 
 * @author zacleung
 *
 */
public class MultMmfeDemand extends Demand {
	private double[] mean;
	/** var[m] = Variance of predicting m periods in advance. */
	private double[] var;
	/** Length of forecast horizon */
	private int M;

	/**
	 * forecastAccuracy[k] = forecast accuracy for level k,
	 * which is a number in [0,1], which gives the proportion of
	 * the past epsilons which are known by the forecaster.
	 * The forecast accuracy levels are strictly increasing with k. 
	 * By convention, forecastAccuracy[0] = 0 and
	 * forecastAccuracy[K] = 1.
	 */
	double[] forecastAccuracy;

	/**
	 * forecastLevel ranges from 0 to K.
	 * The forecaster is only able to observe the epsilon_tmk values
	 * for k < forecastLevel.  
	 * See comments about forecastAccuracy.
	 */
	private int forecastLevel;

	/**
	 * The weight of forecast variable k
	 *   = forecastAccuracy[k + 1] - forecastAccuracy[k];
	 * We need to scale C * Z by sqrt(forecastWeight[k]) to get the
	 * k-th random variable.
	 */
	private double[] forecastWeight;

	/**
	 * Number of random variables to generate to get demand forecasts
	 * of varying accuracy.
	 */
	private int K;

	private FlexArray3D<Double> z;

	/** To round the demand from a float to an integer. */
	private FlexArray1D<Double> zRound;
	
	/**
	 * Multiplicative MMFE demand model to generate actual demand
	 * and demand forecasts.
	 * @param var var[m] is the variance of the normal random
	 * variable which is revealed m periods in advance.
	 * @param forecastAccuracy
	 */
	public MultMmfeDemand(double[] mean, double[] var,
			double[] forecastAccuracy, int forecastLevel) {
		this.mean = Arrays.copyOf(mean, mean.length);
		this.var = Arrays.copyOf(var, var.length);
		M = var.length;

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

	@Override
	public void generate(int tStart, int tEnd, int randomSeed) {
		RandomDataImpl rand = new RandomDataImpl();
		rand.reSeed(randomSeed);

		// We have to generate random numbers before the tStart
		// of the time horizon
		z = new FlexArray3D<>(tStart - M, tEnd, 0, M, 0, K);
		for (int t = tStart - M; t < tEnd; ++t) {
			for (int m = 0; m < M; ++m) {
				for (int k = 0; k < K; ++k) {
					double thevar = forecastWeight[k] * var[m];
					double mu = -thevar / 2;
					double sigma = Math.sqrt(thevar);
					double d = rand.nextGaussian(mu, sigma);
					z.set(t, m, k, d);
				}
			}
		}
		
		zRound = new FlexArray1D<>(tStart, tEnd);
		for (int t = tStart; t < tEnd; ++t) {
			zRound.set(t, rand.nextUniform(0, 1));
		}
	}

	@Override
	public Forecast getForecast(int t, int nPeriods) {
		double[] myMean = new double[nPeriods];
		double[] myVar = new double[nPeriods];
		
		for (int i = 0; i < nPeriods; ++i) {
			MyForecast myF = myGetForecast(t, t + i, forecastLevel);
			myMean[i] = myF.mean;
			myVar[i] = myF.var;
		}
		
		return new Forecast(t, myMean, myVar);
	}

	@Override
	public int[] getDemand(int t, int nPeriods) {
		int[] d = new int[nPeriods];
		for (int i = 0; i < nPeriods; ++i) {
			MyForecast myF = myGetForecast(t + i + 1, t + i, K);
			d[i] = (int) (myF.mean + zRound.get(t + i));
		}
		
		return d;
	}

	/**
	 * Demand forecast at the beginning of period t for the demand
	 * during period u.
	 * @param t The current time period
	 * @param u The forecasted time period
	 * @return The demand forecast mean and standard deviation
	 */
	private MyForecast myGetForecast(int t, int u, int Ksum) {
		// Sum of the normal random variables
		double logmean = 0;
		// Sum of the variance of the normal random variables
		double logvar = 0;
		for (int v = u - M + 1; v < t && v <= u; ++v) {
			int m = u - v;
			for (int k = 0; k < Ksum; ++k) {
				logmean += z.get(v, m, k);
			}
			for (int k = forecastLevel; k < K; ++k) {
				logvar += forecastWeight[k] * var[m];
			}
		}
		for (int v = Math.max(t, u - M + 1); v <= u; ++v) {
			int m = u - v;
			logvar += var[m];
		}
		double myMean = getMean(u) * Math.exp(logmean);
		double myVar = Math.pow(getMean(u) * Math.sqrt(Math.exp(logvar) - 1), 2);
		return new MyForecast(myMean, myVar);
	}

	/**
	 * 
	 * @param t
	 * @return
	 */
	private double getMean(int t) {
		int Y = mean.length; 
		return mean[Helper.modulo(t, Y)];
	}
	
	class MyForecast {
		double mean;
		double var;
		MyForecast(double mean, double var) {
			this.mean = mean;
			this.var = var;
		}
	}
	
	public static void main(String[] args) {

		double[] var = {0.01, 0.25, 0.16, 0.09};
		double[] mean = {100};
		double[] forecastAccuracy = {0, 0.4, 1};
		int forecastLevel = 2;
		int randomSeed = 0;
		int tStart = -3;
		int tEnd = 3;
		
		MultMmfeDemand mmfe = new MultMmfeDemand(mean, var,
				forecastAccuracy, forecastLevel);
		
		mmfe.generate(randomSeed, tStart, tEnd);
		
		for (int t = -3; t <= 1; ++t) {
			System.out.println(mmfe.getForecast(t, 5));
		}
		
		for (int t = -3; t <= 1; ++t) {
			System.out.println("Period = " + t + "\t"
					+ "Demand = " + mmfe.getDemand(t, 1)[0]);
		}
	}

	@Override
	public int getNumberOfPeriodsInYear() {
		return mean.length;
	}
}
