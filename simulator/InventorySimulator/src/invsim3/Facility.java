package invsim3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import d65helper.FlexArray1D;

/**
 * A Facility is an object which represents a location that holds
 * inventory, such as the national or district warehouses, and the
 * retailer.
 *  
 * @author zacleung
 *
 */
public abstract class Facility extends SimulatorEntity {
	/** The start of week inventory. */
	protected FlexArray1D<Long> invStart;
	/** The end of week inventory. */
	protected FlexArray1D<Long> invEnd;
	/**
	 * shipmentArray(t) = the quantity sent to the facility in period t.
	 */
	protected FlexArray1D<Long> shipmentArray;
	/**
	 * issueArray(t) = the quantity issued by the facility in period t.
	 */	
	protected FlexArray1D<Long> issueArray;

	protected FlexArray1D<Long> orders;
	
	protected List<Shipment> shipmentList = new LinkedList<Shipment>();

	protected Inventory inventory = new Inventory();
	
	public void generate(int randomSeed) {
		int tStart = simulator.getStartPeriod();
		int tEnd = simulator.getEndPeriod();
		int Y = simulator.getNumberOfPeriodsInYear();
		
		invStart = new FlexArray1D<>(tStart, tEnd + 1);
		invEnd = new FlexArray1D<>(tStart, tEnd);
		shipmentArray = new FlexArray1D<>(tStart, tEnd);
		orders = new FlexArray1D<>(tStart, tEnd);

		issueArray = new FlexArray1D<>(tStart - Y, tEnd);
		for (int t = tStart - Y; t < tEnd; ++t) {
			issueArray.set(t, (long) 0);
		}
		
		// Initialize the shipment array to 0, because it is not
		// set in periods when there are no shipments
		for (int t = tStart; t < tEnd; ++t) {
			shipmentArray.set(t, (long) 0);
			// Initialize the shipment array to 0, because it is not
			// set in periods when there are no shipments
			invStart.set(t, (long) 0);
			invEnd.set(t, (long) 0);
		}
	}
	
	public void advanceToNextPeriod() {
		int t = getCurrentTimePeriod();
		
		// Set the end of period inventory level
		invEnd.set(t, (long) inventory.getInventoryLevel());
		
		++t;

		// Remove expired drugs from the inventory and update
		// starting inventory levels
		inventory.update(t);		

		// I decided to set the start of period inventory level after
		// receiving shipments
		// Set the start of period inventory level
		//invStart.set(t, (long) inventory.getInventoryLevel());
		
//		System.out.println("Facility.advanceToNextPeriod()");
//		for (Shipment s : shipmentList) {
//			System.out.println(s);
//		}
	}

	public void receiveShipments() {
		int t = getCurrentTimePeriod();
		
		//System.out.println("Facility.receiveShipments() Period = " + t);
		// Shipments arrive at retailers
		for (ListIterator<Shipment> itr = shipmentList.listIterator(); itr.hasNext();) {
			Shipment s = itr.next();
			if (s.periodArrive == t) {
				inventory.addDrugs(s.inventory);
				itr.remove();
			}
		}
		// Set the start of period inventory level
		invStart.set(t, (long) inventory.getInventoryLevel());
	}


	public void addShipment(Shipment shipment) throws IllegalArgumentException {
		int t = getCurrentTimePeriod();
		shipmentList.add(shipment);
		shipmentArray.set(t, (long) shipment.quantity);
//		System.out.println("Facility.addShipment()");
//		System.out.println(shipment);
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
		int t = getCurrentTimePeriod();

		if (quantity <= getInventoryLevel()) {
			long temp = issueArray.get(t) + quantity;
			issueArray.set(t, temp);
			return inventory.getDrugs(quantity);
		} else {
			throw new IllegalArgumentException(
					"Attempting to get more drugs than the inventory level!\n"
					+ "quantity = " + quantity
					+ " inventory = " + getInventoryLevel());
		}
	}

	public long getInventoryLevel() {
		return inventory.getInventoryLevel();
	}

	public long getInventoryPosition() {
		long result = inventory.getInventoryLevel();
		for (Shipment s : shipmentList) {
			result += s.quantity;
		}
		return result;
	}

	public long getOrders(int t) {
		return orders.get(t);
	}
	
	/** Return the quantity sent to the facility in period t. */
	public long getShipmentQuantity(int t) {
		return shipmentArray.get(t);
	}

	public long getIssues(int t) {
		return issueArray.get(t);
	}

	public long getStartInventory(int t) {
		return invStart.get(t);
	}
		
	public Collection<Shipment> getShipments() {
		return Collections.unmodifiableCollection(shipmentList);
	}

	/**
	 * Add this quantity to the quantity of the orders received
	 * during period t.
	 * @param t
	 * @param quantity
	 */
	public void addOrder(int t, long quantity) throws Exception {
		if (quantity < 0)
			throw new Exception("Quantity is less than 0!" + quantity);
		long temp = orders.get(t);
		orders.set(t, temp + quantity);
	}

	
}
