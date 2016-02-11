package invsim3;

class Shipment {
	final int destination;
	final int periodSent;
	final int periodArrive;
	final int leadTime;
	final int quantity;
	final Inventory inventory;
	final double[] leadTimePmf;
	
	/**
	 * Copy the lead time pmf, to protect against any modifications
	 * to its value.
	 * @param destination
	 * @param periodSent
	 * @param leadTime
	 * @param quantity
	 * @param leadTimePmf
	 */
	Shipment(int destination, int periodSent, int leadTime,
			Inventory shipmentInventory, double[] leadTimePmf) {
		this.destination = destination;
		this.periodSent = periodSent;
		this.leadTime = leadTime;
		this.inventory = shipmentInventory;
		this.quantity = shipmentInventory.getInventoryLevel();
		
		this.leadTimePmf = new double[leadTimePmf.length];
		for (int i = 0; i < leadTimePmf.length; ++i)
			this.leadTimePmf[i] = leadTimePmf[i];
		
		periodArrive = periodSent + leadTime;
	}
	
	public String toString() {
		return "Shipment (dest: " + destination + ", sent: " + periodSent
				+ ", lead time: " + leadTime + ", quantity: "
				+ inventory.getInventoryLevel() + ")";
	}
}
