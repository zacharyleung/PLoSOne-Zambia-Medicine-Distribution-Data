package invsim3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.zacleung.invsim.policy.OptimizationPolicy;
import com.zacleung.invsim.policy.OrderUpToDistrictPolicy;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;

public class MainFrontier {
	private static double[] forecastVariance;
	private static double[] forecastAccuracy = {0, 0.5, 1};
	private static double sdr;
	private static double[] ltpArray = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6,
		0.7, 0.8, 0.9, 0.95, 0.98, 0.99};  

	private static int nYears;
	private static int nYearsWarmup;
	private static int fl;
	private static int delay;
	private static int nReps;

	private static File inFolder;

	public static void main(String[] args) throws Exception {
		// For ORC computers: 212 HC input
		//inFolder = "/usr3/zacleung/zambia-java-1/input/212-48-v3";

		// For T60p: 212 HC input
		//inFolder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";

		// For T60p: 4 HC input
		//inFolder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\4-48-v3";

		inFolder = new File("input", "1-48-v3-cutoff");

		nYearsWarmup = 2;
		nYears = 3;
		nReps = 2;

		forecastVariance = CalibrateForecasts.getForecastVariance();
		delay = 0;
		sdr = 1.2;
		fl = 2;

		doFrontiers(1.2);
		doFrontiers(1.0);
		doFrontiers(0.8);
	}

	private static void doFrontiers(double mySdr) throws Exception {
		sdr = mySdr;

		File outFolder = new File("frontier", Double.toString(sdr));
		File outFile;

		// Parameters for the optimization policies
		int T = 48;
		int hcInc = 1;
		int hcMax = 20;

		long startTime;
		long endTime;

		Policy policy;
		PrintStream out;


		// District policy
		startTime = System.nanoTime();
		outFile = new File(outFolder, "district.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("OUT\tSL\tInv");
		for (int i = 1; i < 30; ++i) {
			policy = new OrderUpToDistrictPolicy(12, 1.5 * i, i);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();



		// Cross-docking past policy
		startTime = System.nanoTime();
		outFile = new File(outFolder, "xdock-past.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		out.println("# SDR = " + sdr);
		out.println("OUT\tSL\tInv");
		for (int i = 1; i < 30; ++i) {
			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
					12, i, Policy.AllocType.PRIORITY);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		out.close();




		// Cross-docking past policy
		startTime = System.nanoTime();
		outFile = new File(outFolder, "xdock-fc.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("OUT\tSL\tInv");
		for (int i = 1; i < 30; ++i) {
			policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.FUTURE_MONTHS,
					12, i, Policy.AllocType.PRIORITY);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		out.close();



		// Optimization policy, vary lead time percentile
		outFile = new File(outFolder, "opt-ltp.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		out.println("LTP\tSL\tInv");
		for (int i = 0; i < ltpArray.length; ++i) {
			double ltp = ltpArray[i];
			policy = new OptimizationPolicy(T, 30,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			if (i == 0) {
				policy.print(out);
			}
			out.format("%.3f", ltp);
			mySimPolicy(out, policy);
		}
		out.close();


		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt-cons-990.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; i += hcInc) {
			double ltp = 0.99;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();




		startTime = System.nanoTime();		
		outFile = new File(outFolder, "clair.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; i += hcInc) {
			double ltp = 0.995;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.ACTUAL,
					30, OptimizationPolicy.LeadTimeType.ACTUAL, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();



		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt-cons-950.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; i += hcInc) {
			double ltp = 0.95;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();




		// Optimization policy with mean lead time
		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt-mean.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; i += hcInc) {
			double ltp = 0.5;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.MEAN_LEAD_TIME, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();



		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt-cons-500.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; i += hcInc) {
			double ltp = 0.5;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();


		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt-cons-750.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; ++i) {
			double ltp = 0.75;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();



		startTime = System.nanoTime();		
		outFile = new File(outFolder, "opt-cons-995.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		printParameters(out);
		out.println("UDP\tSL\tInv");
		for (int i = 1; i <= hcMax; ++i) {
			double ltp = 0.995;
			policy = new OptimizationPolicy(T, i,
					OptimizationPolicy.TangentType.MULTI_PERIOD,
					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
			if (i == 1) {
				policy.print(out);
			}
			out.print(i);
			mySimPolicy(out, policy);
		}
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();





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
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 20,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 30,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 30,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//
		//
		//
		//
		//		fl = 0;
		//		out = new PrintStream(new FileOutputStream("exp20120903-11.txt"));
		//		out.println("LTP\tSL\tInv");
		//		for (int i = 0; i < ltpArray.length; ++i) {
		//			double ltp = ltpArray[i];
		//			policy = new OptimizationPolicy(T, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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
		//			policy = new OptimizationPolicy(T, 40,
		//					OptimizationPolicy.TangentType.MULTI_PERIOD,
		//					30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, ltp);
		//			if (i == 0) {
		//				policy.print(out);
		//			}
		//
		//			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
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

	}

	private static void printParameters(PrintStream out) {
		out.println("# SDR = " + sdr);
		out.println("# delay = " + delay);
		out.println("# forecast level = " + fl);
		out.println("# Number of warmup years = " + nYearsWarmup);
		out.println("# Number of years = " + nYears);
		out.println("# Number of replications = " + nReps);
	}

	private static void mySimPolicy(PrintStream out, Policy policy)
			throws Exception {
		SummaryStatistics slStats = new SummaryStatistics();
		SummaryStatistics invStats = new SummaryStatistics();

		for (int r = 0; r < nReps; ++r) {
			Simulator sim = Main.readInputFiles(inFolder, delay, sdr,
					forecastVariance, forecastAccuracy, fl);
			sim.simulate(policy, nYearsWarmup, nYears, r);
			Simulator.Stats stats = sim.getStats();
			slStats.addValue(stats.serviceLevel);
			invStats.addValue(stats.inventoryInDemandPerPeriod);
		}
		out.format("\t%.4f\t%.2f", slStats.getMean(), invStats.getMean());
		out.println();
	}
}
