package invsim;

import java.io.PrintStream;

/**
 * Order-Up-To Inventory Policy
 * @author Zack
 *
 */
public class OutInvPol extends InvPol {
	public final int tHistory;
	public final double tOrder;
	public final String orderType;
	
	public static final String PAST_DEMAND = "past demand";
	public static final String FORECASTS = "forecasts";
	
	/**
	 * Use the last tHistory periods demand to compute the average
	 * demand in a period.  Then order up to tOrder periods of
	 * inventory.
	 * @param tHistory
	 * @param tOrder
	 */
	public OutInvPol(String orderType, int tHistory, double tOrder) {
		this.orderType = orderType;
		this.tHistory = tHistory;
		this.tOrder = tOrder;

		// Check the validity of the orderType parameter.
		if (isPastDemand())
			;
		else if (isForecast())
			;
		else
			throw new IllegalArgumentException();

	}
	
	@Override
	int[] computeShipments(InvSys sys) throws Exception {
		// Get current inventory levels.
		int T = (int) Math.round(Math.max(Math.ceil(tOrder), 12));
		DetInvSys dis = sys.getDetInvSys(T);
		long[] base = dis.I0;
		int R = base.length;
		
		// Compute the inventory position.
		Shipment[] X = dis.X;
		for (int i = 0; i < X.length; ++i) {
			base[X[i].destination] += X[i].quantity;
		}
		
		// Compute order up to level.
		int[] out = new int[R];
		if (isPastDemand()) {
			double[] mean = sys.meanDemand(tHistory);
			for (int r = 0; r < R; ++r)
				out[r] = (int) Math.round(Math.ceil(mean[r] * tOrder));
		}
		else if (isForecast()) {
			for (int r = 0; r < R; ++r)
				for (int t = 0; t < tOrder; ++t)
					out[r] += dis.dMean[r][t];
		}
		
		int[] Q = new int[R];
		int sum = 0;
		for (int r = 0; r < R; ++r) {
			if (dis.L[r][0] != DetInvSys.NO_LEAD_TIME) {
				Q[r] = (int) Math.round(Math.max(0, out[r] - base[r]));
				sum += Q[r];
			}
		}
		
		// If the total sent inventory exceeds the inventory
		// available, then rationing is needed
		if (sum > dis.Iware) {
			for (int r = 0; r < R; ++r) {
				Q[r] = (int) Math.floor((double) Q[r] * dis.Iware / sum);
			}
		}
		//System.out.println("sum = " + sum + "Iware = " + dis.Iware);
		return Q;
	}

	@Override
	void print(PrintStream out) {
		out.println("# Order-up-to Inventory Policy");
		out.println("# Order type = " + orderType);
		out.println("# tHistory = " + tHistory);
		out.println("# tOrder = " + tOrder);
		out.println("#");
	}

	private boolean isPastDemand() {
		return orderType.equals(PAST_DEMAND);
	}
	
	private boolean isForecast() {
		return orderType.equals(FORECASTS);
	}
}
