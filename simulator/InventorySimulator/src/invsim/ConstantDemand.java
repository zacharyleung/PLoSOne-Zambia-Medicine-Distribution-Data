package invsim;

public class ConstantDemand extends Demand {
	private final int demand;
	private final int Y;
	
	public ConstantDemand(int demand, int Y) {
		this.demand = demand;
		this.Y = Y;
	}
	
	@Override
	public Forecast getForecast(int t, int nPeriods) {
		double[] mean = new double[nPeriods];
		double[] var = new double[nPeriods];
		
		for (int u = 0; u < nPeriods; ++u) {
			mean[u] = demand;
			var[u] = 0;
		}
		
		return new Forecast(mean, var);
	}

	@Override
	public int[] getDemand(int t, int nPeriods) {
		int[] result = new int[nPeriods];
		for (int u = 0; u < nPeriods; ++u) {
			result[u] = demand;
		}
		return result;
	}

	@Override
	public void generate(int randomSeed, int tStart, int tEnd) {
		// Nothing needs to be done
	}

	@Override
	public int getNumberOfPeriodsInYear() {
		return Y;
	}

}
