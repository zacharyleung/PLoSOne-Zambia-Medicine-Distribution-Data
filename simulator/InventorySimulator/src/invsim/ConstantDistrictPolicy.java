package invsim;

import java.io.PrintStream;

public class ConstantDistrictPolicy extends DistrictPolicy {
	private final int quantity;
	
	public ConstantDistrictPolicy(int quantity) {
		this.quantity = quantity;
	}
	
	
	@Override
	public int[] computeRetailerShipments(Simulator simulator) throws Exception {
		int R = simulator.R;
		int D = simulator.D;
		int[] districtIndex = simulator.districtIndex;
		RetailerAAA[] retailers = simulator.retailers;
		District[] districts = simulator.districts;
		
		int[] X = new int[R];
		
		long[] I = new long[D];
		for (int d = 0; d < D; ++d) {
			I[d] = districts[d].getInventoryLevel();
		}
		for (int r = 0; r < R; ++r) {
			//System.out.println(I + " " + quantity);
			int d = districtIndex[r];
			X[r] = (int) Math.min(I[d], quantity);
			I[d] -= X[r];
		}
		return X;
	}

	@Override
	public int[] computeDistrictShipments(Simulator simulator) throws Exception {
		int D = simulator.D;
		NationalWarehouse warehouse = simulator.warehouse;

		int[] X = new int[D];
		
		long I = warehouse.getInventoryLevel();
		for (int d = 0; d < D; ++d) {
			X[d] = (int) Math.min(I, quantity);
			I -= X[d];
		}

		return X;
	}

	@Override
	public void print(PrintStream out) {
		// TODO Auto-generated method stub
		
	}

}
