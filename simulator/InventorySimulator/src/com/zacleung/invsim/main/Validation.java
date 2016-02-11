package com.zacleung.invsim.main;

import invsim3.CalibrateForecasts;
import invsim3.GeometricLeadTime;
import invsim3.Simulator;
import invsim3.SimulatorParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.zacleung.invsim.policy.DemandEstimation;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;
import com.zacleung.invsim.view.Trace;
import com.zacleung.util.MyMath;

public class Validation {
	private static int numberOfReplications = 1;
	private static boolean shouldPrintTrace = true;


	public static void main(String[] args) throws Exception {
		//String[] access = {"full", "partial"};
		String[] access = {"all"};
		int[] packSize = {6, 12, 18, 24};
		double[] scalePreSimulationDemand = {0.8, 1.8, 1.5, 0.95};
		double[] scaleDemandMean = {0.48, 0.27, 0.30, 0.52};
		// 10 reps: 7 s
		// 50 reps: 18 s
		// 100 reps: 27 s
		// 200 reps: 49 s
		// 2500 reps: 410 s

		for (int i = 0; i < access.length; ++i) {
			//for (int j = 0; j < packSize.length; ++j) {
			for (int j = 0; j == 0; ++j) {
				doValidationReplications(access[i], packSize[j],
						scalePreSimulationDemand[j],
						scaleDemandMean[j], 6);
			}
		}


	}




	/**
	 * 
	 * @param numberOfReplications
	 * @param packSize
	 * @param accessibility Either "full" or "partial"
	 * @param scalePreSimulationDemand
	 */
	public static void doValidationReplications(
			String accessibility,
			int packSize,
			double scalePreSimulationDemand,
			double scaleDemandMean,
			int numberOfWarmupPeriods) 
					throws Exception {
		double supplyDemandRatio = 100;

		File outFolder = new File("output/validation");

		int delay = 1;
		int forecastLevel = 0;

		DemandEstimation demandEstimation = new DemandEstimation(
				DemandEstimation.Type.PAST_CONSUMPTION, 12);
		Policy policy = new OrderUpToXDockPolicy(demandEstimation,
				16, Policy.AllocType.PRIORITY);

		int nReps = numberOfReplications;

		double[] forecastVariance = CalibrateForecasts.getForecastVariance();
		double[] forecastAccuracy = {0, 0.5, 1};
		int shiftLeft = 16; // 16: means start simulation in May

		Simulator simulator;

		PrintStream outLog = new PrintStream(
				new FileOutputStream(
						new File(outFolder, "log.txt")));

		String outFileName = String.format("%s-%02d.txt", accessibility, packSize);
		PrintStream outStockOuts = new PrintStream(
				new FileOutputStream(
						new File(outFolder, outFileName)));

		Simulator.Builder builder = SimulatorParser.readInputFiles(
				"input/1-48-v3-" + accessibility,
				delay, supplyDemandRatio,
				forecastVariance, forecastAccuracy, forecastLevel,
				shiftLeft);



		// Simulation parameters
		// Implement number of warmup periods
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.withNumberOfWarmupPeriods(numberOfWarmupPeriods + 2 * delay)
		.withNumberOfSimulationYears(1)
		.withPolicy(policy);

		// Make first shipment happen at t = -numberOfWarmupPeriods
		// by setting
		// secondary cycle offset = numberOfWarmupPeriods % 4
		GeometricLeadTime.Builder gltBuilder =
				builder.getLeadTimeBuilder();
		int[] primaryCycleOffset =
				new int[]{MyMath.positiveModulo(-numberOfWarmupPeriods, 4)};
		gltBuilder.primaryCycleOffset(primaryCycleOffset);
		// put this GeometricLeadTime Builder into the Simulator Builder
		builder = builder.withLeadTimeBuilder(gltBuilder);
		System.out.println(MyMath.positiveModulo(-numberOfWarmupPeriods, 4));
		
		
		

		for (int i = 0; i < nReps; ++ i) {
			simulator = builder
					.withScalePreSimulationDemand(scalePreSimulationDemand)
					.withScaleDemandMean(scaleDemandMean)
					.build();

			// Print parameters only for first simulator
			if (i == 0) {
				outStockOuts.print(builder);
				outStockOuts.printf("# Delay = %d%n", delay);
				outStockOuts.printf("# Mean secondary lead time = %.2f%n", 
						((GeometricLeadTime) simulator.leadTime).getMeanSecondaryLeadTime()[0]);
				outStockOuts.printf("# Accessibility = %.2f %.2f %.2f...%n",
						simulator.leadTime.getAccessibility(0, 0),
						simulator.leadTime.getAccessibility(0, 1),
						simulator.leadTime.getAccessibility(0, 2));
				outStockOuts.print(parameters);
				outStockOuts.printf("# Number of replications = %d%n", nReps);
				outStockOuts.printf("# Circular shift left = %d%n", shiftLeft);
				outStockOuts.printf("# Scale pre-simulation demand factor = %.2f%n", 
						scalePreSimulationDemand);
				outStockOuts.printf("# Each row is from a replication of the simulation.%n");
				outStockOuts.printf("# Each column is from a period of the simulation.%n");
			}

			parameters.withRandomSeed(i);
			simulator.simulate(parameters);

			// print trace
			if (shouldPrintTrace) {
				File file = new File(outFolder, 
						String.format("trace-%d.csv", i));

				// open a new file, overwriting if necessary
				PrintStream outTrace = new PrintStream(file);
				outTrace.printf("# Delay = %d%n", delay);
				outTrace.printf("# Mean secondary lead time = %.2f%n", 
						((GeometricLeadTime) simulator.leadTime).getMeanSecondaryLeadTime()[0]);
				outTrace.printf("# Accessibility = %.2f %.2f %.2f...%n",
						simulator.leadTime.getAccessibility(0, 0),
						simulator.leadTime.getAccessibility(0, 1),
						simulator.leadTime.getAccessibility(0, 2));
				outTrace.print(parameters);
				outTrace.printf("# Number of replications = %d%n", nReps);
				outTrace.printf("# Circular shift left = %d%n", shiftLeft);
				outTrace.printf("# Scale pre-simulation demand factor = %.2f%n", 
						scalePreSimulationDemand);
				outTrace.close();
				Trace.trace(simulator, file, true);
			}


			double[] stockoutProbability = simulator.getStockOuts();
			outLog.printf("Replication % 4d%n", i);
			outLog.printf("Total memory used by JVM = %.0f MB%n", 
					Runtime.getRuntime().totalMemory() / 1e6);

			for (int j = 0; j < stockoutProbability.length; ++j) {
				if (j != 0) outStockOuts.print("\t");
				outStockOuts.printf("%.4f", stockoutProbability[j]);
			}
			outStockOuts.println();

		}

		outLog.close();
		outStockOuts.close();

		//		PrintStream outTrace = new PrintStream(
		//				new FileOutputStream(
		//						new File(outFolder, "trace.txt")));
		//		simulators[0].print(outTrace);
		//		outTrace.close();
	}

}
