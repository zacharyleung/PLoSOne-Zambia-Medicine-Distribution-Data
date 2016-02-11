package d65helper;

import org.apache.commons.math.distribution.PoissonDistributionImpl;

public class ZachRvPoisson extends ZachRv {
	private PoissonDistributionImpl rand;
	
	public ZachRvPoisson(double mean) {
		rand = new PoissonDistributionImpl(mean);
	}
	
	@Override
	public double getMean() throws Exception {
		return rand.getMean();
	}

	@Override
	public double getUnmetDemand(double I) throws Exception {
		long intI = Math.round(I);
		double u = getMean();
		for (int i = 1; i <= intI; ++i) {
			u -= i * rand.probability(i);
		}
		throw new Exception("Not yet implemented!");
		//return 0;
	}

	@Override
	public double getStandardDeviation() throws Exception {
		return Math.sqrt(getMean());
	}

	@Override
	public double inverseCumulativeProbability(double p) throws Exception {
		throw new Exception("Not yet implemented!");
		// TODO Auto-generated method stub
		//return 0;
	}

}
