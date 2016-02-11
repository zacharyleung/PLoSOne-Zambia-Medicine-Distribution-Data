package invsim;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import d65helper.FlexArray1D;

public class RetailerAAA extends Facility {
	final Demand demand;
	
	/** The unmet demand. */
	private FlexArray1D<Long> demandUnmet;

	private List<Shipment> shipmentList = new LinkedList<Shipment>();

	public RetailerAAA(Demand demand, LeadTime leadTime) {
		this.demand = demand;
		this.leadTime = leadTime;
		
		// Check that the demand and the lead time model have the
		// same number of periods in a year.
		if (demand.getNumberOfPeriodsInYear() !=
				leadTime.getNumberOfPeriodsInYear()) {
			throw new IllegalArgumentException(
					"The number of years don't match!");
		}
	}

	public void generate(int tStart, int tEnd, int randomSeed) {
		super.generate(tStart, tEnd, randomSeed);
		
		int Y = getNumberOfPeriodsInYear();
		// Generate one extra year of demand at the beginning for
		// policies that need past demand
		// Generate one extra year of demand at the end for policies
		// that need demand forecasts
		demand.generate(randomSeed, tStart - Y, tEnd + Y);
		// Generate one extra year of demand at the end for policies
		// that need lead time forecasts
		leadTime.generate(randomSeed, tStart, tEnd + Y);

		demandUnmet = new FlexArray1D<>(tStart, tEnd);
	}
	
	/**
	 * Demand arrives. Consume inventory,  
	 * @throws Exception 
	 */
	public void demandAppears() throws Exception {
		long d = getDemand(t);
		long i = getInventoryLevel();
		getDrugs(Math.min(d, i));
		demandUnmet.set(t, Math.max(0, d - i));
		invEnd.set(t, Math.max(0, i - d));
//		System.out.format("Retailer.demandAppears() period %d\n", t);
//		System.out.format("inventory = %d, demand = %d, unmet = %d, end inventory = %d\n",
//				i, d, Math.max(0, d - i), Math.max(0, i - d));
	}

	public long getUnmetDemand(int t) {
		return demandUnmet.get(t);
	}
	
	public long getDemand(int t) {
		return demand.getDemand(t, 1)[0];
	}

	/**
	 * Print the current state of the retailer.
	 * @param out
	 */
	public void print(PrintStream out) {
		out.println("Retailer.print()");
		out.println("Inventory level = " + getInventoryLevel());
		out.println("Shipments = ");
		for (Shipment s : shipmentList) {
			out.println(s);
		}
	}
	
	public int getNumberOfPeriodsInYear() {
		return demand.getNumberOfPeriodsInYear();
	}

	public Stats getStats() {
		long demandTotal = 0;
		long demandUnmet = 0;
		long inventoryTotal = 0;
		for (int t = 0; t < tEnd; ++t) {
			demandTotal += getDemand(t);
			demandUnmet += getUnmetDemand(t);
			inventoryTotal += invEnd.get(t);
		}
		
		return new Stats(demandTotal, demandUnmet, inventoryTotal);
	}
	
	public class Stats {
		/** The amount demanded. */
		public final long demandTotal;
		/** The amount demanded which was not satisfied. */
		public final long demandUnmet;
		public final double serviceLevel;
		public final long inventoryTotal;
		
		public Stats(long demandTotal, long demandUnmet, long inventoryTotal) {
			this.demandTotal = demandTotal;
			this.demandUnmet = demandUnmet;
			this.inventoryTotal = inventoryTotal;
			
			serviceLevel = 1 - (double) demandUnmet / demandTotal;
		}
	}
}
