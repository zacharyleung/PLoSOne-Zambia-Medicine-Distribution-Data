package invsim3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.zacleung.invsim.policy.OptimizationPolicy;
import com.zacleung.invsim.policy.OrderUpToDistrictPolicy;
import com.zacleung.invsim.policy.OrderUpToXDockPolicy;
import com.zacleung.invsim.policy.Policy;

public class MainScatter {
	/**
	 * Print out the distribution of the service levels and lead times.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48-v3";
		//String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\1-48-v3";
		//String folder = "/usr3/zacleung/zambia-java/input/212-48-v3";

		File outFolder = new File("scatter");
		File outFile;

		
		int nYearsWarmup = 2;
		int nYears = 3;
		int randomSeed = 0;

		double[] forecastVariance = {
				0.005, 0.01, 0.015, 0.02, 0.0225, 0.025,
				0.0275, 0.03, 0.0325, 0.035, 0.0375, 0.04,
				0.0425, 0.045, 0.0475, 0.065};
		double[] forecastAccuracy = {0, 0.5, 1};
		int delay = 0;
		double sdr = 0.8;
		int fl = 0;

		Policy policy;
		PrintStream out;

		long startTime;
		long endTime;

		Simulator sim;
		Simulator.Stats stats;

		double leadTimePercentile = 0.99;
		
		
		fl = 2;
		startTime = System.nanoTime();
		outFile = new File(outFolder, "district.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		policy = new OrderUpToDistrictPolicy(12, 12, 8);
		
		out.println("# Supply/demand ratio = " + sdr);
		out.println("# Delay = " + delay);
		out.println("# Forecast level = " + fl);
		
		sim = Main.readInputFiles(folder, delay, sdr,
				forecastVariance, forecastAccuracy, fl);

		sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

		stats = sim.getStats();

		policy.print(out);
		stats.printRetailers(out);
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();

		
		
		
		
		fl = 2;
		startTime = System.nanoTime();
		outFile = new File(outFolder, "xdock-out.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.RECENT_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		
		out.println("# Supply/demand ratio = " + sdr);
		out.println("# Delay = " + delay);
		out.println("# Forecast level = " + fl);
		
		sim = Main.readInputFiles(folder, delay, sdr,
				forecastVariance, forecastAccuracy, fl);

		sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

		stats = sim.getStats();

		policy.print(out);
		stats.printRetailers(out);
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();

		
		
		fl = 2;
		startTime = System.nanoTime();
		outFile = new File(outFolder, "xdock-fc.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		policy = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.AmcType.FUTURE_MONTHS,
				12, 16, Policy.AllocType.PRIORITY);
		
		out.println("# Supply/demand ratio = " + sdr);
		out.println("# Delay = " + delay);
		out.println("# Forecast level = " + fl);
		
		sim = Main.readInputFiles(folder, delay, sdr,
				forecastVariance, forecastAccuracy, fl);

		sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

		stats = sim.getStats();

		policy.print(out);
		stats.printRetailers(out);
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();

		
		
		
		fl = 2;
		startTime = System.nanoTime();
		outFile = new File(outFolder, "opt.txt");
		out = new PrintStream(new FileOutputStream(outFile));
		policy = new OptimizationPolicy(36, 30,
				OptimizationPolicy.TangentType.MULTI_PERIOD,
				30, OptimizationPolicy.LeadTimeType.CONSERVATIVE,
				leadTimePercentile);

		out.println("# Supply/demand ratio = " + sdr);
		out.println("# Delay = " + delay);
		out.println("# Forecast level = " + fl);
		
		sim = Main.readInputFiles(folder, delay, sdr,
				forecastVariance, forecastAccuracy, fl);

		sim.simulate(policy, nYearsWarmup, nYears, randomSeed);

		stats = sim.getStats();

		policy.print(out);
		stats.printRetailers(out);
		endTime = System.nanoTime();
		out.format("# Time elapsed = %.1f s", (endTime - startTime) / 1e9);
		out.println();
		out.close();


	}
}
