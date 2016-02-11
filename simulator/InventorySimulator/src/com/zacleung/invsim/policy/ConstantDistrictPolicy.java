package com.zacleung.invsim.policy;

import invsim3.District;
import invsim3.LeadTime;
import invsim3.NationalWarehouse;
import invsim3.Retailer;
import invsim3.Simulator;

import java.io.PrintStream;


public class ConstantDistrictPolicy extends DistrictPolicy {
	private final int qRetailer;
	private final int qDistrict;
	
	public ConstantDistrictPolicy(int qDistrict, int qRetailer) {
		this.qDistrict = qDistrict;
		this.qRetailer = qRetailer;
	}


	@Override
	public int[] computeRetailerShipments(Simulator simulator) throws Exception {
		int R = simulator.R;
		int D = simulator.D;
		Retailer[] retailers = simulator.retailers;
		District[] districts = simulator.districts;
		LeadTime leadTime = simulator.leadTime;
		int t = simulator.getCurrentTimePeriod();

		int[] X = new int[R];

		long[] I = new long[D];
		for (int d = 0; d < D; ++d) {
			I[d] = districts[d].getInventoryLevel();
		}
		for (int r = 0; r < R; ++r) {
			if (leadTime.isSecondaryShipmentPeriod(r, t)) {
				//System.out.println(I + " " + quantity);
				int d = leadTime.getDistrictIndex(r);
				X[r] = (int) Math.min(I[d], qRetailer);
				I[d] -= X[r];
			}
		}
		
		return X;
	}

	@Override
	public int[] computeDistrictShipments(Simulator simulator) throws Exception {
		int D = simulator.D;
		NationalWarehouse national = simulator.national;
		LeadTime leadTime = simulator.leadTime;
		int t = simulator.getCurrentTimePeriod();

		int[] X = new int[D];

		long I = national.getInventoryLevel();
		for (int d = 0; d < D; ++d) {
			if (leadTime.isPrimaryShipmentPeriod(d, t)) {
				X[d] = (int) Math.min(I, qDistrict);
				I -= X[d];
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
		// TODO Auto-generated method stub

	}

}
