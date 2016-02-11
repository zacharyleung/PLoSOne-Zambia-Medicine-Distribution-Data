package d65helper;

public class ZachRvConstantInteger extends ZachRv {
	private final int value;
	
	public ZachRvConstantInteger(int value) {
		this.value = value;
	}
	
	@Override
	public double getMean() throws Exception {
		return value;
	}

	@Override
	public double getStandardDeviation() throws Exception {
		return 0;
	}

	@Override
	public double getUnmetDemand(double I) throws Exception {
		return Math.max(0, value - I);
	}

	@Override
	public double inverseCumulativeProbability(double p) throws Exception {
		return value;
	}

}
