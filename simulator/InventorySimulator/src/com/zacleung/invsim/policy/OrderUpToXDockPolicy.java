package com.zacleung.invsim.policy;

import invsim3.Forecast;
import invsim3.LeadTime;
import invsim3.NationalWarehouse;
import invsim3.Retailer;
import invsim3.Simulator;

import java.io.PrintStream;

import com.zacleung.invsim.policy.Policy.AllocType;

public class OrderUpToXDockPolicy extends XDockPolicy {
	public final double numberOfPeriods;
	public final AllocType allocType;
	public final DemandEstimation demandEstimation;
	
	/**
	 * Order up to an inventory level of tOrder periods of
	 * demand.
	 * The mean period demand is estimated either using the last
	 * tHistory periods of observed demand, or using the demand
	 * forecast.  
	 * @param tHistory
	 * @param tOrder
	 */
	public OrderUpToXDockPolicy(DemandEstimation demandEstimation,
			double numberOfPeriods, AllocType allocType) {
		this.demandEstimation = demandEstimation;
		this.numberOfPeriods = numberOfPeriods;
		this.allocType = allocType;
	}


	@Override
	public int[] computeShipments(Simulator simulator) {
		int R = simulator.R;
		int t = simulator.getCurrentTimePeriod();
		NationalWarehouse warehouse = simulator.national;
		Retailer[] retailers = simulator.retailers;
		LeadTime leadTime = simulator.leadTime; 

		//System.out.println("OrderUpToPolicy.computeShipments() week " + t);

		int[] orders = new int[R];
		for (int r = 0; r < R; ++r) {
			// Make an order only if it is a shipment period
			if (leadTime.isTotalShipmentPeriod(r, t)) {

				long base = retailers[r].getInventoryPosition();

				double averageDemandPerPeriod = demandEstimation.getPerPeriodDemand(retailers[r]);
				
				// Compute order up to level.
				double out = numberOfPeriods * averageDemandPerPeriod;
				
//				retailers[r].print(System.out);
//				System.out.printf("order-up-to[%d] = %d%n", r, Math.round(out));
//				System.out.printf("base[%d] = %d%n", r, base);
//				System.out.println(" " + retailers[r].getInventoryLevel());

				orders[r] = (int) Math.max(0, out - base);
			}
		}

		int[] Q;
		// If the total sent inventory exceeds the inventory
		// available, then rationing is needed
		long wareInv = warehouse.getInventoryLevel(); 
		if (allocType == AllocType.PRIORITY) {
			Q = getPriorityAllocation(wareInv, orders);
		} else {
			Q = getProportionalAllocation(wareInv, orders);
		}

		return boxify(wareInv, Q);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder()
		.append(String.format("# Order-Up-To Cross-docking Inventory Policy%n"))
		.append(String.format("# Demand Estimation = %s%n",
				demandEstimation.toString()))
		.append(String.format("# Order-up-to number of periods = %.1f%n",
				numberOfPeriods))
		.append(String.format("# How to allocation limited inventory = %s%n",
				allocType.toString()));
		return sb.toString();
	}
}
