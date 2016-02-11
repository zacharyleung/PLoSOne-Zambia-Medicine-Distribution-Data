package invsim;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class InvSim {
	static long startTime;
	static long endTime;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		startTime = System.nanoTime();

		//ZachRvNormal.test();
		
		int T = 365 * 10;

		InvPol[] ip = new InvPol[6];
		ip[0] = new OutInvPol(OutInvPol.PAST_DEMAND,91,121);
		ip[1] = new OutInvPol(OutInvPol.FORECASTS,91,121);
		ip[2] = new DeiInvPol(360, 500, DeiInvPol.MEAN_DEMAND, 20,
				DeiInvPol.MEAN_LEAD_TIME);
		ip[3] = new DeiInvPol(360, 500, DeiInvPol.MEAN_DEMAND, 20,
				DeiInvPol.CONSERVATIVE);
		ip[4] = new DeiInvPol(360, 500, DeiInvPol.SINGLE_PERIOD, 20,
				DeiInvPol.CONSERVATIVE);
		ip[5] = new DeiInvPol(360, 500, DeiInvPol.MULTI_PERIOD, 20,
				DeiInvPol.CONSERVATIVE);
		
		//runOne(ip[0], "ami-stat.txt");
		//runOne(ip[0], "ami-nonstat.txt");
		//runOne(ip[1], "forecast.txt");
		//runOne(ip[2], "mean.txt");
		//runOne(ip[3], "single.txt");
		//runOne(ip[4], "multi.txt");
		
		//runTwo(ip[0], "past.txt");
		//runTwo(ip[1], "forecast.txt");
		//runTwo(ip[2], "mean.txt");
		//runTwo(ip[4], "multi.txt");
		
		//exp1(T,ip);
		//exp2(T,ip);
		//exp3(T,ip);
		//frontier(T, 3, 4, 12, "exp4/");
		
		//stochFrontier(T);
		
		endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime)/1e9 + " s");
	}

	private static void exp1(int T, InvPol[] ip) throws Exception {
		int S = 4;
		// Demand seasonality.
		double[] sArray = new double[S];
		int leadTimePri = 28;
		double[] mltArray = new double[S];
		int[] nwcoArray = new int[S];
		for (int s = 0; s < sArray.length; ++s) {
			sArray[s] = 1 + s * 1;
			mltArray[s] = 21;
			nwcoArray[s] = 0;
		}
		
		seasonality(T, sArray, leadTimePri, mltArray, nwcoArray, ip, "exp1/");
	}
	
	/**
	 * In this example, we have fixed seasonality, but varying lead
	 * times, even though the retailer is never cut off.
	 * @param T
	 * @param ip
	 * @throws Exception
	 */
	private static void exp2(int T, InvPol[] ip) throws Exception {
		int S = 5;
		// Demand seasonality.
		double[] sArray = new double[S];
		int leadTimePri = 28;
		double[] mltArray = new double[S];
		int[] nwcoArray = new int[S];
		for (int s = 0; s < sArray.length; ++s) {
			sArray[s] = 2.2;
			mltArray[s] = 7 + 7 * s;
			nwcoArray[s] = 0;
		}
		
		seasonality(T, sArray, leadTimePri, mltArray, nwcoArray, ip, "exp2/");
	}
		
	/**
	 * In this example, we have fixed seasonality, fixed mean lead
	 * times, but the retailer is cut off.
	 * @param T
	 * @param ip
	 * @throws Exception
	 */
	private static void exp3(int T, InvPol[] ip) throws Exception {
		int S = 5;
		// Demand seasonality.
		double[] sArray = new double[S];
		double[] mltArray = new double[S];
		int leadTimePri = 28;
		int[] nwcoArray = new int[S];
		for (int s = 0; s < sArray.length; ++s) {
			sArray[s] = 2.2;
			mltArray[s] = 21;
			nwcoArray[s] = 30 * s;
		}
		
		seasonality(T, sArray, leadTimePri, mltArray, nwcoArray, ip, "exp3/");
	}
	
	private static void runOne(InvPol ip, String filename) throws Exception {
		// 151.1/68.7 = 2.20 is the seasonality of the mean daily
		// demand in Zambia.
		double seasonality = 2.2;//2.20;
		int leadTimePri = 28;
		double meanLeadTime = 21;
		int nPeriodsCutOff = 0;
		int T = 365 * 100;
		
		StochInvSys sis = TestCaseInvSys.singleFacility(
				100, seasonality, leadTimePri, meanLeadTime, nPeriodsCutOff, 5, 4, 1.5);
		sis.simulate(T, ip, true);
		sis.printGraph(filename, ip);
		sis.printStats(System.out);
	}
	
	private static void runTwo(InvPol ip, String filename) throws Exception {
		double[] sArray = new double[2];
		sArray[0] = 2.2;
		sArray[1] = 2.2;
		int leadTimePri = 28;
		double[] mltArray = new double[2];
		mltArray[0] = 7;
		mltArray[1] = 42;
		int[] npcoArray = new int[2];
		npcoArray[0] = 0;
		npcoArray[1] = 0;
		double supplyDemandRatio = 0.99;
		
		int T = 365 * 10;
		StochInvSys sis = new TestCaseInvSys(
				100, sArray, leadTimePri, mltArray, npcoArray, supplyDemandRatio, 12, 1.5);
		sis.simulate(T, ip, true);
		sis.printGraph(filename, ip);
		ip.print(System.out);
		sis.printStats(System.out);
	}
	
	private static void seasonality(int T, double[] sArray,
			int leadTimePri, double[] mltArray, int[] nwcoArray,
			InvPol[] ip, String prefix) throws Exception {
		PrintStream out = new PrintStream(new FileOutputStream(
				prefix + "log.txt"));
		InvSys.Stats[][] statArray = new InvSys.Stats[sArray.length][ip.length];
		for (int s = 0; s < sArray.length; ++s) {
			for (int i = 0; i < ip.length; ++i) {
				StochInvSys sis = TestCaseInvSys.singleFacility(
						100, sArray[s], leadTimePri, mltArray[s], nwcoArray[s], 5, 4,
						TestCaseInvSys.NO_EXPIRY);
				sis.simulate(T, ip[i], true);
				sis.printGraph(prefix + s + "-" + i + ".txt", ip[i]);
				ip[i].print(out);
				statArray[s][i] = sis.printStats(out);
				sis.printStats(System.out);
			}
		}
		
		endTime = System.nanoTime();
		out.println("Took "+(endTime - startTime)/1e9 + " s");
		
		PrintStream table = new PrintStream(new FileOutputStream(
				prefix + "table.txt"));
		
		// Print the parameters of the inventory policies.
		for (int i = 0; i < ip.length; ++i)
			ip[i].print(table);
		
		table.print("Season\tMeanLT\tCutoff");
		for (int i = 0; i < ip.length; ++i)
			table.print("\tIP" + i + "Unm\tIP" + i + "Hold\tIP" + i +
					"Serv\tIP" + i + "Tot");
		table.println();
		for (int s = 0; s < sArray.length; ++s) {
			table.format("%.1f\t%.1f\t%d", sArray[s], mltArray[s], nwcoArray[s]);
			for (int i = 0; i < ip.length; ++i) {
				InvSys.Stats stat = statArray[s][i]; 
				table.format("\t%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
						stat.serviceLevel,stat.totalCost);
			}
			table.println();
		}
		table.close();
	}
	
	private static void frontier(int T, double demandSeasonality,
			double meanLeadTime, int nWeeksCutOff, String prefix)
					throws Exception {
	
		double unmetPenalty = 100;
		
		InvPol ip = null;
		//ip[0] = new OutInvPol(OutInvPol.PAST_DEMAND,12,16);
		//ip[1] = new OutInvPol(OutInvPol.FORECASTS,12,16);
		//ip[2] = new DeiInvPol(24, 20, DeiInvPol.SINGLE_PERIOD, 10,
		//		DeiInvPol.CONSERVATIVE);

		PrintStream out = new PrintStream(new FileOutputStream(
				prefix + "frontier-log.txt"));

		int iPolicy = -1;
		InvSys.Stats[] statArray;
		int K = 100;
		int k;
		PrintStream table;
		int leadTimePri = 28;
		
		
		// Run policy.
		++iPolicy;
		k = 0;
		statArray = new InvSys.Stats[K];
		for (int q = 200; q < 400; q+=20) {
			ip = new ConstInvPol(q);
			StochInvSys sis = TestCaseInvSys.singleFacility(unmetPenalty,
					demandSeasonality, leadTimePri, meanLeadTime, nWeeksCutOff,
					5, 4, 1.5);
			sis.simulate(T, ip, true);
			ip.print(out);
			statArray[k++] = sis.printStats(System.out);
		}
		
		// Print table.
		table = new PrintStream(new FileOutputStream(
				prefix + "frontier-" + iPolicy + ".txt"));
		ip.print(table);
		table.print("IPUnmet\tIPHolding\tIPService\tIPTotal");
		table.println();
		for (int i = 0; i < k; ++i) {
			InvSys.Stats stat = statArray[i]; 
			table.format("%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
					stat.serviceLevel,stat.totalCost);
			table.println();
			
		}
		table.close();

		// Run policy.
		++iPolicy;
		k = 0;
		statArray = new InvSys.Stats[K];
		for (double s = 2; s <= 24; s++) {
		    ip = new OutInvPol(OutInvPol.PAST_DEMAND,12,s);
			StochInvSys sis = TestCaseInvSys.singleFacility(unmetPenalty,
					demandSeasonality, leadTimePri, meanLeadTime, nWeeksCutOff,
					5, 4, 1.5);
			sis.simulate(T, ip, true);
			ip.print(out);
			statArray[k++] = sis.printStats(System.out);
		}

		// Print table.
		table = new PrintStream(new FileOutputStream(
				prefix + "frontier-" + iPolicy + ".txt"));
		ip.print(table);
		table.print("IPUnmet\tIPHolding\tIPService\tIPTotal");
		table.println();
		for (int i = 0; i < k; ++i) {
			InvSys.Stats stat = statArray[i]; 
			table.format("%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
					stat.serviceLevel,stat.totalCost);
			table.println();
			
		}
		table.close();

		// Run policy.
		++iPolicy;
		k = 0;
		statArray = new InvSys.Stats[K];
		for (double s = 2; s <= 24; s++) {
			ip = new OutInvPol(OutInvPol.FORECASTS,12,s);
			StochInvSys sis = TestCaseInvSys.singleFacility(unmetPenalty,
					demandSeasonality, leadTimePri, meanLeadTime, nWeeksCutOff,
					5, 4, 1.5);
			sis.simulate(T, ip, true);
			ip.print(out);
			statArray[k++] = sis.printStats(System.out);
		}

		// Print table.
		table = new PrintStream(new FileOutputStream(
				prefix + "frontier-" + iPolicy + ".txt"));
		ip.print(table);
		table.print("IPUnmet\tIPHolding\tIPService\tIPTotal");
		table.println();
		for (int i = 0; i < k; ++i) {
			InvSys.Stats stat = statArray[i]; 
			table.format("%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
					stat.serviceLevel,stat.totalCost);
			table.println();

		}
		table.close();

		// Run policy.
		++iPolicy;
		k = 0;
		statArray = new InvSys.Stats[K];
		for (double p = 1; p <= 20; p+=1) {
			ip = new DeiInvPol(24, p, DeiInvPol.SINGLE_PERIOD, 10,
					DeiInvPol.CONSERVATIVE);
			StochInvSys sis = TestCaseInvSys.singleFacility(unmetPenalty,
					demandSeasonality, leadTimePri, meanLeadTime, nWeeksCutOff,
					5, 4, 1.5);
			sis.simulate(T, ip, true);
			ip.print(out);
			statArray[k++] = sis.printStats(System.out);
		}

		// Print table.
		table = new PrintStream(new FileOutputStream(
				prefix + "frontier-" + iPolicy + ".txt"));
		ip.print(table);
		table.print("IPUnmet\tIPHolding\tIPService\tIPTotal");
		table.println();
		for (int i = 0; i < k; ++i) {
			InvSys.Stats stat = statArray[i]; 
			table.format("%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
					stat.serviceLevel,stat.totalCost);
			table.println();

		}
		table.close();
		
		// Run policy.
		++iPolicy;
		k = 0;
		statArray = new InvSys.Stats[K];
		for (double p = 1; p <= 20; p+=1) {
			ip = new DeiInvPol(24, p, DeiInvPol.MULTI_PERIOD, 10,
					DeiInvPol.CONSERVATIVE);
			StochInvSys sis = TestCaseInvSys.singleFacility(unmetPenalty,
					demandSeasonality, leadTimePri, meanLeadTime, nWeeksCutOff,
					5, 4, 1.5);
			sis.simulate(T, ip, true);
			ip.print(out);
			statArray[k++] = sis.printStats(System.out);
		}

		// Print table.
		table = new PrintStream(new FileOutputStream(
				prefix + "frontier-" + iPolicy + ".txt"));
		ip.print(table);
		table.print("IPUnmet\tIPHolding\tIPService\tIPTotal");
		table.println();
		for (int i = 0; i < k; ++i) {
			InvSys.Stats stat = statArray[i]; 
			table.format("%.1f\t%.1f\t%.3f\t%.1f",stat.unmetDemand,stat.holdingCost,
					stat.serviceLevel,stat.totalCost);
			table.println();

		}
		table.close();

		endTime = System.nanoTime();
		out.println("Took "+(endTime - startTime)/1e9 + " s");
		
		out.close();
	}
	
	private static void testZipkin(int T) throws Exception{
		int[] parray = new int[4];
		parray[0] = 4;
		parray[1] = 9;
		parray[2] = 19;
		parray[3] = 39;
		
		ZipkinInvSys sys;
		
		InvPol ip;
		
		PrintStream out = new PrintStream(new FileOutputStream("zipkin.txt"));
		
		for (int L = 1; L <= 4; L++) {
			for (int k = 0; k <= 3; k++) {
				int p = parray[k];
				//sys = new ZipkinInvSys("geometric",L,p,1);
				sys = new ZipkinInvSys("poisson",L,p,1);
				//ip = new DeiInvPol(L+1);
				ip = new SampInvPol(L+2,100,1);
				//ip = new ConstInvPol(3);
				sys.simulate(T, ip, true);
				sys.printStats(System.out);
				sys.printStats(out);
				ip.print(out);
			}
		}

		endTime = System.nanoTime();
		out.println("Took "+(endTime - startTime)/1e9 + " s");
		
		out.close();

	}

}
