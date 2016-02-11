package com.zacleung.invsim.policy;

import invsim3.District;
import invsim3.LeadTime;
import invsim3.NationalWarehouse;
import invsim3.Retailer;
import invsim3.Simulator;

import java.io.PrintStream;

/**
 * This class implements an inventory replenishment policy that
 * uses order-up-to policy
 * @author zacleung
 *
 */
public class OrderUpToDistrictPolicy extends DistrictPolicy {
	private final int tHistory;
	private final double tOrderDistrict;
	private final double tOrderRetailer;

	public OrderUpToDistrictPolicy(int tHistory, double tOrderDistrict,
			double tOrderRetailer) {
		this.tHistory = tHistory;
		this.tOrderDistrict = tOrderDistrict;
		this.tOrderRetailer = tOrderRetailer;
	}

	@Override
	public int[] computeRetailerShipments(Simulator simulator) throws Exception {
		int R = simulator.R;
		int D = simulator.D;
		Retailer[] retailers = simulator.retailers;
		District[] districts = simulator.districts;
		LeadTime leadTime = simulator.leadTime;
		int t = simulator.getCurrentTimePeriod();

		long[] base = new long[R];
		for (int r = 0; r < R; ++r) {
			base[r] = retailers[r].getInventoryPosition();
		}

		int[] out = new int[R];
		for (int r = 0; r < R; ++r) {
			// Make an order only if it is a shipment period
			double sum = 0;
			for (int k = 0; k < tHistory; ++k) {
				sum += retailers[r].getDemand(t - 1 - k);
			}
			out[r] = (int) Math.round(Math.ceil(sum * tOrderRetailer / tHistory));
		}

		int[] X = new int[R];

		long[] I = new long[D];
		for (int d = 0; d < D; ++d) {
			I[d] = districts[d].getInventoryLevel();
		}
		for (int r = 0; r < R; ++r) {
			if (leadTime.isSecondaryShipmentPeriod(r, t)) {
				//System.out.println(I + " " + quantity);
				int d = leadTime.getDistrictIndex(r);
				long quantity = Math.max(0, out[r] - base[r]);
				districts[d].addOrder(t, quantity);
				X[r] = (int) Math.min(I[d], quantity);
				I[d] -= X[r];
			}
		}

		return X;
	}

	@Override
	public int[] computeDistrictShipments(Simulator simulator) throws Exception {
		int D = simulator.D;
		NationalWarehouse national = simulator.national;
		District[] districts = simulator.districts;
		LeadTime leadTime = simulator.leadTime;
		int t = simulator.getCurrentTimePeriod();

		int[] X = new int[D];

		long[] base = new long[D];
		for (int d = 0; d < D; ++d) {
			base[d] = districts[d].getInventoryPosition();
//			System.out.format("base[%d] = %d", d, base[d]);
//			System.out.println();
		}

		int[] out = new int[D];
		for (int d = 0; d < D; ++d) {
			double sum = 0;
			for (int k = 0; k < tHistory; ++k) {
				sum += districts[d].getOrders(t - 1 - k);
//				System.out.format("%d\t", districts[d].getOrders(t - 1 - k));
			}
//			System.out.println();
			out[d] = (int) Math.round(Math.ceil(sum * tOrderDistrict / tHistory));
//			System.out.format("out[%d] = %d", d, out[d]);
//			System.out.println();
		}

		
		long I = national.getInventoryLevel();
		for (int d = 0; d < D; ++d) {
			if (leadTime.isPrimaryShipmentPeriod(d, t)) {
				long order = Math.max(0, out[d] - base[d]);
				X[d] = (int) Math.min(I, order);
				I -= X[d];
//				System.out.format("ship[%d] = %d", d, X[d]);
//				System.out.println();
			}
		}

		//		System.out.println("ConstantDistrictPolicy.computeDistrictShipments()");
		//		for (int d = 0; d < D; ++d)
		//			System.out.print(X[d] + "\t");
		//		System.out.println();

		return X;	
	}

	@Override
	public void print(PrintStream out) {
		out.println("# Order-Up-To District Inventory Policy");
		out.println("# tHistory = " + tHistory);
		out.println("# tOrderDistrict = " + tOrderDistrict);
		out.println("# tOrderRetailer = " + tOrderRetailer);
	}

}
