package com.zacleung.invsim.main;

import invsim3.GeometricLeadTime;
import invsim3.Simulator;
import invsim3.Simulator.Builder;
import invsim3.Simulator.Parameters;
import invsim3.Simulator.Stats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.common.base.Stopwatch;
import com.zacleung.invsim.policy.DemandEstimation;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;

/**
 * You can either run this code in Eclipse, or from the command line. 
 * <code>
 * java Exp02 number-of-replications [all or 1-4]
 * </code>
 * 
 * Running time for 100 replications = 30 seconds.
 * Running time for 10^5 replications = 8 hours (estimated)
 * 
 * @author znhleung
 *
 */
public class Exp02 {
	private static enum PolicyTest {ESTIMATION, ORDER};
	
	private static int numberOfWarmupYears = 3;
	private static int numberOfSimulationYears = 1;
	private static int numberOfReplications = 10;
	private static File outFolder = new File("output/exp02/");
	private static int delay = 0;
	private static String accessibility = "all";

	/** The lead time values to try for varyLeadTimes() */
	private static double[] meanSecondaryLeadTime = {2, 2.5, 3, 3.5, 4, 4.5, 5};
	/** The seasonality values to try for varySeasonality() */
	private static double[] seasonality = {1, 1.4, 1.8, 2.2, 2.6, 3.0, 3.4};

	public static void main(String[] args) throws Exception {
		Stopwatch stopwatch = new Stopwatch().start();

		System.out.println("java Exp02 number-of-replications [all or 1-4]");

		String theCase;
		if (args.length == 0) {
			theCase = "all"; 
		} else {
			theCase = args[1];
		}
		
		if (args.length > 0) {
			numberOfReplications = Integer.parseInt(args[0]);
		}
		
		System.out.printf("Number of replications = %d%n", numberOfReplications);
		System.out.printf("Case = %s%n", theCase);
		
		switch(theCase) {
		case "all":
			varySeasonality(PolicyTest.ESTIMATION);
			varySeasonality(PolicyTest.ORDER);
			varyLeadTimes(PolicyTest.ESTIMATION);
			varyLeadTimes(PolicyTest.ORDER);
			break;
			
		case "1":
			varySeasonality(PolicyTest.ESTIMATION);
			break;
			
		case "2":
			varySeasonality(PolicyTest.ORDER);
			break;
			
		case "3":
			varyLeadTimes(PolicyTest.ESTIMATION);
			break;
			
		case "4":
			varyLeadTimes(PolicyTest.ORDER);
			break;
		}

		System.out.println("That took: " + stopwatch);
	}


	/**
	 * 
	 * @param policiesToTest Takes values "estimation" or "order"
	 */
	private static Policy[] loadPolicies(PolicyTest policyTest) {
		Policy[] policies = new Policy[0];
		switch(policyTest) {

		case ORDER:
			DemandEstimation est = new DemandEstimation(
					DemandEstimation.Type.PAST_CONSUMPTION, 12);
			policies = new Policy[]{
					new OrderUpToXDockPolicy(est, 8, Policy.AllocType.PRIORITY),
					new OrderUpToXDockPolicy(est, 12, Policy.AllocType.PRIORITY),
					new OrderUpToXDockPolicy(est, 16, Policy.AllocType.PRIORITY),
					new OrderUpToXDockPolicy(est, 20, Policy.AllocType.PRIORITY),
					new OrderUpToXDockPolicy(est, 24, Policy.AllocType.PRIORITY)
			};
			break;

		case ESTIMATION:	
			DemandEstimation[] ests = {
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

			policies = new Policy[ests.length];
			for (int i = 0; i < ests.length; ++i) {
				policies[i] = new OrderUpToXDockPolicy(ests[i],
						16, Policy.AllocType.PRIORITY);
			};
			break;

		}

		return policies;
	}

	private static void varyLeadTimes(PolicyTest policyTest) throws Exception {
		System.out.printf("Running varyLeadTimes(%s)...\n", policyTest.toString());
		
		Policy[] policies = loadPolicies(policyTest);

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.withNumberOfWarmupYears(numberOfWarmupYears)
		.withNumberOfSimulationYears(numberOfSimulationYears);

		Simulator.Builder builder = Simulator.Builder.load(accessibility, delay);

		String outputFile = String.format("java-leadtime-%s-%s.txt", 
				policyTest.toString().toLowerCase(), accessibility);
		PrintStream out = new PrintStream(
				new FileOutputStream(
						new File(outFolder, outputFile)));

		// Print the policies
		for (int i = 0; i < policies.length; ++i) {
			out.println("# Policy " + i);
			out.print(policies[i]);
			out.println("# ");
		}

		printHeader(out);

		for (int i = 0; i < meanSecondaryLeadTime.length; ++i) {
			double[] temp = {meanSecondaryLeadTime[i]};
			GeometricLeadTime.Builder leadTimeBuilder = 
					builder.getLeadTimeBuilder();
			builder.withLeadTimeBuilder(
					leadTimeBuilder.meanSecondaryLeadTime(temp));

			for (int j = 0; j < policies.length; ++j) {
				parameters.withPolicy(policies[j]);

				for (int iRep = 0; iRep < numberOfReplications; ++iRep) {
					Simulator simulator = builder.build();				
					parameters.withRandomSeed(iRep);
					simulator.simulate(parameters);
					printLine(out, builder, simulator, j, iRep);
				} // for iRep
			} // for each policy j
		} // for each mean secondary lead time value
		out.close();
	}


	private static void varySeasonality(PolicyTest policyTest) throws Exception {
		System.out.printf("Running varySeasonality(%s)...\n", policyTest.toString());
		
		Policy[] policies = loadPolicies(policyTest);

		// Simulation parameters
		Simulator.Parameters parameters =
				new Simulator.Parameters()
		.withNumberOfWarmupYears(numberOfWarmupYears)
		.withNumberOfSimulationYears(numberOfSimulationYears);

		Simulator.Builder builder = Simulator.Builder.load(accessibility, delay);

		String outputFile = String.format("java-seasonality-%s-%s.txt", 
				policyTest.toString().toLowerCase(), accessibility);
		PrintStream out = new PrintStream(
				new FileOutputStream(
						new File(outFolder, outputFile)));

		// Print the policies
		for (int i = 0; i < policies.length; ++i) {
			out.println("# Policy " + i);
			out.print(policies[i]);
			out.println("# ");
		}

		printHeader(out);

		for (int i = 0; i < seasonality.length; ++i) {
			builder.withDemandSeasonality(seasonality[i]);

			for (int j = 0; j < policies.length; ++j) {
				parameters.withPolicy(policies[j]);

				for (int iRep = 0; iRep < numberOfReplications; ++iRep) {
					Simulator simulator = builder.build();				
					parameters.withRandomSeed(iRep);
					simulator.simulate(parameters);
					printLine(out, builder, simulator, j, iRep);
				} // for iRep
			} // for each policy j
		} // for each seasonality value
		out.close();
	}

	private static void printHeader(PrintStream out) {
		out.println("LeadTime\tSeasonality\tPolicy\tReplication\tService\tInventory\tMaxInventory");		
	}

	private static void printLine(PrintStream out, Simulator.Builder builder,
			Simulator simulator, int iPolicy, int iRep) {
		Simulator.Stats stats = simulator.getStats();
		out.printf("%.2f\t%.2f\t%d\t%d\t%.4f\t%.2f\t%.2f%n",
				builder.getLeadTimeBuilder().getMeanSecondaryLeadTime()[0],
				builder.getDemandSeasonality(),
				iPolicy,
				iRep,
				stats.serviceLevel,
				stats.inventoryInDemandPerPeriod,
				stats.maxInventoryInDemandPerPeriod);
	}

}
