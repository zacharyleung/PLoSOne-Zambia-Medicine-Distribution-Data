package d65helper;

/**
 * A random variable wrapper.
 * @author Zack
 *
 */
public abstract class ZachRv {
	public abstract double getMean() throws Exception;
	
	public abstract double getStandardDeviation() throws Exception;
	
	/**
	 * For this distribution, X, this method returns E[(I-X)^-].
	 * @param I
	 * @return
	 * @throws Exception
	 */
	public abstract double getUnmetDemand(double I) throws Exception;
	
	/**
	 * For this distribution, X, this method returns the critical point x,
	 * such that P(X < x) = p.
	 * @param p
	 * @return
	 */
	public abstract double inverseCumulativeProbability(double p) throws Exception; 
    
}
