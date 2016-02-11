package invsim3;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import d65helper.FlexArray1D;

public class Retailer extends Facility {
	public final Demand demand;
	/** Multiplicative factor to scale */
	public final double scalePreSimulationDemand;
	
	/** The unmet demand. */
	private FlexArray1D<Long> demandUnmet;

	private List<Shipment> shipmentList = new LinkedList<Shipment>();

	public Retailer(Demand demand) {
		this(demand, 1.0);
	}
	
	public Retailer(Demand demand, double scalePreSimulationDemand) {
		this.demand = demand;
		this.scalePreSimulationDemand = scalePreSimulationDemand;
	}

	public void generate(int randomSeed) {
		super.generate(randomSeed);
		
		int tStart = simulator.getStartPeriod();
		int tEnd = simulator.getEndPeriod();
		int Y = simulator.getNumberOfPeriodsInYear();
		
		// Generate one extra year of demand at the beginning for
		// policies that need past demand
		// Generate one extra year of demand at the end for policies
		// that need demand forecasts
		demand.generate(tStart - Y, tEnd + Y, randomSeed);

		for (int t = tStart - Y; t < tStart; ++t) {
			long issue = (long) Math.ceil(scalePreSimulationDemand * getDemand(t));
			issueArray.set(t, issue);
		}
		
		demandUnmet = new FlexArray1D<>(tStart, tEnd);
	}
	
	/**
	 * Demand arrives. Consume inventory,  
	 * @throws Exception 
	 */
	public void demandAppears() throws Exception {
		int t = getCurrentTimePeriod();

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
		return new Stats(this);
	}
	
	public class Stats {
		/** The amount demanded. */
		public final long demandTotal;
		/** The amount demanded which was not satisfied. */
		public final long demandUnmet;
		public final double serviceLevel;
		public final long inventoryTotal;
		public final double maxInventoryInDemandPerPeriod;
		
		public Stats(Retailer retailer) {
			int tEnd = retailer.simulator.getEndPeriod();

			long demandTotalTemp = 0;
			long demandUnmetTemp = 0;
			long inventoryTotalTemp = 0;
			long maxInventory = 0;
			for (int t = 0; t < tEnd; ++t) {
				demandTotalTemp += getDemand(t);
				demandUnmetTemp += getUnmetDemand(t);
				inventoryTotalTemp += invEnd.get(t);
				maxInventory = Math.max(maxInventory, invEnd.get(t));
			}
			demandTotal = demandTotalTemp;
			demandUnmet = demandUnmetTemp;
			inventoryTotal = inventoryTotalTemp;
			double meanDemandPerPeriod = demandTotalTemp / tEnd;
			maxInventoryInDemandPerPeriod = maxInventory / meanDemandPerPeriod;
						
			serviceLevel = 1 - (double) demandUnmet / demandTotal;
		}
	}
}
