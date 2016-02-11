package invsim;

import java.io.PrintStream;

public class ConstantXDockPolicy extends XDockPolicy {
	private final int quantity;
	
	public ConstantXDockPolicy(int quantity) {
		this.quantity = quantity;
	}
	
	@Override
	public int[] computeShipments(Simulator simulator) {
		int R = simulator.R;
		NationalWarehouse warehouse = simulator.warehouse;
		RetailerAAA[] retailers = simulator.retailers;

		int[] X = new int[R];
		
		long I = warehouse.getInventoryLevel();
		for (int r = 0; r < R; ++r) {
			//System.out.println(I + " " + quantity);
			X[r] = (int) Math.min(I, quantity);
			I -= X[r];
		}
		return X;
	}

	@Override
	public void print(PrintStream out) {
		out.println("# Constant " + quantity + " policy\n\n");
	}

}
