package invsim3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.zacleung.invsim.policy.ConstantDistrictPolicy;
import com.zacleung.invsim.policy.ConstantXDockPolicy;
import com.zacleung.invsim.policy.DemandEstimation;
import com.zacleung.invsim.policy.OrderUpToDistrictPolicy;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;

public class SimulatorTest {


	public static void main(String[] args) throws Exception {
		//testConstant();
		testXDock();
	}


	/**
	 * Test how well the simulator works with constant demand,
	 * constant lead time, and a constant shipment policy.
	 */
	private static void testConstant() throws Exception {
		int Y = 5;

		int R = 2;
		Retailer[] retailers = new Retailer[R];
		for (int r = 0; r < R; ++r) {
			retailers[r] = new Retailer(new ConstantDemand(5 - r, Y));
		}

		int[] shipmentSchedule = {15, 15, 15, 15, 15};
		NationalWarehouse warehouse = new NationalWarehouse(shipmentSchedule);

		Policy policy;
		policy = new ConstantXDockPolicy(25);
		policy = new ConstantDistrictPolicy(50, 25);
		policy = new OrderUpToDistrictPolicy(10, 10, 10);
		//		policy = new OptimizationPolicy(40, 50, OptimizationPolicy.MULTI_PERIOD,
		//				4, OptimizationPolicy.CONSERVATIVE);

		int cycle = 5;
		int[] OP = {2};
		int[] OS = {0, 0};
		int delay = 0;
		int[] R2D = {0, 0};
		LeadTime leadTime;
		int[] primaryLeadTime = {2};
		double[] meanSecondaryLeadTime = {2, 3};
		double[][] accessibility = {{1, 1, 1, 1, 1}, {1, 1, 0.5, 0, 0}};
		leadTime = new ConstantLeadTime(cycle, OP, OS, delay, Y, R2D,
				1, 1);

		leadTime = new GeometricLeadTime(cycle, OP, OS, delay, Y, R2D,
				primaryLeadTime, meanSecondaryLeadTime, accessibility);

		Simulator sim = new Simulator(warehouse, retailers, leadTime);

		Simulator.Parameters parameters = new Simulator.Parameters()
		.numberOfWarmupYears(2)
		.numberOfSimulationYears(2)
		.policy(policy);

		sim.simulate(parameters);
		
		sim.print(System.out);

		sim.getStats().print(System.out);
	}

	
	
	private static void testXDock() throws Exception {
		double supplyDemandRatio = 100;

		File outFolder = new File("output");

		int delay = 1;
		int forecastLevel = 0;

		DemandEstimation demandEstimation = new DemandEstimation(
				DemandEstimation.Type.PAST_CONSUMPTION, 12);
		Policy policy = new OrderUpToXDockPolicy(demandEstimation,
				16, Policy.AllocType.PRIORITY);

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters().numberOfWarmupPeriods(12)
				.numberOfSimulationYears(1)
				.policy(policy);

		double[] forecastVariance = CalibrateForecasts.getForecastVariance();
		double[] forecastAccuracy = {0, 0.5, 1};
		int shiftLeft = 16; // 16: means start simulation in May

		Simulator.Builder builder = SimulatorParser.readInputFiles("input/1-48-v3-full",
				delay, supplyDemandRatio,
				forecastVariance, forecastAccuracy, forecastLevel,
				shiftLeft); 

		Simulator simulator = builder
				.withDemandSeasonality(5)
				.build();

		simulator.simulate(parameters);

		simulator.print(System.out);
	}
}
