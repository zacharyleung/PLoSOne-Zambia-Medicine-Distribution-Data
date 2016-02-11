package invsim;

import java.io.PrintStream;

public class OrderUpToXDockPolicy extends XDockPolicy {
	public final int tHistory;
	public final double tOrder;
	public final String orderType;
	
	public static final String PAST_DEMAND = "past demand";
	public static final String FORECASTS = "forecasts";
	
	/**
	 * Order up to an inventory level of tOrder periods of
	 * demand.
	 * The mean period demand is estimated either using the last
	 * tHistory periods of observed demand, or using the demand
	 * forecast.  
	 * @param tHistory
	 * @param tOrder
	 */
	public OrderUpToXDockPolicy(String orderType, int tHistory, double tOrder) {
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
	public int[] computeShipments(Simulator simulator) {
		int R = simulator.R;
		int t = simulator.getCurrentTimePeriod();
		NationalWarehouse warehouse = simulator.warehouse;
		RetailerAAA[] retailers = simulator.retailers;

		System.out.println("OrderUpToPolicy.computeShipments() week " + t);

		// The current inventory position for each retailer
		long[] base = new long[R];
		for (int r = 0; r < R; ++r) {
			base[r] = retailers[r].getInventoryPosition();
//			retailers[r].print(System.out);
//			System.out.format("base[%d] = %d", r, base[r]);
//			System.out.println();
//			System.out.println(" " + retailers[r].getInventoryLevel());
		}
		
		
		// Compute order up to level.
		int[] out = new int[R];
		for (int r = 0; r < R; ++r) {
			// Make an order only if it is a shipment period
			if (retailers[r].leadTime.isTotalShipmentPeriod(t)) {
				if (isPastDemand()) {
					double sum = 0;
					for (int k = 0; k < tHistory; ++k) {
						sum += retailers[r].getDemand(t - 1 - k);
					}
					out[r] = (int) Math.round(Math.ceil(sum * tOrder / tHistory));
				} else if (isForecast()) {
					double sum = 0;
					int tOrderInt = (int) Math.round(Math.ceil(tOrder));
					Forecast f = retailers[r].demand.getForecast(t, tOrderInt);
					for (int k = 0; k < tOrderInt; ++k) {
						sum += f.mean[k];
					}
					out[r] = (int) sum;
				}
//				System.out.format("out[%d] = %d", r, out[r]);
//				System.out.println();

			}
		}
		
		int[] Q = new int[R];
		int sumOfOrders = 0;
		for (int r = 0; r < R; ++r) {
			if (retailers[r].leadTime.isTotalShipmentPeriod(t)) {
				Q[r] = (int) Math.round(Math.max(0, out[r] - base[r]));
				sumOfOrders += Q[r];
			}
		}
		
		// If the total sent inventory exceeds the inventory
		// available, then rationing is needed
		long wareInv = warehouse.getInventoryLevel(); 
		if (sumOfOrders > wareInv) {
			for (int r = 0; r < R; ++r) {
				Q[r] = (int) Math.floor((double) Q[r] * wareInv / sumOfOrders);
			}
		}
		//System.out.println("sum = " + sum + "Iware = " + dis.Iware);
		return Q;
	}

	public void print(PrintStream out) {
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
