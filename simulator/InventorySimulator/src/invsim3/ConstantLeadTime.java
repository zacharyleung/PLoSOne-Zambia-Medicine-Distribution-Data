package invsim3;

public class ConstantLeadTime extends LeadTime {
	private final int L1;
	private final int L2;
	
	ConstantLeadTime(int C, int[] OP, int[] OS, int D, int Y, int[] R2D,
			int L1, int L2) {
		super(C, OP, OS, D, Y, R2D);
		
		this.L1 = L1;
		this.L2 = L2;
	}
	
	@Override
	public void generate(int tStart, int tEnd, int randomSeed) {
		// Nothing needs to be done
	}

	@Override
	public double[] getTotalLeadTimePmf(int r, int t) {
		if (isTotalShipmentPeriod(r, t)) {
			int L = 2 * D + L1 + L2;
			double[] pmf = new double[L + 1]; 
			pmf[L] = 1;
			return pmf;
		} else {
			return new double[0];
		}
	}

	@Override
	public int getPrimaryLeadTime(int d, int t) {
		if (isPrimaryShipmentPeriod(d, t)) {
			return D + L1;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public int getSecondaryLeadTime(int r, int t) {
		if (isSecondaryShipmentPeriod(r, t)) {
			return D + L2;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public int getTotalLeadTime(int r, int t) {
		if (isTotalShipmentPeriod(r, t)) {
			return 2 * D + L1 + L2;
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public double getMeanAccessibility(int r) {
		return 1.0;
	}

	@Override
	public double getAccessibility(int r, int t) {
		return 1.0;
	}
}
