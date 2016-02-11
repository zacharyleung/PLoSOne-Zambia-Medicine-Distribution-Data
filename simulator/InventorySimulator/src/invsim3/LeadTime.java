package invsim3;

import com.zacleung.util.MyMath;

import d65helper.Helper;

public abstract class LeadTime {
	/** Number of periods in a cycle. */
	private final int C;
	/** Cycle offset for primary lead times. */
	private final int[] OP;
	/** Cycle offset for secondary lead times (under district system). */
	private final int[] OS;
	/** Number of periods of delay in order transmission. */
	protected final int D;
	/** Number of retailers. */
	protected final int R;
	/** Number of periods in a year. */
	protected final int Y;
	/**
	 * R2D stands for "Retailer to District."
	 * R2D[r] = the district index d of the district which contains
	 * retailer r.
	 */
	protected final int[] R2D;
	
	private final int nDistricts;
	
	public static final int NO_LEAD_TIME = Integer.MIN_VALUE;
	
	protected LeadTime(int C, int[] OP, int[] OS, int D, int Y, int[] R2D) {
		this.C = C;
		this.OP = OP;
		this.OS = OS;
		this.D = D;
		this.Y = Y;
		this.R2D = R2D;
		this.nDistricts = OP.length;
		
		// Check that OP has the right number of elements
		int maxR2D = 0;
		for (int r = 0; r < OS.length; ++r) {
			maxR2D = Math.max(maxR2D, R2D[r]);
		}
		if (OP.length != maxR2D + 1) {
			throw new IllegalArgumentException(
					"OP and R2D don't match!");
		}
		
		R = OS.length;
		
		// Check that OS and R2D have the same length
		if (OS.length != R2D.length) {
			throw new IllegalArgumentException(
					"OS and R2D don't match!");
		}
	}
	
	
	abstract public void generate(int tStart, int tEnd, int randomSeed);
	
	/**
	 * Get the lead time pmf for shipments that are sent during
	 * week t to retailer r.
	 * @param t
	 * @param nPeriods
	 * @return
	 */
	abstract public double[] getTotalLeadTimePmf(int r, int t);

	abstract public int getPrimaryLeadTime(int d, int t);

	abstract public int getSecondaryLeadTime(int r, int t);
	
	/**
	 * Get the lead time for the shipment that was sent during
	 * week t.
	 * @param t
	 * @return
	 */
	abstract public int getTotalLeadTime(int r, int t);

	public boolean isPrimaryShipmentPeriod(int d, int t) {
		return Helper.modulo(t + D, C) == OP[d];
	}

	public boolean isSecondaryShipmentPeriod(int r, int t) {
		return Helper.modulo(t + D, C) == OS[r];
	}
	
	public boolean isTotalShipmentPeriod(int r, int t) {
		int d = R2D[r];
		boolean result = MyMath.positiveModulo(t + 2 * D, C) == OP[d]; 

//		System.out.printf("Period %d is total shipment period = %b\n",
//				t, result);
		
		return result;
	}
	
	public int getNumberOfPeriodsInYear() {
		return Y;
	}

	public int getNumberOfDistricts() {
		return nDistricts;
	}
	
	public int getDistrictIndex(int r) {
		return R2D[r];
	}
	
	abstract public double getMeanAccessibility(int r);
	
	abstract public double getAccessibility(int r, int t);
}
