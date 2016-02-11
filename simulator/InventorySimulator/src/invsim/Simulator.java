package invsim;

import invsim.InvSys.Stats;

import java.io.File;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

import d65helper.Helper;

public class Simulator {
	final RetailerAAA[] retailers;
	final District[] districts;
	final NationalWarehouse warehouse;
	/** The current time period. */
	private int tCurrent;
	private Policy policy;
	public final int R;
	public final int Y;
	/** Number of districts. */
	public final int D;
	private int tStart;
	private int tEnd;
	
	/**
	 * It is assumed that the demand means and accessibility and the
	 * warehouse shipment schedule all have the same number of periods
	 * in a year.
	 * @param warehouse
	 * @param retailers
	 */
	public Simulator(NationalWarehouse warehouse, RetailerAAA[] retailers) {
		this.warehouse = warehouse;
		this.retailers = retailers;
		
		R = retailers.length;
		Y = warehouse.getNumberOfPeriodsInYear();

		// Make sure that the retailers have the same number of
		// periods in a year
		for (int r = 0; r < R; ++r) {
			if (Y != retailers[r].getNumberOfPeriodsInYear()) {
				throw new IllegalArgumentException(
						"The number of years don't match!");
			}
		}
	}

	public void simulate(Policy policy, int nYears, int randomSeed)
			throws Exception {
		this.policy = policy;

		tStart = -Y;
		tEnd = Y * nYears;

		// Generate the warehouse and retailers
		warehouse.generate(tStart, tEnd, 0);
		for (int i = 0; i < D; ++i) 
			districts[i].generate(tStart, tEnd, 0);
		
		for (int r = 0; r < R; ++r) {
			// Make sure each retailer gets a different random seed
			retailers[r].generate(tStart, tEnd, randomSeed + r);
		}

		for (int t = tStart; t < tEnd; ++t) {
			//			System.out.format("Simulator.simulate() period %d\n", t);
			tCurrent = t;
			// Shipments arrive
			warehouse.receiveShipments();
			for (int r = 0; r < R; ++r) {
				retailers[r].receiveShipments();
			}


			// Make shipment decisions
			if (XDockPolicy.class.isInstance(policy)) {
				XDockPolicy myPolicy = (XDockPolicy) policy;
				int[] X = myPolicy.computeShipments(this);
				for (int r = 0; r < R; ++r) {
					addShipment(warehouse, retailers[r], X[r]);
					//System.out.format("X[%d] = %d\n", r, X[r]);
					// If the policy decides to ship to retailer r
				}
			} else { // is DistrictPolicy
				DistrictPolicy myPolicy = (DistrictPolicy) policy;
				int[] X = myPolicy.computeRetailerShipments(this);
				for (int r = 0; r < R; ++r) {
					int d = districtIndex[r];
					addShipment(districts[d], retailers[r], X[r]);
					//System.out.format("X[%d] = %d\n", r, X[r]);
					// If the policy decides to ship to retailer r
				}
				X = myPolicy.computeDistrictShipments(this);
				for (int i = 0; i < D; ++i) {
					addShipment(warehouse, districts[i], X[i]);
				}
			}


			// Demand appears at retailers
			for (int r = 0; r < R; ++r) {
				retailers[r].demandAppears();
			}

			warehouse.advanceToNextPeriod();
			for (int r = 0; r < R; ++r) {
				retailers[r].advanceToNextPeriod();
			}
		}
	}

	public void print(PrintStream out) {
		out.println("Simulator.print()");

		// Print header
		out.print("Period\tWareInv");
		for (int r = 0; r < R; ++r) {
			out.format("\tRe%dInv\tRe%dDem\tRe%dUnm\tRe%dShp", r, r, r, r);
		}
		for (int d = 0; d < D; ++d) {
			out.format("\tRe%dInv\tRe%dOrd\tRe%dUnm\tRe%dShp", d, d, d);
		}
		out.println();

		for (int t = 0; t < tEnd; ++t) {
			out.format("%d\t%d", t, warehouse.getStartInventory(t));
			for (int r = 0; r < R; ++r) {
				out.format("\t%d\t%d\t%d\t%d", retailers[r].getStartInventory(t),
						retailers[r].getDemand(t),
						retailers[r].getUnmetDemand(t),
						retailers[r].getShipmentQuantity(t));
			}
			for (int d = 0; d < D; ++d) {
				out.format("\tRe%dInv\tRe%dOrd\tRe%dShp",
						districts[d].getStartInventory(t),
						districts[d].getOrders(t),
						districts[d].getShipmentQuantity(t));
			}
			out.println();
		}
	}

	public int getCurrentTimePeriod() {
		return tCurrent;
	}

	public Stats getStats() {
		long demandTotal = 0;
		long demandUnmet = 0;
		long inventoryTotal = 0;

		for (int r = 0; r < R; ++r) {
			RetailerAAA.Stats stats = retailers[r].getStats();
			demandTotal += stats.demandTotal;
			demandUnmet += stats.demandUnmet;
			inventoryTotal += stats.inventoryTotal;
		}

		return new Stats(demandTotal, demandUnmet, inventoryTotal, tEnd);
	}

	private void addShipment(Facility supply, Facility demand, int quantity)
			throws Exception {
		int t = tCurrent;
		LeadTime leadTime = demand.getLeadTime();
		if (quantity > 0) {
			// If it is a shipment week, then make a shipment
			if (leadTime.isTotalShipmentPeriod(t)) {
				Inventory shipment = supply.getDrugs(quantity);
				double[] pmf = leadTime.getTotalLeadTimePmf(t, 1)[0];
				int l = leadTime.getTotalLeadTime(t);
				demand.addShipment(
						new Shipment(0, t, l, shipment, pmf));
			} else { // Something wrong happened!
				throw new Exception(
						"Attempted shipment made during non-shipment period!");
			}
		}

	}

	/**
	 * Create a simulator based on input files in a containing folder.
	 * The simulator will have Y periods in a year.
	 */
	public static Simulator readInputFiles(String folder,
			double supplyDemandRatio, double[] forecastVariance,
			double[] forecastAccuracy, int forecastLevel) throws Exception {

		// Number of shipments that warehouse receives in a year
		int nWarehouseShipments = 4;

		File file = null;

		file = new File(new File(folder), "year.txt");
		int Y = Helper.readInt(file);

		file = new File(new File(folder), "retailers.txt");
		int R = Helper.readInt(file);

		file = new File(new File(folder), "accessibility.txt");
		double[][] accessibility = Helper.readDouble2D(file, R, Y);
		for (int r = 0; r < R; ++r)
			for (int t = 0; t < Y; ++t)
				accessibility[r][t] /= 100;

		file = new File(new File(folder), "demand-means.txt");
		double[][] demandMean = Helper.readDouble2D(file, R, Y);

		file = new File(new File(folder), "lead-times.txt");
		double[][] lead = Helper.readDouble2D(file, R, 4);

		RetailerAAA[] retailers = new RetailerAAA[R];
		for (int r = 0; r < R; ++r) {
			Demand mmfe = new MultMmfeDemand(demandMean[r],
					forecastVariance, forecastAccuracy, forecastLevel);

			int nPeriodsInCycle = (int) Math.round(lead[r][0]);
			int cycleOffset = (int) Math.round(lead[r][1]);
			int minLeadTime = (int) Math.round(lead[r][2]);
			double meanLeadTime = lead[r][3];
			LeadTime leadTime = new GeometricLeadTime(nPeriodsInCycle, cycleOffset,
					minLeadTime, meanLeadTime, accessibility[r]);

			retailers[r] = new RetailerAAA(mmfe, leadTime);
		}

		double sum = 0;
		for (int r = 0; r < R; ++r)
			for (int y = 0; y < Y; ++y)
				sum += demandMean[r][y];

		int[] shipmentSchedule = new int[Y];
		for (int k = 0; k < nWarehouseShipments; ++k) {
			int t = (int) Math.round((double) k * Y / nWarehouseShipments); 
			shipmentSchedule[t] = (int) Math.round(
					supplyDemandRatio * sum / nWarehouseShipments);
		}

		NationalWarehouse warehouse = new NationalWarehouse(shipmentSchedule);

		return new Simulator(warehouse, retailers);
	}

	public static class Stats {
		/** Proportion of demand that is served. */
		public final double serviceLevel;
		/** Mean demand per period. */
		public final double demandPerPeriod;
		/** Mean inventory level per period. */
		public final double inventoryPerPeriod;
		/**
		 * Mean inventory level in units of mean demand per period.
		 */
		public final double inventoryInDemandPerPeriod;

		public final long demandTotal;
		public final long demandUnmet;
		public final long inventoryTotal;

		Stats(long demandTotal, long demandUnmet, long inventoryTotal, long T) {
			this.demandTotal = demandTotal;
			this.demandUnmet = demandUnmet;
			this.inventoryTotal = inventoryTotal;

			serviceLevel = 1 - (double) demandUnmet / demandTotal;
			inventoryPerPeriod = (double) inventoryTotal / T;
			demandPerPeriod = (double) demandTotal / T;
			inventoryInDemandPerPeriod = (double) inventoryPerPeriod / demandPerPeriod;
		}

		public void print(PrintStream out) {
			out.println("Simulator.Stats.print()");
			out.format("Service level = %.4f", serviceLevel);
			out.println();
			out.format("Mean inventory level = %.1f periods",
					inventoryInDemandPerPeriod);
			out.println();
			out.format("Total demand = %d", demandTotal);
			out.println();
			out.format("Total demand unmet = %d", demandUnmet);
			out.println();
			out.format("Total inventory = %d", inventoryTotal);
			out.println();
		}
	}

	public static void main(String[] args) throws Exception {
		//testConstant();
		//testOrderUpTo();
		testReadInputFiles();
	}

	/**
	 * Test how well the simulator works with constant demand,
	 * constant lead time, and a constant shipment policy.
	 */
	private static void testConstant() throws Exception {
		int tStart = -5;
		int tEnd = 10;
		int Y = 5;

		int R = 2;
		RetailerAAA[] retailers = new RetailerAAA[R];
		for (int r = 0; r < R; ++r) {
			retailers[r] = new RetailerAAA(
					new ConstantDemand(4*(r+1), Y), new ConstantLeadTime(r+2, Y));
		}

		int[] shipmentSchedule = {15, 15, 15, 15, 15};
		NationalWarehouse warehouse = new NationalWarehouse(shipmentSchedule);

		//Policy policy = new ConstantXDockPolicy(5);
		Policy policy = new ConstantDistrictPolicy(5);
		
		Simulator sim = new Simulator(warehouse, retailers);

		sim.simulate(policy, 2, 0);

		sim.print(System.out);
	}

	private static void testOrderUpTo() throws Exception {
		int Y = 12;
		int tStart = -12;
		int tEnd = 24;

		double[] access = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
		GeometricLeadTime geo = new GeometricLeadTime(4, 1, 2,
				4, access);

		double[] mean = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
		double[][] cov = {{9, 0, 0}, {0, 4, 0}, {0, 0, 1}};
		//		double[] forecastAccuracy = {0, 1};
		//		AdditiveMmfeDemand demand = new AdditiveMmfeDemand(mean, cov,
		//				forecastAccuracy, 0);

		double[] var = {0.1, 0.00001, 0.00001};
		double[] forecastAccuracy = {0, 1};
		int forecastLevel = 1;
		MultMmfeDemand demand = new MultMmfeDemand(mean, var, forecastAccuracy,
				forecastLevel);

		int R = 1;
		RetailerAAA[] retailers = new RetailerAAA[R];
		for (int r = 0; r < R; ++r) {
			retailers[r] = new RetailerAAA(demand, geo);
		}

		int[] shipmentSchedule = {12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12};
		NationalWarehouse warehouse = new NationalWarehouse(shipmentSchedule);

		Policy policy = new OrderUpToXDockPolicy(
				OrderUpToXDockPolicy.PAST_DEMAND, 12, 12);

		policy = new OptimizationPolicy(24, 100, OptimizationPolicy.MULTI_PERIOD,
				20, OptimizationPolicy.CONSERVATIVE);

		Simulator sim = new Simulator(warehouse, retailers);

		sim.simulate(policy, 2, 0);

		sim.print(System.out);

	}

	private static void testReadInputFiles() throws Exception {
		String dirName = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1";

		double[] forecastVariance = {0.1, 0.00001, 0.00001};
		double[] forecastAccuracy = {0, 1};
		int forecastLevel = 1;

		Policy policy = new OrderUpToXDockPolicy(
				OrderUpToXDockPolicy.PAST_DEMAND, 12, 16);

		policy = new OptimizationPolicy(24, 100, OptimizationPolicy.MULTI_PERIOD,
				10, OptimizationPolicy.CONSERVATIVE);

		Simulator sim = readInputFiles(dirName, 2.0, forecastVariance,
				forecastAccuracy, forecastLevel);

		sim.simulate(policy, 2, 0);

		sim.print(System.out);
		sim.getStats().print(System.out);
	}
}
