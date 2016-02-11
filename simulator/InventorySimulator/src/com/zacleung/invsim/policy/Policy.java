package com.zacleung.invsim.policy;

import invsim3.Facility;
import invsim3.Forecast;
import invsim3.Retailer;

import java.io.PrintStream;

public abstract class Policy {
	public static enum AllocType {
		PRIORITY, PROPORTIONAL
	}
	
	public static final int BOX_SIZE = 30;
	
	protected int[] getPriorityAllocation(long inventory, int[] orders) {
		int R = orders.length;
		int[] X = new int[R];

		for (int r = 0; r < R; ++r) {
//			System.out.format("inventory = %d", inventory);
			X[r] = (int) Math.min(inventory, orders[r]);
			inventory -= X[r];
//			System.out.format("\torder = %d\tshipment = %d", orders[r], X[r]);
//			System.out.println();
		}
		
		return X;
	}

	protected int[] getProportionalAllocation(long inventory, int[] orders) {
		int R = orders.length;
		int[] X = new int[R];

		int sum = 0;
		for (int r = 0; r < R; ++r) {
			sum += orders[r];
		}

		for (int r = 0; r < R; ++r) {
			if (sum <= inventory) { // Satisfy orders fully
				X[r] = orders[r];
			} else { // Make proporitional allocation
				double scale = ((double) inventory) / sum;
				X[r] = (int) Math.floor((double) orders[r] * scale);
			}
		}

		return X;
	}
	
	protected int[] boxify(long warehouseInventory, int[] shipments) {
		int sum = 0;
		for (int i = 0; i < shipments.length; ++i) {
			int nBoxes = (shipments[i] + BOX_SIZE - 1) / BOX_SIZE;
			shipments[i] = nBoxes * BOX_SIZE;
			sum += shipments[i];
		}
		if (sum > warehouseInventory) {
			throw new RuntimeException("Warehouse inventory exceeded!");
		} else {
			return shipments;
		}
	}
	
}
