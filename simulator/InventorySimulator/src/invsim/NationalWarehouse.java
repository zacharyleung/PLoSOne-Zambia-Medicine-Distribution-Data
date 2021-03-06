package invsim;

import d65helper.FlexArrayLong;
import d65helper.Helper;

public class NationalWarehouse extends Facility{
	private int[] shipmentSchedule;
	private int tExpiry;
	/** The length of a year. */
	private int Y;

	public NationalWarehouse(int[] shipmentSchedule) {
		this(shipmentSchedule, Integer.MAX_VALUE);
	}
	
	public NationalWarehouse(int[] shipmentSchedule, int tExpiry) {
		this.shipmentSchedule = shipmentSchedule;
		this.tExpiry = tExpiry;
		this.Y = shipmentSchedule.length;
	}

	/**
	 * Get this quantity of drugs from the warehouse inventory.
	 * @param quantity
	 * @throws IllegalArgumentException If inventory is less than quantity
	 * requested.
	 * @return
	 */
	public Inventory getDrugs(long quantity)
			throws Exception {
		if (quantity <= getInventoryLevel()) {
			return inventory.getDrugs(quantity);
		} else {
			throw new IllegalArgumentException(
					"Attempting to get more drugs than the inventory level!\n"
					+ "quantity = " + quantity
					+ " inventory = " + getInventoryLevel());
		}
	}
	
	public void receiveShipments() {
		int quantity = shipmentSchedule[Helper.modulo(t, Y)];
		inventory.addDrugs(quantity, tExpiry);
	}
	
	/**
	 * Get the shipment schedule for the horizon [t, t + nPeriods).
	 * @param t
	 * @param nPeriods
	 * @return
	 */
	public int[] getShipmentSchedule(int t, int nPeriods) {
		int[] result = new int[nPeriods];
		for (int i = 0; i < nPeriods; ++i) {
			result[i] = shipmentSchedule[Helper.modulo(t + i, Y)];
		}
		return result;
	}
	
	public int getNumberOfPeriodsInYear() {
		return Y;
	}
}
