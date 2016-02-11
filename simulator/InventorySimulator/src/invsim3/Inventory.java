package invsim3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The Inventory object keeps accounts of the inventory of drugs,
 * including removing expired drugs.  Drugs are issued in a first
 * expiry first out (FEFO) order. 
 * @author Zack
 *
 */
class Inventory {
	/**
	 * A list of the drugs, with the drugs closest to expiry at the
	 * front of the list.
	 */
	private LinkedList<Drug> drugList;

	public static final boolean shouldExpire = false;
	
	Inventory() {
		this(new LinkedList<Drug>());
	}
	
	Inventory(LinkedList<Drug> drugList) {
		this.drugList = drugList;
	}
	
	Inventory(int quantity, int tExpiry) {
		this();
		drugList.add(new Drug(quantity, tExpiry));
	}
	
	/** Return the total inventory level. */
	int getInventoryLevel() {
		int sum = 0;
		for (Iterator<Drug> itr = drugList.iterator(); itr.hasNext();) {
			sum += itr.next().quantity;
		}
		return sum;
	}
	
	/**
	 * Add new drugs into the inventory with an expiry date of tExpiry.
	 * @param quantity The quantity of drugs received.
	 * @param tExpiry The time period when these drugs will expire.
	 */
	void addDrugs(int quantity, int tExpiry) {
		// If the quantity is zero, don't do anything
		if (quantity == 0) {
			;
		} else {
			drugList.add(new Drug(quantity, tExpiry));
		}
	}

	/**
	 * Add new drugs into the inventory.  Since the new drugs that are
	 * in the shipment have later expiry dates than those currently in
	 * the inventory. 
	 * @param inventory
	 */
	void addDrugs(Inventory inventory) {
		drugList.addAll(inventory.drugList);
	}
	
	/**
	 * Return a new Inventory object which contains the drugs that
	 * are most close to their expiry date.
	 * @return
	 */
	Inventory getDrugs(long quantity) throws Exception {
		if (quantity > getInventoryLevel())
			throw new Exception("Quantity exceeds inventory level!");
		
		LinkedList<Drug> shipment = new LinkedList<>();
		int sum = 0;
		Iterator<Drug> itr = drugList.iterator();
		while (itr.hasNext()) {
			Drug drug = itr.next();
		    itr.remove();
		    sum += drug.quantity;
		    if (sum < quantity) {
		    	shipment.add(drug);
		    } else { // We have removed more drugs than requested
			    int excess = sum - (int) quantity;
			    int consumed = drug.quantity - excess;
			    shipment.add(new Drug(consumed, drug.tExpiry));
			    drugList.addFirst(new Drug(excess, drug.tExpiry));
		    	break;
		    }
		}

		return new Inventory(shipment);
	}
	
	/**
	 * At the beginning of time period t, this method should be called.
	 * The method removes from the inventory any expired drugs.
	 * @param t
	 */
	void update(int t) {
		if (shouldExpire) {
			System.out.println("WARNING: EXPIRY TURNED ON!");
		for (Iterator<Drug> itr = drugList.iterator(); itr.hasNext();) {
			Drug drug = itr.next();
			if (drug.tExpiry < t) {
				itr.remove();
			}
		}
		}
	}
	
	void print() {
		System.out.println("Printing Inventory object.");
		for (Iterator<Drug> itr = drugList.iterator(); itr.hasNext();) {
			Drug drug = itr.next();
			System.out.format("quantity = %d\texpiry=%d\n",
					drug.quantity, drug.tExpiry);
		}
	}
	
	class Drug {
		int quantity;
		int tExpiry;
		Drug(int quantity, int tExpiry) {
			this.quantity = quantity;
			this.tExpiry = tExpiry;
			//System.out.println("WARNING: EXPIRY TURNED OFF!");
			//this.tExpiry = Integer.MAX_VALUE;
		}
	}
}
