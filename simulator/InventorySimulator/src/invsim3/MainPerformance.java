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

public class MainPerformance {
	public static void main(String[] args) throws Exception {
		String inFolder;

		// For ORC computers: 212 HC input
		inFolder = "/usr3/zacleung/zambia-java-1/input/212-48-v3";

		// For T60p: 212 HC input
		//inFolder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";

		// For T60p: 4 HC input
		//inFolder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\4-48-v3";

		File outFolder = new File("perform");
		File outFile;
		PrintStream out;

		int delay = 1;
		int fl = 0;
		double sdrTic = 0.1;

		int nPolicies = 7;

		int nYearsWarmup = 2;
		int nYears = 3;
		int nReps = 5;
		int randomSeed;

		double[] forecastVariance = CalibrateForecasts.getForecastVariance();
		double[] forecastAccuracy = {0, 0.5, 1};
		Policy[] policies = new Policy[8];
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
		policies[5] = new OptimizationPolicy(48, 40,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE, leadTimePercentile);
		policies[6] = new OptimizationPolicy(48, 40,
				OptimizationPolicy.TangentType.ACTUAL,
				30, OptimizationPolicy.LeadTimeType.ACTUAL, leadTimePercentile);
		//		policies[7] = new OptimizationPolicy(36, 50,
		//				OptimizationPolicy.TangentType.MEAN_DEMAND,
		//				30, OptimizationPolicy.LeadTimeType.MEAN_LEAD_TIME, leadTimePercentile);


		long startTime;
		long endTime;

		for (int p = 0; p < nPolicies; ++p) { //policies.length; ++p) {
			startTime = System.nanoTime();

			outFile = new File(outFolder, p + ".txt");
			out = new PrintStream(new FileOutputStream(outFile));
			System.out.println("outFile = " + outFile.getAbsolutePath());

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
			policies[p].print(out);

			out.println("SDR\tSL\tSLe\tSLsd\tSLsde\tSLmin\tInv\tInve");

			for (double sdr = 0.6; sdr < 1.6; sdr += sdrTic) {

				SummaryStatistics slStats = new SummaryStatistics();
				SummaryStatistics invStats = new SummaryStatistics();
				// Service level standard deviation summary statistics object
				SummaryStatistics slsdStats = new SummaryStatistics();
				SummaryStatistics slRetStats = new SummaryStatistics();

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

					Simulator sim = SimulatorParser.readInputFiles(inFolder, delay, sdr,
							forecastVariance, forecastAccuracy, fl);
					sim.simulate(policies[p], nYearsWarmup, nYears, randomSeed);
					Simulator.Stats stats = sim.getStats();
					stats.print(System.out);
					slStats.addValue(stats.serviceLevel);
					invStats.addValue(stats.inventoryInDemandPerPeriod);
					slsdStats.addValue(stats.serviceLevelStdDev);
					for (int i = 0; i < sim.R; ++i) {
						slRetStats.addValue(sim.retailers[i].getStats().serviceLevel);
					}
				}
				
				out.format("%.2f", sdr);
				
				double width;
				width = Helper.getConfidenceIntervalWidth(slStats, 0.05);
				out.format("\t%.4f\t%.4f", slStats.getMean(), width);
				width = Helper.getConfidenceIntervalWidth(slsdStats, 0.05);
				out.format("\t%.4f\t%.4f", slsdStats.getMean(), width);
				out.format("\t%.4f", slRetStats.getMin());
				width = Helper.getConfidenceIntervalWidth(invStats, 0.05);
				out.format("\t%.2f\t%.2f" , invStats.getMean(), width);
				out.println();
			}

			endTime = System.nanoTime();

			out.format("# Time taken = %d s", (int) ((endTime - startTime)/1e9));
			out.println();
			out.close();
		}
	}

}
