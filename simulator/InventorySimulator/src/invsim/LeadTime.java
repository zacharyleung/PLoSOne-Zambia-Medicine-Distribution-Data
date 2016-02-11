package invsim;

import d65helper.Helper;

/**
 * An abstract class for lead time model classes.
 * 
 * There are three types of lead times:
 *   o Primary lead time: from National to District
 *   o Secondary lead time: from District to Retailer
 *   o Total lead time: from National to Retailer 
 * 
 * In a cross-docking system, we only need to use total lead times.
 * In a district system, we districts order according to some cycle
 * offset, but the retailers can order according to a possibly
 * different cycle offset.
 * 
 * @author zacleung
 *
 */
public abstract class LeadTime {
	/** Number of periods in a cycle. */
	private final int C;
	/** Cycle offset for primary lead times. */
	private final int OP;
	/** Cycle offset for secondary lead times. */
	private final int OS;
	/** Number of weeks of delay. */
	private final int D;
	
	LeadTime(int C, int OP, int OS, int D) {
		this.C = C;
		this.OP = OP;
		this.OS = OS;
		this.D = D;
	}
	
	public static final int NO_LEAD_TIME = Integer.MIN_VALUE;
	
	abstract public void generate(int randomSeed, int tStart, int tEnd);
	
	/**
	 * Get the lead time pmf for shipments that are sent during
	 * weeks [t, t+nPeriods).
	 * @param t
	 * @param nPeriods
	 * @return
	 */
	abstract public double[][] getTotalLeadTimePmf(int t, int nPeriods);

	abstract public int getPrimaryLeadTime(int t);

	abstract public int getSecondaryLeadTime(int t);

	
	/**
	 * Get the lead time for the shipment that was sent during
	 * week t.
	 * @param t
	 * @return
	 */
	abstract public int getTotalLeadTime(int t);

	public boolean isPrimaryShipmentPeriod(int t) {
		return Helper.modulo(t + D, C) == OP;
	}

	public boolean isSecondaryShipmentPeriod(int t) {
		return Helper.modulo(t + D, C) == OS;
	}
	
	public boolean isTotalShipmentPeriod(int t) {
		return Helper.modulo(t + 2 * D, C) == OP;
	}
	
	abstract public int getNumberOfPeriodsInYear();
}
