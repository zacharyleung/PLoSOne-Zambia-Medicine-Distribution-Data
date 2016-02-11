package invsim;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		exp20120823();
	}

	private static void exp20120823() throws Exception {
		String folder = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48";
		double[] forecastVariance = {0.2, 0.1, 0.05, 0.025};
		double[] forecastAccuracy = {0, 0.5, 1};
		Policy[] policies = new Policy[2];
		int nYears = 1;
		int randomSeed = 0;
		
		policies[0] = new OrderUpToXDockPolicy(OrderUpToXDockPolicy.PAST_DEMAND, 12, 12);
		policies[1] = new OptimizationPolicy(24, 100,
				OptimizationPolicy.MULTI_PERIOD,
				30, OptimizationPolicy.CONSERVATIVE);
		
		PrintStream out = new PrintStream(new FileOutputStream("exp20120823.txt"));
		
		out.println("SDR\tP0-0-SL\tP0-0-In\tP0-1-SL\tP0-1-In\tP0-2-SL\tP0-2-In\t");
		
		for (double sdr = 0.6; sdr < 1.4; sdr += 0.2) {
			out.format("%.2f\t", sdr);
			for (int fl = 2; fl < 3; ++fl) {
				for (int p = 0; p < policies.length; ++p) {
					Simulator sim = Simulator.readInputFiles(folder, sdr,
							forecastVariance, forecastAccuracy, fl);
					sim.simulate(policies[p], nYears, randomSeed);
					Simulator.Stats stats = sim.getStats();
					out.format("%.4f\t%.1f\t", stats.serviceLevel,
							stats.inventoryInDemandPerPeriod);
					sim.getStats().print(System.out);
				}
			}
			out.println();
		}
		
		out.close();
	}
}
