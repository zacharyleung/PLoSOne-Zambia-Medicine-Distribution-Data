package invsim;

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
public abstract class Facility {
	/** The current time period. */
	protected int t;
	protected int tStart;
	protected int tEnd;
	
	/** The start of week inventory. */
	protected FlexArray1D<Long> invStart;
	/** The end of week inventory. */
	protected FlexArray1D<Long> invEnd;
	/** The shipments. */
	protected FlexArray1D<Long> shipmentArray;

	protected FlexArray1D<Long> orders;
	
	protected List<Shipment> shipmentList = new LinkedList<Shipment>();

	protected Inventory inventory = new Inventory();

	protected LeadTime leadTime;
	
	public void generate(int tStart, int tEnd, int randomSeed) {
		this.tStart = tStart;
		this.tEnd = tEnd;
		this.t = tStart;

		invStart = new FlexArray1D<>(tStart, tEnd + 1);
		invEnd = new FlexArray1D<>(tStart, tEnd);
		shipmentArray = new FlexArray1D<>(tStart, tEnd);
		orders = new FlexArray1D<>(tStart, tEnd);
		
		// Initialize the shipment array to 0, because it is not
		// set in periods when there are no shipments
		for (int t = tStart; t < tEnd; ++t) {
			shipmentArray.set(t, (long) 0);
		}
	}
	
	public void advanceToNextPeriod() {
		// Set the end of period inventory level
		invEnd.set(t, (long) inventory.getInventoryLevel());
		
		++t;

		// Remove expired drugs from the inventory and update
		// starting inventory levels
		inventory.update(t);		

		// Set the start of period inventory level
		invStart.set(t, (long) inventory.getInventoryLevel());
	}

	public void receiveShipments() {		
		// Shipments arrive at retailers
		for (ListIterator<Shipment> itr = shipmentList.listIterator(); itr.hasNext();) {
			Shipment s = itr.next();
			if (s.periodArrive == t) {
				inventory.addDrugs(s.inventory);
				itr.remove();
			}
		}
	}


	public void addShipment(Shipment shipment) throws IllegalArgumentException {
		shipmentList.add(shipment);
		shipmentArray.set(t, (long) shipment.quantity);
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

	public LeadTime getLeadTime() {
		return leadTime;
	}

	public long getOrders(int t) {
		return orders.get(t);
	}
	
	public long getShipmentQuantity(int t) {
		return shipmentArray.get(t);
	}

	public long getStartInventory(int t) {
		return invStart.get(t);
	}
		
	public Collection<Shipment> getShipments() {
		return Collections.unmodifiableCollection(shipmentList);
	}

	/**
	 * Set the total orders to the facility during period t to be
	 * equal to this quantity.
	 * @param t
	 * @param quantity
	 */
	public void setOrders(int t, long quantity) {
		orders.set(t, quantity);
	}

	
}
