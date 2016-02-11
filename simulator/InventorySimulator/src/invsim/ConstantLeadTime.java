package invsim;

public class ConstantLeadTime extends LeadTime {
	/** Number of periods for primary lead time. */
	private final int L1;
	/** Number of periods for secondary lead time. */
	private final int L2;
	/** Number of periods for total lead time, i.e. L1 + L2. */
	private final int L;

	/** Number of periods in a year. */
	private final int Y;
	
	/**
	 * @param L Lead time in number of periods
	 */
	public ConstantLeadTime(int C, int OP, int OS, int D,
			int L1, int L2, int Y) {
		super(C, OP, OS, D);
		
		this.L1 = L1;
		this.L2 = L2;
		this.Y = Y;
		
		L = L1 + L2;
	}
	
	@Override
	public void generate(int randomSeed, int tStart, int tEnd) {
		// Nothing needs to be done
	}

	@Override
	public int getNumberOfPeriodsInYear() {
		return Y;
	}

	@Override
	public int getPrimaryLeadTime(int t) {
		if (isPrimaryShipmentPeriod(t)) {
			return L1;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public int getSecondaryLeadTime(int t) {
		if (isSecondaryShipmentPeriod(t)) {
			return L2;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public int getTotalLeadTime(int t) {
		if (isTotalShipmentPeriod(t)) {
			return L1;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public double[][] getTotalLeadTimePmf(int t, int nPeriods) {
		int L = L1 + L2;
		double[][] pmf = new double[nPeriods][L + 1]; 
		for (int i = 0; i < nPeriods; ++i) {
			pmf[i][L] = 1;
		}
		return pmf;
	}
}
