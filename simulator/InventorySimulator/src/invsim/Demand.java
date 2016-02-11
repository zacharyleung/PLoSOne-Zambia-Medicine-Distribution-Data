package invsim;

/**
 * An abstract class that defines methods for demand model classes.
 * @author zacleung
 *
 */
public abstract class Demand {
	/**
	 * Generate actual demand and demand forecasts for the horizon
	 * [tStart, tEnd).
	 * 
	 * This method must be called before the demand class is used.
	 * 
	 * @param randomSeed
	 * @param tStart
	 * @param tEnd
	 */
	abstract public void generate(int randomSeed, int tStart, int tEnd);
	
	/**
	 * Return a Forecast object which gives the forecasted demand
	 * mean and standard deviation over periods [t, t + nPeriods).
	 */
	abstract public Forecast getForecast(int t, int nPeriods);
	
	abstract public int[] getDemand(int t, int nPeriods);
	
	abstract public int getNumberOfPeriodsInYear();

}
