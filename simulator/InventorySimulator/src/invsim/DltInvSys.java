package invsim;

import d65helper.ZachRv;

/**
 * A structure that represents a Deterministic lead time (DLT)
 * Inventory System.
 *  
 * @author Zack
 *
 */
public class DltInvSys extends AbstractInvSys {
	/** Number of retailers. */
	final int R;
	/** Number of periods in horizon. */
	final int T;
	/** Penalty cost for one unit of unmet demand. */
	final double p;
	/** Initial inventory levels. */
	final long[] I0;
	/** Array of shipments in the pipeline. */
	final Shipment[] X;
	/** Demand in each facility and each period. */
	final ZachRv[][] D;
	/** Deterministic lead time for shipment to facility in period. */
	final int[][] L;
	final double[][][] leadTimePmf;
	///** The smallest unit of inventory that can be shipped. */
	//final int invUnit;
	/** Initial inventory at the warehouse. */
	final long Iw0;
	/** 
	 * Xw[t] = inventory scheduled to arrive at the beginning of
	 * period t.
	 */
	final int[] Xw;
	
	DltInvSys(double p, long[] I0, Shipment[] X, ZachRv[][] D,
			int[][] L, double[][][] leadTimePmf, long Iw0, int[] Xw) {
		this.p  = p;
		this.I0 = I0;
		this.X = X;
		this.D = D;
		this.L = L;
		this.leadTimePmf = leadTimePmf;
		this.Iw0 = Iw0;
		this.Xw = Xw;
		
		R = I0.length;
		T = D[0].length;
	}
}
