package invsim3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.common.base.Stopwatch;
import com.zacleung.invsim.policy.DemandEstimation;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;
import com.zacleung.util.Matrix;
import com.zacleung.util.Vector;

import d65helper.Helper;


/**
 * Run the single-facility simulation model for the medical journal
 * paper.
 * 
 * @author zacleung
 *
 */
public class MedicalPaper {
	private static int numberOfWarmupYears = 3;
	private static int numberOfSimulationYears = 1;
	private static int numberOfReplications = 100000;
	private static File outFolder = new File("output");
	private static int delay = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//computeNormalizedDemand();
		//exp20130208();
		Stopwatch stopwatch = new Stopwatch().start();

		// 100 reps: 16 s
		// 10000 reps: 790 s
		// 1000000 reps: maybe 22 hours
		//doValidation();
		
		//varyLeadTimes();
		
		//varyOrders();

		// 3 warmup years, 1 simulation year
		// 100 reps: 17 s, 180 kb
		// 1000 reps: 130 s
		// 10000 reps: 1300 s, 18 mb
		//varyHistory();
		// Mac 1000 reps: 124 s
		
		System.out.println("That took: " + stopwatch);		

	}

	public static void computeNormalizedDemand() throws Exception {
		double[][] matrix = Matrix.readDoubleMatrix("input/daily-demand-normalized-4800-periods-365.txt",
				1, 365);
		double[] oldVector = matrix[0];
		double[] newVector = Vector.scaleVector(oldVector, 48, true);
		double sum = 0;
		for (int i = 0; i < newVector.length; ++i) {
			sum += newVector[i];
		}
		System.out.println("Sum of newVector = " + sum);
		matrix[0] = newVector;
		Matrix.writeDoubleMatrix("input/daily-demand-normalized-4800-periods-48.txt",
				matrix);
	}

	public static void doValidation() throws Exception {
	}


	public static void varyLeadTimes() throws Exception {
		double[] meanSecondaryLeadTime = {2, 3, 4, 5, 6, 7, 8};

		DemandEstimation demandEstimation = new DemandEstimation(
				DemandEstimation.Type.PAST_CONSUMPTION, 12);
		Policy policy = new OrderUpToXDockPolicy(demandEstimation,
				16, Policy.AllocType.PRIORITY);

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.numberOfWarmupYears(numberOfWarmupYears)
		.numberOfSimulationYears(numberOfSimulationYears)
		.policy(policy);


		for (String accessibility : new String[]{"full", "partial"}) {
			Simulator.Builder builder = Simulator.Builder.load(accessibility, delay);

			String outputFile = String.format("lead-times-%s.txt", accessibility);
			PrintStream outService = new PrintStream(
					new FileOutputStream(
							new File(outFolder, outputFile)));

			// For each mean secondary lead time value
			for (int i = 0; i < meanSecondaryLeadTime.length; ++i) {
				double[] temp = {meanSecondaryLeadTime[i]};
				builder.leadTimeBuilder.meanSecondaryLeadTime(temp);

				for (int iRep = 0; iRep < numberOfReplications; ++iRep) {
					Simulator simulator = builder.build();				
					parameters.randomSeed(iRep);
					simulator.simulate(parameters);

					// Print parameters and header row only once
					if (i == 0 && iRep == 0) {
						outService.printf("# Delay = %d%n", delay);
						outService.printf("# Mean secondary lead time = %.2f%n", 
								((GeometricLeadTime) simulator.leadTime).getMeanSecondaryLeadTime()[0]);
						outService.printf("# Accessibility = %.2f %.2f %.2f...%n",
								simulator.leadTime.getAccessibility(0, 0),
								simulator.leadTime.getAccessibility(0, 1),
								simulator.leadTime.getAccessibility(0, 2));
						outService.print(parameters);
						// Header row
						outService.println("LeadTime\tService\tInventory");
					}

					Simulator.Stats stats = simulator.getStats();
					outService.printf("%.2f\t%.4f\t%.2f%n", 
							meanSecondaryLeadTime[i],
							stats.serviceLevel,
							stats.inventoryInDemandPerPeriod);
				} // for iRep

			} // for i
			outService.close();
		} // for (String accessibility :
	}


	public static void varyHistory() throws Exception {
		double[] meanSecondaryLeadTime = {2, 3, 4, 5, 6, 7, 8};

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.numberOfWarmupYears(numberOfWarmupYears)
		.numberOfSimulationYears(numberOfSimulationYears);

		DemandEstimation[] demandEstimation = {
				new DemandEstimation(DemandEstimation.Type.PAST_CONSUMPTION, 4),
				new DemandEstimation(DemandEstimation.Type.PAST_CONSUMPTION, 12),
				new DemandEstimation(DemandEstimation.Type.PAST_CONSUMPTION, 24),
				new DemandEstimation(DemandEstimation.Type.PAST_CONSUMPTION, 48),
				new DemandEstimation(DemandEstimation.Type.LAST_YEAR_CONSUMPTION, 12),
				new DemandEstimation(DemandEstimation.Type.PAST_DEMAND, 4),
				new DemandEstimation(DemandEstimation.Type.PAST_DEMAND, 12),
				new DemandEstimation(DemandEstimation.Type.PAST_DEMAND, 24),
				new DemandEstimation(DemandEstimation.Type.PAST_DEMAND, 48),
				new DemandEstimation(DemandEstimation.Type.LAST_YEAR_DEMAND, 12)
		};
				
		Policy[] policies = new Policy[demandEstimation.length];
		for (int i = 0; i < demandEstimation.length; ++i) {
			policies[i] = new OrderUpToXDockPolicy(demandEstimation[i],
					16, Policy.AllocType.PRIORITY);
		};

		for (String accessibility : new String[]{"all"}) {
			Simulator.Builder builder = Simulator.Builder.load(accessibility, delay);

			String outputFile = String.format("history-%s-%d.txt",
					accessibility,
					numberOfReplications);
			PrintStream out = new PrintStream(
					new FileOutputStream(
							new File(outFolder, outputFile)));

			// Print the policies
			for (int i = 0; i < policies.length; ++i) {
				out.println("# Policy " + i);
				out.print(policies[i]);
				out.println("# ");
			}

			// Header row
			out.println("LeadTime\tPolicy\tService\tInventory\tMaxInventory");

			for (int i = 0; i < meanSecondaryLeadTime.length; ++i) {
				double[] temp = {meanSecondaryLeadTime[i]};
				builder.leadTimeBuilder.meanSecondaryLeadTime(temp);

				// For each mean secondary lead time value
				for (int j = 0; j < policies.length; ++j) {
					parameters.policy(policies[j]);

					for (int iRep = 0; iRep < numberOfReplications; ++iRep) {
						System.out.printf("i = %d, j = %d, replication = %d\n", i, j, iRep);
						Simulator simulator = builder.build();				
						parameters.randomSeed(iRep);
						simulator.simulate(parameters);

						Simulator.Stats stats = simulator.getStats();
						out.printf("%.2f\t%d\t%.4f\t%.2f\t%.2f%n",
								meanSecondaryLeadTime[i],
								j,
								stats.serviceLevel,
								stats.inventoryInDemandPerPeriod,
								stats.maxInventoryInDemandPerPeriod);
					} // for iRep
				} // for j = 1
			} // for i = 
			out.close();
		} // for (String accessibility :
	}


	
	
	public static void varyOrders() throws Exception {
		double[] meanSecondaryLeadTime = {2, 3, 4, 5, 6, 7, 8};

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.numberOfWarmupYears(numberOfWarmupYears)
		.numberOfSimulationYears(numberOfSimulationYears);


		DemandEstimation demandEstimation = new DemandEstimation(
				DemandEstimation.Type.PAST_CONSUMPTION, 12);
		Policy[] policies = {
				new OrderUpToXDockPolicy(demandEstimation, 8, Policy.AllocType.PRIORITY),
				new OrderUpToXDockPolicy(demandEstimation, 12, Policy.AllocType.PRIORITY),
				new OrderUpToXDockPolicy(demandEstimation, 16, Policy.AllocType.PRIORITY),
				new OrderUpToXDockPolicy(demandEstimation, 20, Policy.AllocType.PRIORITY),
				new OrderUpToXDockPolicy(demandEstimation, 24, Policy.AllocType.PRIORITY)
		};

		for (String accessibility : new String[]{"full", "partial"}) {
			Simulator.Builder builder = Simulator.Builder.load(accessibility, delay);

			String outputFile = String.format("order-%s.txt", accessibility);
			PrintStream out = new PrintStream(
					new FileOutputStream(
							new File(outFolder, outputFile)));

			// Print the policies
			for (int i = 0; i < policies.length; ++i) {
				out.println("# Policy " + i);
				out.print(policies[i]);
				out.println("# ");
			}

			// Header row
			out.println("LeadTime\tPolicy\tService\tInventory");

			for (int i = 0; i < meanSecondaryLeadTime.length; ++i) {
				double[] temp = {meanSecondaryLeadTime[i]};
				builder.leadTimeBuilder.meanSecondaryLeadTime(temp);

				// For each mean secondary lead time value
				for (int j = 0; j < policies.length; ++j) {
					parameters.policy(policies[j]);

					for (int iRep = 0; iRep < numberOfReplications; ++iRep) {
						Simulator simulator = builder.build();				
						parameters.randomSeed(iRep);
						simulator.simulate(parameters);

						Simulator.Stats stats = simulator.getStats();
						out.printf("%.2f\t%d\t%.4f\t%.2f%n",
								meanSecondaryLeadTime[i],
								j,
								stats.serviceLevel,
								stats.inventoryInDemandPerPeriod);
					} // for iRep
				} // for j = 1
			} // for i = 
			out.close();
		} // for (String accessibility :
	}

}
