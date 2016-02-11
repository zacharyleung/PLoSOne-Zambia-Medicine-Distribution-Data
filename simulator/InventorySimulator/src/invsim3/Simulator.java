package invsim3;

import java.io.PrintStream;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import com.zacleung.invsim.policy.ConstantDistrictPolicy;
import com.zacleung.invsim.policy.ConstantXDockPolicy;
import com.zacleung.invsim.policy.DistrictPolicy;
import com.zacleung.invsim.policy.OrderUpToDistrictPolicy;
import com.zacleung.invsim.policy.Policy;
import com.zacleung.invsim.policy.XDockPolicy;
import com.zacleung.util.Vector;

public class Simulator {
	public final LeadTime leadTime;
	public final Retailer[] retailers;
	public District[] districts;
	public final NationalWarehouse national;
	private Policy policy;
	private Parameters parameters = null;
	
	
	public static enum Status {NOT_STARTED, IS_RUNNING, FINISHED};
	private Status status = Status.NOT_STARTED;
	
	// Time variables

	/** The current time period. */
	private int tCurrent;
	private int tStart;
	private int tEnd;

	// Number variables
	public final int R;
	public final int Y;
	/** Number of districts. */
	public final int D;

	/**
	 * It is assumed that the demand means and accessibility and the
	 * warehouse shipment schedule all have the same number of periods
	 * in a year.
	 * @param national
	 * @param retailers
	 */
	public Simulator(NationalWarehouse national, Retailer[] retailers,
			LeadTime leadTime) {
		this.national = national;
		this.retailers = retailers;
		this.leadTime = leadTime;

		R = retailers.length;
		Y = national.getNumberOfPeriodsInYear();

		D = leadTime.getNumberOfDistricts();
		districts = new District[D];
		for (int i = 0; i < D; ++i) {
			districts[i] = new District();
			// Set the simulator for each of the SimulatorEntity objects
			districts[i].setSimulator(this);
		}

		// Make sure that the retailers have the same number of
		// periods in a year
		for (int r = 0; r < R; ++r) {
			// Set the simulator for each of the SimulatorEntity objects
			retailers[r].setSimulator(this);
			if (Y != retailers[r].getNumberOfPeriodsInYear()) {
				throw new IllegalArgumentException(
						"The number of years don't match!");
			}
		}
		
		// Set the simulator for each of the SimulatorEntity objects
		national.setSimulator(this);
	}


	
	public Status getStatus() {
		return status;
	}
	
	public Parameters getParameters() {
		return new Parameters(parameters);
	}
	
	
	public void simulate(Parameters parameters)
			throws Exception {
		status = Status.IS_RUNNING;
		this.parameters = new Parameters(parameters);
		this.policy = parameters.policy;

		if (parameters.numberOfWarmupYears == 0) {
			tStart = -parameters.numberOfWarmupPeriods;
		} else {
			tStart = -parameters.numberOfWarmupYears * Y;
		}

		//tStart = -4;
		tEnd = Y * parameters.numberOfSimulationYears;

		int randomSeed = parameters.randomSeed;

		// Generate the warehouse and retailers
		national.generate(0);
		for (int i = 0; i < D; ++i) 
			districts[i].generate(0);

		for (int r = 0; r < R; ++r) {
			// Make sure each retailer gets a different random seed
			retailers[r].generate(randomSeed + r);
		}

		// Generate an extra year of lead time data for the
		// clairvoyant policy
		leadTime.generate(tStart, tEnd + Y, randomSeed);



		for (int t = tStart; t < tEnd; ++t) {
			//System.out.format("Simulator.simulate() period %d\n", t);
			tCurrent = t;

			// Shipments arrive
			national.receiveShipments();
			for (int d = 0; d < D; ++d) {
				//				System.out.println("District" + d);
				//				for (Shipment s : districts[d].shipmentList) {
				//					System.out.println(s);
				//					System.out.println(s.periodArrive);
				//				}
				districts[d].receiveShipments();
			}
			for (int r = 0; r < R; ++r) {
				retailers[r].receiveShipments();
			}

			// Make shipment decisions
			if (XDockPolicy.class.isInstance(policy)) {
				XDockPolicy myPolicy = (XDockPolicy) policy;
				int[] X = myPolicy.computeShipments(this);
				for (int r = 0; r < R; ++r) {
					addNationalRetailerShipment(r, X[r]);
					//System.out.format("X[%d] = %d\n", r, X[r]);
					// If the policy decides to ship to retailer r
				}
			} else { // is DistrictPolicy
				DistrictPolicy myPolicy = (DistrictPolicy) policy;

				int[] X = myPolicy.computeRetailerShipments(this);
				for (int r = 0; r < R; ++r) {
					addDistrictRetailerShipment(r, X[r]);
					//System.out.format("X[%d] = %d\n", r, X[r]);
					// If the policy decides to ship to retailer r
				}

				X = myPolicy.computeDistrictShipments(this);
				for (int d = 0; d < D; ++d) {
					addNationalDistrictShipment(d, X[d]);
				}
			}

			// Demand appears at retailers
			for (int r = 0; r < R; ++r) {
				retailers[r].demandAppears();
			}

			national.advanceToNextPeriod();
			for (int d = 0; d < D; ++d) {
				districts[d].advanceToNextPeriod();
			}
			for (int r = 0; r < R; ++r) {
				retailers[r].advanceToNextPeriod();
			}
		}
		
		status = Status.FINISHED;
	}

	public int getCurrentTimePeriod() {
		return tCurrent;
	}
	
	public int getNumberOfPeriodsInYear() {
		return Y;
	}

	/** Get the start period of the simulation. */
	public int getStartPeriod() {
		return tStart;
	}
	
	/** Get the end period of the simulation. */
	public int getEndPeriod() {
		return tEnd;
	}

	
	private void addNationalRetailerShipment(int r, int quantity)
			throws Exception {
		int t = tCurrent;
		if (quantity > 0) {
			// If it is a shipment week, then make a shipment
			if (leadTime.isTotalShipmentPeriod(r, t)) {
				Inventory shipment = national.getDrugs(quantity);
				double[] pmf = leadTime.getTotalLeadTimePmf(r, t);
				int l = leadTime.getTotalLeadTime(r, t);
				retailers[r].addShipment(
						new Shipment(r, t, l, shipment, pmf));
			} else { // Something wrong happened!
				throw new Exception(
						"Attempted shipment made during non-shipment period!");
			}
		}

	}


	private void addDistrictRetailerShipment(int r, int quantity)
			throws Exception {
		int t = tCurrent;
		int d = leadTime.R2D[r];
		if (quantity > 0) {
			// If it is a shipment week, then make a shipment
			if (leadTime.isSecondaryShipmentPeriod(r, t)) {
				Inventory shipment = districts[d].getDrugs(quantity);
				double[] pmf = new double[0]; // not needed
				int l = leadTime.getSecondaryLeadTime(r, t);
				retailers[r].addShipment(
						new Shipment(r, t, l, shipment, pmf));
			} else { // Something wrong happened!
				throw new Exception(
						"Attempted shipment made during non-shipment period!");
			}
		}

	}


	private void addNationalDistrictShipment(int d, int quantity)
			throws Exception {
		int t = tCurrent;
		if (quantity > 0) {
			// If it is a shipment week, then make a shipment
			if (leadTime.isPrimaryShipmentPeriod(d, t)) {
				Inventory shipment = national.getDrugs(quantity);
				double[] pmf = new double[0]; // not needed
				int l = leadTime.getPrimaryLeadTime(d, t);
				districts[d].addShipment(
						new Shipment(d, t, l, shipment, pmf));
			} else { // Something wrong happened!
				throw new Exception(
						"Attempted shipment made during non-shipment period!");
			}
		}
	}

	public Stats getStats() {
		return new Stats(this);
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
		
		public final double maxInventoryInDemandPerPeriod;

		public final long demandTotal;
		public final long demandUnmet;
		public final long inventoryTotal;

		/** Standard deviation of the retailer service levels. */
		public final double serviceLevelStdDev;

		/** The service level of retailer r. */
		private double[] retailerSL;
		/** The mean total lead time of retailer r. */
		private double[] meanLeadTime;
		public final int R;

		Stats(Simulator simulator) {
			Retailer[] retailers = simulator.retailers;
			int T = simulator.tEnd;
			LeadTime leadTime = simulator.leadTime;

			R = retailers.length;

			long demandTotal = 0;
			long demandUnmet = 0;
			long inventoryTotal = 0;

			retailerSL = new double[R];
			meanLeadTime = new double[R];
			SummaryStatistics sl = new SummaryStatistics();
			double maxInvTemp = 0;
			for (int r = 0; r < R; ++r) {
				Retailer.Stats stats = retailers[r].getStats();
				demandTotal += stats.demandTotal;
				demandUnmet += stats.demandUnmet;
				inventoryTotal += stats.inventoryTotal;
				retailerSL[r] = stats.serviceLevel;
				SummaryStatistics sumStat = new SummaryStatistics();
				for (int t = 0; t < T; ++t) {
					if (leadTime.isTotalShipmentPeriod(r, t)) {
						sumStat.addValue(leadTime.getTotalLeadTime(r, t));
					}
				}
				meanLeadTime[r] = sumStat.getMean();
				sl.addValue(stats.serviceLevel);
				maxInvTemp = Math.max(maxInvTemp, stats.maxInventoryInDemandPerPeriod);
			}
			this.serviceLevelStdDev = sl.getStandardDeviation();

			this.demandTotal = demandTotal;
			this.demandUnmet = demandUnmet;
			this.inventoryTotal = inventoryTotal;
			this.maxInventoryInDemandPerPeriod = maxInvTemp;

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

		/** Print retailer statistics. */
		public void printRetailers(PrintStream out) {
			out.println("Index\tSL\tMLT");
			for (int r = 0; r < R; ++r) {
				out.format("%d\t%.4f\t%.2f", r, retailerSL[r], meanLeadTime[r]);
				out.println();
			}
		}
	}


	/**
	 * For the first retailer, compute for each period the probability of
	 * being stocked out.
	 */
	public double[] getStockOuts() {
		double[] totalStockouts = new double[Y];
		double[] stockoutProbability = new double[Y];

		int[] count = new int[Y];
		Retailer retailer = retailers[0];
		// Sum the stockout probability over each period
		for (int t = 0; t < tEnd; ++t) {
			int y = t % Y;
			//				if (retailer.getUnmetDemand(t) > 0) {
			//					System.out.println(retailer.getUnmetDemand(t));
			//					System.out.println(retailer.getDemand(t));
			//				}
			long demand = retailer.getDemand(t);
			// To avoid division by zero, check that demand is not zero
			if (demand == 0) {
				totalStockouts[y] += 0;
			} else {
				totalStockouts[y] += 
						(float) retailer.getUnmetDemand(t) / retailer.getDemand(t);
			}
			++count[y];
		}
		// Compute the mean stockout probability for each period
		for (int y = 0; y < Y; ++y) {
			stockoutProbability[y] = totalStockouts[y] / count[y];
		}
		
		return stockoutProbability;
	}




	public static class Parameters {
		/**
		 * Specify either the number of warmup periods or the number
		 * of warmup years, not both.
		 * If one is nonzero, then the other is zero.
		 */
		public int numberOfWarmupPeriods = 0;
		public int numberOfWarmupYears = 0;
		public int numberOfSimulationYears = 0;
		public int randomSeed = 0;
		public Policy policy = null;
		
		public Parameters() {}
		
		public Parameters(Parameters parameters) {
			this.numberOfSimulationYears = parameters.numberOfSimulationYears;
			this.numberOfWarmupPeriods = parameters.numberOfWarmupPeriods;
			this.numberOfSimulationYears = parameters.numberOfSimulationYears;
			this.randomSeed = parameters.randomSeed;
			this.policy = parameters.policy;
		}
		
		public Parameters withRandomSeed(int randomSeed) {
			this.randomSeed = randomSeed;
			return this;
		}
		
		public Parameters withNumberOfWarmupYears(int numberOfWarmupYears) {
			this.numberOfWarmupYears = numberOfWarmupYears;
			this.numberOfWarmupPeriods = 0;
			return this;
		}
		
		public Parameters withNumberOfWarmupPeriods(int numberOfWarmupPeriods) {
			this.numberOfWarmupPeriods = numberOfWarmupPeriods;
			this.numberOfWarmupYears = 0;
			return this;
		}
		
		public Parameters withNumberOfSimulationYears(int numberOfSimulationYears) {
			this.numberOfSimulationYears = numberOfSimulationYears;
			return this;
		}
	
		
		public Parameters withPolicy(Policy policy) {
			this.policy = policy;
			return this;
		}
				
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder()
			.append(String.format("# Number of warmup years = %d%n",
					numberOfWarmupYears))
			.append(String.format("# Number of warmup periods = %d%n",
					numberOfWarmupPeriods))
			.append(String.format("# Number of simulation years = %d%n",
					numberOfSimulationYears))
			.append(policy.toString());
			return sb.toString();
		}
	
	}




	public static class Builder {
		private int numberOfPeriodsInYear;
		private int numberOfRetailers;
		private Retailer[] retailers;
		private NationalWarehouse warehouse;

		private GeometricLeadTime.Builder leadTimeBuilder;
		
		private double scalePreSimulationDemand = 1.0;
		private double _scaleDemandMean = 1.0;
		private int numberOfWarehouseShipmentsPerYear;
		private double supplyDemandRatio;
		private double[][] demandMean;
		private double[] forecastVariance;
		private double[] forecastAccuracy;
		private int forecastLevel;
		private double demandSeasonality = NONE;
		
		private static double NONE = -1;

		public Builder withNumberOfWarehouseShipmentsPerYear(int numberOfWarehouseShipmentsPerYear) {
			this.numberOfWarehouseShipmentsPerYear = numberOfWarehouseShipmentsPerYear;
			return this;
		}

		public Builder withSupplyDemandRatio(double supplyDemandRatio) {
			this.supplyDemandRatio = supplyDemandRatio;
			return this;
		}

		public Builder withLeadTimeBuilder(GeometricLeadTime.Builder leadTimeBuilder) {
			this.leadTimeBuilder = leadTimeBuilder;
			return this;
		}
		
		public Builder withForecastVariance(double[] forecastVariance) {
			this.forecastVariance = forecastVariance.clone();
			return this;
		}

		public Builder withForecastAccuracy(double[] forecastAccuracy) {
			this.forecastAccuracy = forecastAccuracy.clone();
			return this;
		}

		public Builder withDemandMean(double[][] dm) {
			this.demandMean = new double[dm.length][];
			for (int i = 0; i < dm.length; ++i) {
				this.demandMean[i] = dm[i].clone();
			}
			return this;
		}

		public Builder withScaleDemandMean(double scaleDemandMean) {
			this._scaleDemandMean = scaleDemandMean;
			return this;
		}

		public Builder withScalePreSimulationDemand(double scalePreSimulationDemand) {
			this.scalePreSimulationDemand = scalePreSimulationDemand;
			return this;
		}

		public Builder withDemandSeasonality(double demandSeasonality) {
			this.demandSeasonality = demandSeasonality;
			return this;
		}
		
		public double getDemandSeasonality() {
			return demandSeasonality;
		}
		
		public GeometricLeadTime.Builder getLeadTimeBuilder() {
			return leadTimeBuilder;
		}
		
		public Simulator build() throws RuntimeException {
			numberOfRetailers = demandMean.length;			
			numberOfPeriodsInYear = demandMean[0].length;

			buildRetailers();

			buildWarehouse();

			return new Simulator(warehouse, retailers, leadTimeBuilder.build());
		}

		/**
		 * 
		 */
		private void buildRetailers() {
			//			System.out.println("Simulator.Builder.buildRetailers()");
			//			System.out.printf("_scalePreSimulationDemand = %.2f%n",
			//					_scalePreSimulationDemand);
			int R = numberOfRetailers;			
			retailers = new Retailer[R];
			for (int r = 0; r < R; ++r) {
				double[] temp = demandMean[r];
				if (demandSeasonality != NONE) {
					temp = Vector.scaleSeasonality(temp, demandSeasonality);
				}

				RealVector v = new ArrayRealVector(temp); 
				
				// Scale the demand down by a certain factor
				v = v.mapMultiply(_scaleDemandMean);
				
				Demand mmfe = new MultMmfeDemand(v.toArray(),
						forecastVariance, forecastAccuracy, forecastLevel);
				retailers[r] = new Retailer(mmfe, scalePreSimulationDemand);
			}
		}

		private void buildWarehouse() {
			// Compute the total HC demand over a year
			double sum = 0;
			for (int r = 0; r < numberOfRetailers; ++r)
				for (int y = 0; y < numberOfPeriodsInYear; ++y)
					sum += demandMean[r][y];

			int Y = numberOfPeriodsInYear;
			int K = numberOfWarehouseShipmentsPerYear;
			int[] shipmentSchedule = new int[numberOfPeriodsInYear];

			for (int k = 0; k < K; ++k) {
				int t = (int) Math.round((double) k * Y / K); 
				shipmentSchedule[t] = (int) Math.round(
						supplyDemandRatio * sum / K);
			}

			warehouse = new NationalWarehouse(shipmentSchedule);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder()
			.append(String.format("# Scale demand mean = %.2f%n",
					_scaleDemandMean))
			.append(String.format("# Scale pre-simulation demand = %.2f%n",
					scalePreSimulationDemand))
			.append(String.format("# Number of warehouse shipments per year = %d%n",
					numberOfWarehouseShipmentsPerYear))
			.append(String.format("# Supply/demand ratio = %.2f%n",
					supplyDemandRatio));

			return sb.toString();
		}
		
		public static Simulator.Builder load(String accessibility, int delay) throws Exception {
			double supplyDemandRatio = 100;

			int forecastLevel = 0;

			double[] forecastVariance = CalibrateForecasts.getForecastVariance();
			double[] forecastAccuracy = {0, 0.5, 1};
			int shiftLeft = 0; // 0: means start simulation in January

			String inputFolder = "input/1-48-v3-" + accessibility;
			Simulator.Builder builder = SimulatorParser.readInputFiles(
					inputFolder,
					delay, supplyDemandRatio,
					forecastVariance, forecastAccuracy, forecastLevel,
					shiftLeft); 

			return builder;
		}
	}
	
}
