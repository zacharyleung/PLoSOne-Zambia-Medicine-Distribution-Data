package invsim3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import com.zacleung.invsim.policy.OptimizationPolicy;
import com.zacleung.invsim.policy.OrderUpToDistrictPolicy;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;

import d65helper.Helper;

public class Main {
	public static void main(String[] args) throws Exception {
		long startTime = System.nanoTime();

		//exp20120830();
		exp20120901();
		//exp20120902();
		//exp20120903();
		//exp20120904();
		//test();

		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime)/1e9 + " s");
	}

	public static void exp20120823() throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		double[] forecastVariance = {0.2, 0.1, 0.05, 0.025};
		double[] forecastAccuracy = {0, 0.5, 1};
		Policy[] policies = new Policy[3];
		int nYearsWarmup = 1;
		int nYears = 20;
		int randomSeed = 0;
		int delay = 0;
		double leadTimePercentile = 0.99;

		policies[0] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		policies[1] = new OrderUpToDistrictPolicy(12, 12, 8);
		policies[2] = new OptimizationPolicy(24, 20,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, leadTimePercentile);

		PrintStream out = new PrintStream(new FileOutputStream("exp20120823.txt"));

		out.println("# Number of years = " + nYears);

		out.println("SDR\tP0-0-SL\tP0-0-In\tP0-1-SL\tP0-1-In\tP0-2-SL\tP0-2-In\t");

		for (double sdr = 0.6; sdr < 1.6; sdr += 0.2) {
			out.format("%.2f\t", sdr);
			for (int fl = 2; fl < 3; ++fl) {
				for (int p = 0; p < policies.length; ++p) {
					System.out.println("Current heap free size = " +
							Runtime.getRuntime().freeMemory());

					Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
							forecastVariance, forecastAccuracy, fl);
					sim.simulate(policies[p], nYearsWarmup, nYears, randomSeed);
					Simulator.Stats stats = sim.getStats();
					out.format("%.4f\t%.1f\t", stats.serviceLevel,
							stats.inventoryInDemandPerPeriod);
					sim.getStats().print(System.out);
					//sim.print(System.out);
				}
			}
			out.println();
		}

		out.close();
	}


	public static void exp20120830() throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1-48-v3";

		int nYearsWarmup = 1;
		int nYears = 1;
		int randomSeed = 0;


		double[] forecastVariance = {0.2, 0.1, 0.05, 0.025};
		double[] forecastAccuracy = {0, 0.5, 1};
		Policy[] policies = new Policy[4];
		int delay = 0;
		int fl = 2;
		double leadTimePercentile = 0.99;

		policies[0] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		policies[1] = new OrderUpToDistrictPolicy(12, 12, 8);
		policies[2] = new OptimizationPolicy(32, 50,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, leadTimePercentile);
		policies[3] = new OptimizationPolicy(48, 50,
				OptimizationPolicy.TangentType.ACTUAL,
				30, OptimizationPolicy.LeadTimeType.ACTUAL, leadTimePercentile);


		PrintStream out = new PrintStream(new FileOutputStream("exp20120830.txt"));

		long startTime = System.nanoTime();


		out.println("# Number of years = " + nYears);
		out.print("# Forecast variance = ");
		for (int i = 0; i < forecastVariance.length; ++i)
			out.format("\t%.3f", forecastVariance[i]);
		out.println();
		out.print("# Forecast accuracy = ");
		for (int i = 0; i < forecastAccuracy.length; ++i)
			out.format("\t%.3f", forecastAccuracy[i]);
		out.println();
		out.println("# Forecast level = " + fl);
		out.println("# Delay = " + delay);
		out.println("# ");
		for (int p = 0; p < policies.length; ++p) {
			out.println("# Policy " + p);
			policies[p].print(out);
			out.println("# ");
		}

		out.print("SDR");
		for (int p = 0; p < policies.length; ++p)
			out.format("\tSL%d\tInv%d", p, p);
		out.println();

		for (double sdr = 1.5; sdr < 1.6; sdr += 0.2) {
			out.format("%.2f\t", sdr);

			//for (int p = 0; p < 1; ++p) { //policies.length; ++p) {
			for (int p = 3; p < policies.length; ++p) {
				System.out.format("Current heap size = %.2e",
						((double) Runtime.getRuntime().totalMemory()));
				System.out.println();
				System.out.format("Current heap free size = %.2e",
						((double) Runtime.getRuntime().freeMemory()));
				System.out.println();

				Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
						forecastVariance, forecastAccuracy, fl);
				sim.simulate(policies[p], nYearsWarmup, nYears, randomSeed);
				Simulator.Stats stats = sim.getStats();
				out.format("%.4f\t%.1f\t", stats.serviceLevel,
						stats.inventoryInDemandPerPeriod);
				sim.getStats().print(System.out);
				PrintStream perfect = new PrintStream(new FileOutputStream("perfect.txt"));
				sim.print(perfect);
				perfect.close();
			}
			out.println();
		}

		long endTime = System.nanoTime();

		out.format("# Time taken = %d s", (int) ((endTime - startTime)/1e9));
		out.println();
		out.close();
	}

	public static void exp20120901() throws Exception {
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\4-48-v3";
		//String folder = "/usr3/zacleung/zambia-java/input/212-48-v3";

		int nPolicies = 7;

		int nYearsWarmup = 2;
		int nYears = 3;
		int nReps = 2;
		int randomSeed;

		double[] forecastVariance = {
				0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
				0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
				0.0425, 0.045, 0.0475, 0.065};
		double[] forecastAccuracy = {0, 0.5, 1};
		Policy[] policies = new Policy[8];
		int delay = 0;
		int fl = 2;
		double leadTimePercentile = 0.99;

		policies[0] = new OrderUpToDistrictPolicy(12, 12, 8);
		policies[1] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		policies[2] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PROPORTIONAL);
		policies[3] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.FUTURE_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		policies[4] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.FUTURE_MONTHS,
				12, 16, Policy.AllocType.PROPORTIONAL);
		policies[5] = new OptimizationPolicy(36, 30,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, leadTimePercentile);
		policies[6] = new OptimizationPolicy(48, 50,
				OptimizationPolicy.TangentType.ACTUAL,
				30, OptimizationPolicy.LeadTimeType.ACTUAL, leadTimePercentile);
		//		policies[7] = new OptimizationPolicy(36, 50,
		//				OptimizationPolicy.TangentType.MEAN_DEMAND,
		//				30, OptimizationPolicy.LeadTimeType.MEAN_LEAD_TIME, leadTimePercentile);


		PrintStream out = new PrintStream(new FileOutputStream("exp20120901.txt"));

		long startTime = System.nanoTime();


		out.println("# Number of years = " + nYears);
		out.println("# Number of replications = " + nReps);
		out.print("# Forecast variance = ");
		for (int i = 0; i < forecastVariance.length; ++i)
			out.format("\t%.3f", forecastVariance[i]);
		out.println();
		out.print("# Forecast accuracy = ");
		for (int i = 0; i < forecastAccuracy.length; ++i)
			out.format("\t%.3f", forecastAccuracy[i]);
		out.println();
		out.println("# Forecast level = " + fl);
		out.println("# Delay = " + delay);
		out.println("# ");
		for (int p = 0; p < nPolicies; ++p) {
			out.println("# Policy " + p);
			policies[p].print(out);
			out.println("# ");
		}

		out.print("SDR");
		for (int p = 0; p < nPolicies; ++p)
			out.format("\tSL%d\tSL%de\tSL%dsd\tInv%d", p, p, p, p);
		out.println();

		for (double sdr = 0.6; sdr < 1.6; sdr += 0.1) {
			out.format("%.2f\t", sdr);

			for (int p = 0; p < nPolicies; ++p) { //policies.length; ++p) {
				//for (int p = 0; p < policies.length; ++p) {
				SummaryStatistics slStats = new SummaryStatistics();
				SummaryStatistics invStats = new SummaryStatistics();
				// Service level standard deviation summary statistics object
				SummaryStatistics slsdStats = new SummaryStatistics();
				for (int r = 0; r < nReps; ++r) {
					randomSeed = r;
					System.out.format("Current heap size = %.2e",
							((double) Runtime.getRuntime().totalMemory()));
					System.out.println();
					System.out.format("Current heap free size = %.2e",
							((double) Runtime.getRuntime().freeMemory()));
					System.out.println();
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					Date date = new Date();
					System.out.println("Current time = " + dateFormat.format(date));

					Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
							forecastVariance, forecastAccuracy, fl);
					sim.simulate(policies[p], nYearsWarmup, nYears, randomSeed);
					Simulator.Stats stats = sim.getStats();
					stats.print(System.out);
					slStats.addValue(stats.serviceLevel);
					invStats.addValue(stats.inventoryInDemandPerPeriod);
					slsdStats.addValue(stats.serviceLevelStdDev);
				}
				double width = Helper.getConfidenceIntervalWidth(slStats, 0.05);
				out.format("%.4f\t%.4f\t%.4f\t%.1f\t",
						slStats.getMean(), width, slsdStats.getMean(), 
						invStats.getMean());

			}
			out.println();
		}

		long endTime = System.nanoTime();

		out.format("# Time taken = %d s", (int) ((endTime - startTime)/1e9));
		out.println();
		out.close();
	}

	/**
	 * What are the right parameters (unmet demand penalty cost and
	 * lead time percentile) for the optimization policy?
	 * @throws Exception
	 */
	public static void exp20120902() throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1-48-v3";

		//		double[] udpArray = {5, 10, 15, 20, 25, 30, 35, 40, 50};
		//		double[] ltpArray = {0.5, 0.8, 0.9, 0.95, 0.98, 0.99};

		double[] udpArray = {30, 40, 50};
		double[] ltpArray = {0.99};

		int nYearsWarmup = 2;
		int nYears = 3;
		int randomSeed = 0;
		int T = 24;

		double[] forecastVariance = {
				0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
				0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
				0.0425, 0.045, 0.0475, 0.065};
		double[] forecastAccuracy = {0, 0.5, 1};
		int delay = 0;
		double sdr = 100;

		PrintStream out = new PrintStream(new FileOutputStream("exp20120902.txt"));

		long startTime = System.nanoTime();


		out.println("# Number of warmup years = " + nYearsWarmup);
		out.println("# Number of years = " + nYears);
		out.println("# Optimization horizon = " + T);
		out.print("# Forecast variance = ");
		for (int i = 0; i < forecastVariance.length; ++i)
			out.format("\t%.3f", forecastVariance[i]);
		out.println();
		out.print("# Forecast accuracy = ");
		for (int i = 0; i < forecastAccuracy.length; ++i)
			out.format("\t%.3f", forecastAccuracy[i]);
		out.println();
		out.println("# Delay = " + delay);

		for (int i = 0; i < udpArray.length; ++i) {
			//for (int i = 0; i < 1; ++i) {
			for (int j = 0; j < ltpArray.length; ++j) {
				//for (int j = 0; j < 1; ++j) {
				for (int k = 2; k < 3; ++k) {
					Policy policy = new OptimizationPolicy(T, udpArray[i],
							OptimizationPolicy.TangentType.MULTI_PERIOD,
							30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltpArray[j]);

					System.out.format("Current heap size = %.2e",
							((double) Runtime.getRuntime().totalMemory()));
					System.out.println();
					System.out.format("Current heap free size = %.2e",
							((double) Runtime.getRuntime().freeMemory()));
					System.out.println();

					int fl = k;
					Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
							forecastVariance, forecastAccuracy, fl);

					sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

					Simulator.Stats stats = sim.getStats();
					stats.print(System.out);
					out.format("UDP = %.1f\tLTP = %.3f\tFL = %d\tSL = %.4f\tInv = %.1f",
							udpArray[i], ltpArray[j], fl, stats.serviceLevel,
							stats.inventoryInDemandPerPeriod);
					out.println();

					//					PrintStream debug = new PrintStream(new FileOutputStream("debug.txt"));
					//					sim.print(debug);
					//					debug.close();

				}
			}
		}

		long endTime = System.nanoTime();

		out.format("# Time taken = %d s", (int) ((endTime - startTime)/1e9));
		out.println();
		out.close();
	}


	/**
	 * Can we plot the efficient frontier for the various policies?
	 * @throws Exception
	 */
	public static void exp20120903() throws Exception {
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1-48-v3";
		String folder = "/usr3/zacleung/zambia-java/input/212-48-v3";

		int nYearsWarmup = 2;
		int nYears = 3;
		int randomSeed = 0;

		double[] forecastVariance = {
				0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
				0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
				0.0425, 0.045, 0.0475, 0.065};
		double[] forecastAccuracy = {0, 0.5, 1};
		int delay = 0;
		double sdr = 100;
		int fl = 0;

		double[] ltpArray = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.99};

		Policy policy;
		PrintStream out;



		//		out = new PrintStream(new FileOutputStream("exp20120903-00.txt"));
		//		out.println("OUT\tSL\tInv");
		//		delay = 1;
		//		for (int i = 1; i < 30; ++i) {
		//			policy = new OrderUpToDistrictPolicy(12, 1.5 * i, i);
		//			if (i == 1) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%d\t%.4f\t%.1f", i, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//
		//		out = new PrintStream(new FileOutputStream("exp20120903-01.txt"));
		//		out.println("OUT\tSL\tInv");
		//		delay = 1;
		//		for (int i = 1; i < 30; ++i) {
		//			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.OrderType.PAST_DEMAND,
		//					12, i, Policy.AllocType.PRIORITY);
		//			if (i == 1) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%d\t%.4f\t%.1f", i, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 0;
		//		delay = 0;
		//		out = new PrintStream(new FileOutputStream("exp20120903-02.txt"));
		//		out.println("OUT\tSL\tInv");
		//		for (int i = 1; i < 30; ++i) {
		//			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.OrderType.FORECASTS,
		//					12, i, Policy.AllocType.PROPORTIONAL);
		//			if (i == 1) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%d\t%.4f\t%.1f", i, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 1;
		//		out = new PrintStream(new FileOutputStream("exp20120903-03.txt"));
		//		out.println("OUT\tSL\tInv");
		//		for (int i = 1; i < 30; ++i) {
		//			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.OrderType.FORECASTS,
		//					12, i, Policy.AllocType.PROPORTIONAL);
		//			if (i == 1) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%d\t%.4f\t%.1f", i, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		fl = 2;
		//		out = new PrintStream(new FileOutputStream("exp20120903-04.txt"));
		//		out.println("OUT\tSL\tInv");
		//		for (int i = 1; i < 30; ++i) {
		//			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.OrderType.FORECASTS,
		//					12, i, Policy.AllocType.PROPORTIONAL);
		//			if (i == 1) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%d\t%.4f\t%.1f", i, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 0;
		//		out = new PrintStream(new FileOutputStream("exp20120903-05.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 1;
		//		out = new PrintStream(new FileOutputStream("exp20120903-06.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//
		//		
		//		
		//		fl = 2;
		//		out = new PrintStream(new FileOutputStream("exp20120903-07.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		
		//		fl = 0;
		//		out = new PrintStream(new FileOutputStream("exp20120903-08.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 30,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 1;
		//		out = new PrintStream(new FileOutputStream("exp20120903-09.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 30,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//
		//		
		//		
		//		fl = 2;
		//		out = new PrintStream(new FileOutputStream("exp20120903-10.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 30,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		
		//		fl = 0;
		//		out = new PrintStream(new FileOutputStream("exp20120903-11.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//		
		//		
		//		
		//		fl = 1;
		//		out = new PrintStream(new FileOutputStream("exp20120903-12.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(36, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();
		//
		//
		//		
		//		
		//		fl = 2;
		//		out = new PrintStream(new FileOutputStream("exp20120903-13.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = 1.0 - ltpArray[i];
		//			policy = new OptimizationPolicy(36, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = readInputFiles(folder, delay, sdr,
		//					forecastVariance, forecastAccuracy, fl);
		//
		//			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);
		//
		//			Simulator.Stats stats = sim.getStats();
		//			out.format("%.4f\t%.4f\t%.1f", ltp, stats.serviceLevel,
		//					stats.inventoryInDemandPerPeriod);
		//			out.println();
		//		}
		//		out.close();

		fl = 2;
		out = new PrintStream(new FileOutputStream("exp20120903-14.txt"));
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= 50; ++i) {
			double ltp = 0.99;
			policy = new OptimizationPolicy(36, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, 0.99);
			if (i == 0) {
				policy.print(out);
			}

			Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
					forecastVariance, forecastAccuracy, fl);

			sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

			Simulator.Stats stats = sim.getStats();
			out.format("%.1f\t%.4f\t%.1f", i, stats.serviceLevel,
					stats.inventoryInDemandPerPeriod);
			out.println();
		}
		out.close();


	}


	/**
	 * Test that the optimization policy works under various parameters.
	 */
	public static void test() throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1-48-v3";

		double[] forecastVariance = {0.8, 0.5, 0.2, 0.1, 0.05, 0.025};
		double[] forecastAccuracy = {0, 0.5, 1};
		int delay = 0;
		double sdr = 1.4;
		int fl = 2;

		int nYearsWarmup = 1;
		int nYears = 10;
		int randomSeed = 0;
		double leadTimePercentile = 0.99;

		Policy policy;
		policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		policy = new OrderUpToDistrictPolicy(12, 12, 8);
		policy = new OptimizationPolicy(32, 100,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, leadTimePercentile);
		policy = new OptimizationPolicy(48, 50,
				OptimizationPolicy.TangentType.ACTUAL,
				30, OptimizationPolicy.LeadTimeType.ACTUAL, leadTimePercentile);

		Simulator sim = SimulatorParser.readInputFiles(folder, delay, sdr,
				forecastVariance, forecastAccuracy, fl);

		sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

		sim.getStats().print(System.out);

	}
}
