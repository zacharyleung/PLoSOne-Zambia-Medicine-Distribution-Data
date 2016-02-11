package d65helper;

import org.apache.commons.math.distribution.NormalDistributionImpl;

public class ZachRvNormal extends ZachRv {
	/** A standard normal random variable. */
	private NormalDistributionImpl rand;
	private final double mean;
	private final double sd;
	
	public ZachRvNormal(double mean, double sd) {
		this.mean = mean;
		this.sd = sd;
		rand = new NormalDistributionImpl(0, 1);
	}
	
	@Override
	public double getMean() throws Exception {
		return mean;
	}

	@Override
	public double getUnmetDemand(double I) throws Exception {
		double mu = I - getMean();
		double sigma = getStandardDeviation();
		return sigma * rand.density(-mu/sigma) +
				(-mu) * rand.cumulativeProbability(-mu/sigma);
	}
	
	public double getStandardDeviation() {
		return sd;
	}

	public static void test() {
		try {
			double mu = 1000;
			double sigma = 20;
			int inc = 10;
			int T = 100000;
			NormalDistributionImpl rand = new NormalDistributionImpl(0, 1);
			
			double[] D = new double[T];
			for (int t = 0; t < D.length; ++t)
				D[t] = mu + sigma * rand.sample();
			
			ZachRvNormal rv = new ZachRvNormal(mu,sigma);
			for (int i = 0; i < 130; i += inc) {
				double approx = 0;
				for (int t = 0; t < D.length; ++t) {
					approx += Math.max(0, D[t] - i);
				}
				System.out.format("I = %2d, E[(I-D)^-] = %6.4f, approx = %.6f\n",
						i,rv.getUnmetDemand(i),approx / T);
			}
			
			// Try computing tangents
			int iPast = 0;
			double uPast = mu;
			double u = uPast;
			int start = 0;
			start = (int) mu;
			// If we start at 0, we may run into numerical difficulties.
			for (int i = start; ; i += inc) {
				u = rv.getUnmetDemand(i);
				
				// Compute the coefficients
				double b = (u - uPast) / (i - iPast);
				double a = u - b * i;
				
				System.out.format("iPast = %d\tuPast = %.4e\n", iPast, uPast);
				System.out.format("i     = %d\tu     = %.4e\n", i, u);
				System.out.format("a + b * iPast = %.4e\n", a + b * iPast);
				System.out.format("uPast         = %.4e\n", uPast);
				System.out.format("a + b * i     = %.4e\n", a + b * i);
				System.out.format("u             = %.4e\n", u);
				System.out.format("i = %6d\tGradient = %.4e\tIntercept = %.8e\n",i,b,a);
				
				// Update previous points
				uPast = u;
				iPast = i;

				// Check to break 
				if (-b < 1.0 / 100.0)
					break;
			}
		} catch (Exception e) {
			;
		}
	}

	@Override
	public double inverseCumulativeProbability(double p) throws Exception {
		throw new Exception("Not yet implemented!");
		// TODO Auto-generated method stub
		//return 0;
	}
}
