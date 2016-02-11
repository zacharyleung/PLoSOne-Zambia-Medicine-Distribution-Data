package d65helper;

public class ZachRvEmpirical extends ZachRv {
	private double[] pmf;

	public ZachRvEmpirical(double[] pmf) throws Exception {
		this.pmf = pmf;

		double sum = 0;
		for (int i = 0; i < pmf.length; ++i)
			sum += pmf[i];
		if (Math.abs(sum - 1.0) > 0.001) {
			for (int i = 0; i < pmf.length; ++i) {
				System.out.print(pmf[i] + "\t");
			}
			System.out.println();
			System.out.println("Sum of pmf = " + sum);
			throw new Exception("PMF invalid.");
		}

		//		for (int i = 0; i < pmf.length; ++i) {
		//			System.out.print(pmf[i] + "\t");
		//		}
		//		System.out.println();
	}

	@Override
	public double getMean() throws Exception {
		double mean = 0;
		for (int i = 0; i < pmf.length; ++i)
			mean += i * pmf[i];
		return mean;
	}

	@Override
	public double getStandardDeviation() throws Exception {
		throw new Exception("Not yet implemented!");
		// TODO Auto-generated method stub
		//return 0;
	}

	@Override
	public double getUnmetDemand(double I) throws Exception {
		double result = 0;
		for (int i = (int) Math.ceil(I); i < pmf.length; ++i) {
			result += pmf[i] * (i - I);
		}
		return result;
	}

	@Override
	public double inverseCumulativeProbability(double p) throws Exception {
		double sum = 0;
		int k;
		for (k = 0; k < pmf.length; ++k) {
			sum += pmf[k];
			if (sum >= p)
				return k;
		}
		throw new Exception("Something wrong with pmf or p!");
	}

	public int getMin() throws Exception {
		for (int k = 0; k < pmf.length; ++k)  {
			if (pmf[k] > 0)
				return k;
		}
		throw new Exception("Something wrong with pmf!");
	}

	/**
	 * Return the new distribution of this random variable,
	 * conditioned on the value being k or above.
	 * @param k
	 * @return
	 */
	public ZachRvEmpirical getConditionalRv(int k) throws Exception {
		int T = pmf.length;
		double[] newpmf = new double[T];

		// Compute the probability that the random variable is
		// k or greater
		double sum = 0;
		for (int i = k; i < T; ++i)
			sum += pmf[i];

		// Scale the old pmf by 1/probability
		for (int i = k; i < T; ++i)
			newpmf[i] = pmf[i] / sum;

		return new ZachRvEmpirical(newpmf);
	}

	@Override
	public String toString() {
		String out = "";
		try {
			String nl = System.getProperty("line.separator");
			out = "ZachRvEmpirical" + nl;
			out += "pmf = ";
			for (int i = 0; i < pmf.length; ++i)
				out += String.format("%.4f", pmf[i]) + "\t";
			out += nl;

			out += "Mean = " + String.format("%.2f", getMean());
		} catch (Exception e) {}

		return out;
	}

	public static void main(String[] args) throws Exception {
		double[] pmf = {0.2, 0.2, 0.2, 0.2, 0.2};
		ZachRvEmpirical emp = new ZachRvEmpirical(pmf);
		
		System.out.println(emp);
		
		System.out.println(emp.getConditionalRv(2));
	}
}
