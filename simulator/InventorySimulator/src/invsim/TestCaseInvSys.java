package invsim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Scanner;

import d65helper.Helper;

/**
 * This class defineds a single-facility test case for various
 * inventory policies.
 * 
 * Lead times are geometric with max lead time equal to a year,
 * and mean lead time approximately equal to the specified
 * parameter.
 *  
 * @author Zack
 *
 */
public class TestCaseInvSys extends StochInvSys {
	private double[] demandSeasonality;
	private int leadTimePri;
	private double[] meanLeadTime;
	private int[] nPeriodsCutOff;
	private double supplyDemandRatio;
	private int nWarehouseShipments;
	
	/** Number of periods in a year. */
	private static final int NUMBER_OF_PERIODS = 365;
	// I set this so demand in one month is 1000 units on average.
	private static final double MEAN_PERIOD_DEMAND = 1000.0 * 12 / NUMBER_OF_PERIODS;
	private static final int PRIMARY_LEAD_TIME = 28;
	public static final double NO_EXPIRY = -1;
	
	/**
	 * 
	 * @param p
	 * @param demandSeasonality
	 * @param leadTimePri Deterministic lead time of primary distribution.
	 * @param meanLeadTimeSec Mean lead time of secondary distribution.
	 * @param nPeriodsCutOff
	 * @param supplyDemandRatio
	 * @param nWarehouseShipments
	 * @param nYearsToExpiry
	 * @throws Exception
	 */
	public TestCaseInvSys(double p, double[] demandSeasonality, 
			int leadTimePri,
			double[] meanLeadTimeSec, int[] nPeriodsCutOff,
			double supplyDemandRatio, int nWarehouseShipments,
			double nYearsToExpiry) throws Exception
	{
		 super(1, p, getDemandMeanYear(demandSeasonality),
				 getLeadTimePmf(leadTimePri, meanLeadTimeSec, nPeriodsCutOff),
				 getDemandCv(), 0.01,
				 getWarehouseShipments(demandSeasonality.length, supplyDemandRatio, nWarehouseShipments),
				 getExpiry(nYearsToExpiry));
		 
		 this.demandSeasonality = demandSeasonality;
		 this.leadTimePri = leadTimePri;
		 this.meanLeadTime = meanLeadTimeSec;
		 this.nPeriodsCutOff = nPeriodsCutOff;
		 this.supplyDemandRatio = supplyDemandRatio;
		 this.nWarehouseShipments = nWarehouseShipments;
	}
	
	private static double[][] getDemandMeanYear(double[] demandSeasonality)
			throws Exception {
		// Number of retailers
		int R = demandSeasonality.length;
		// The number of periods in a year
		int Y = NUMBER_OF_PERIODS;
		// The number of days in a year
		int DAYS_IN_YEAR = 365;
		
		// Daily demand that we read from data
		double[] demandDay = new double[DAYS_IN_YEAR];
		// Scale the demand to per-period demand
		double[] demandPeriod = new double[Y];

		// The min and max mean demand in a period.
		double dMin = MEAN_PERIOD_DEMAND;
		double dMax = MEAN_PERIOD_DEMAND;

		double[][] demandMeanYear = new double[R][Y];
		
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(
				"daily-demand.txt")));
		// Skip the header row.
		scanner.nextLine();
		for (int i = 0; i < DAYS_IN_YEAR; ++i) {
			demandDay[i] = MEAN_PERIOD_DEMAND * scanner.nextDouble();
			scanner.nextDouble();
			System.out.println(demandDay[i]);
		}
		
//		// Shift demand so we start in July instead of January. (turned off)
//		int daysToShift = 0;
//		double[] temp = new double[DAYS_IN_YEAR];
//		for (int i = 0; i < DAYS_IN_YEAR; ++i)
//			temp[i] = demandDay[Helper.modulo(i, DAYS_IN_YEAR)];
//		for (int i = 0; i < DAYS_IN_YEAR; ++i)
//			demandDay[i] = temp[i];
				
		// We cut up each day into Y pieces.
		double[] dailyCutUp = new double[365*Y];
		for (int i = 0; i < 365; ++i)
			for (int j = i * Y; j < (i+1) * Y; ++j)
				dailyCutUp[j] = demandDay[i] / Y;
		
		// Now we compute the demand at each period
		for (int i = 0; i < Y; ++i) {
			for (int j = i * DAYS_IN_YEAR; j < (i+1) * DAYS_IN_YEAR; ++j) {
				demandPeriod[i] += dailyCutUp[j];
			}
			dMin = Math.min(demandPeriod[i], dMin);
			dMax = Math.max(demandPeriod[i], dMax);
			//System.out.format("%d\t%.1f\n",i,demandMeanYear[0][i]);
		}
		
		for (int r = 0; r < R; ++r) {
			double s = demandSeasonality[r];
			double num = (dMax - dMin * s);
			double den = s * (MEAN_PERIOD_DEMAND - dMin) + dMax - MEAN_PERIOD_DEMAND;
			double a = num / den;
			for (int i = 0; i < Y; ++i) {
				demandMeanYear[r][i] =
						a * MEAN_PERIOD_DEMAND + (1 - a) * demandPeriod[i];
			}
		}
		
		return demandMeanYear;
	}
	
	private static double[][][] getLeadTimePmf(int minLeadTime,
			double[] meanLeadTime, int[] nPeriodsCutOff) {
		int R = meanLeadTime.length;
		int Y = NUMBER_OF_PERIODS;
		double[][][] leadTimePmf = new double[R][Y][];
		
		for (int r = 0; r < R; ++r) {
			boolean[] isAccessible = new boolean[Y];
			// The inaccessibility happens during months centered around
			// March.
			int t1 = Y / 4 - (nPeriodsCutOff[r] + 1) / 2;
			int t2 = Y / 4 + nPeriodsCutOff[r] / 2;
			for (int t = 0; t < Y; ++t)
				isAccessible[t] = true;
			for (int t = t1; t < t2; ++t)
				isAccessible[Helper.modulo(t, Y)] = false;
			System.out.println("Cut-off from [" + t1 + " to " + t2 + ")");
			// Not accessible during the other weeks.

			// Probability of making a visit in each week.
			double v = 1 / meanLeadTime[r];
			for (int i = 0; i < 12; ++i) {
				int t = (int) Math.round((double) i / 12 * Y);
				// Remaining probability.
				double rp = 1;
				leadTimePmf[r][t] = new double[Y];
				for (int y = minLeadTime; y < Y-1; ++y) {
					if (isAccessible[Helper.modulo(t+y, Y)]) {
						// If the remaining probability is really small,
						// then if it is accessible, make the delivery
						// happen.
						//System.out.println(rp);
						if (rp < 0.001) {
							leadTimePmf[r][t][y] = rp;
							rp = 0;
							break;
						} else {
							leadTimePmf[r][t][y] = rp * v;
							rp *= (1-v);
						}
					}
					//System.out.format("%d %d %.6f\n",t,y,leadTimePmf[0][t][y]);
				}
				leadTimePmf[r][t][Y-1] = rp;
			}
		}
		
		return leadTimePmf;
		
//		// This code is for debugging, to check that the standard
//		// deviations of the product of uncertainties is as desired.
//		for (int i = 0; i < M; ++i) {
//			// x is the sum of the variances from 0 to i inclusive.
//			double sumvar = 0;
//			//for (int j = 0; j <= i; ++j) {
//			for (int j = i; j >=0; --j) {
//				sumvar += (sigmaD[j] * sigmaD[j]);
//			}
//			//System.out.println(Math.exp(x)-1);
//			System.out.println(Math.sqrt(Math.exp(sumvar)-1));
//		}

	}
	
	private static double[] getDemandCv() {
		// Demand uncertainty is only resolved M weeks in advance.
		int M = 14;
		double[] demandCv = new double[M+1];
		for (int i = 0; i < M; ++i) {
			//demandCv[i] = 0.2 * (1.0 - Math.pow(0.5, i+1));
			demandCv[i] = 0.15 + 0.05 * i / (M-1);
			//System.out.println("demandCv[" + i + "] = " + demandCv[i]);
		}
		demandCv[M] = demandCv[M-1];
		return demandCv;
	}
	
	private static int[] getWarehouseShipments(int R,
			double supplyDemandRatio, int nWarehouseShipments)
	{
		int[] warehouseShipments = new int[NUMBER_OF_PERIODS];
		double yearDemand = MEAN_PERIOD_DEMAND * NUMBER_OF_PERIODS;

		for (int i = 0; i < nWarehouseShipments; ++i) {
			int day = (int) Math.round((double) i / nWarehouseShipments * NUMBER_OF_PERIODS);
			warehouseShipments[day] = R * (int) (supplyDemandRatio * yearDemand / nWarehouseShipments);
		}
	
		return warehouseShipments;
	}
	
	private static int getExpiry(double nYearsToExpiry) {
		return (int) Math.round(nYearsToExpiry * NUMBER_OF_PERIODS);
	}
	
	protected void printParameters(PrintStream out) {
		out.println("# Test Case Inv Sys");
		out.println("# Lost sales penalty          = " + p);
		out.print("# Demand seasonality          = ");
		for (int r = 0; r < R; ++ r)
			out.format("%.2f ",demandSeasonality[r]);
		out.println();
		out.print("# Lead time (primary)         = ");
		out.println(leadTimePri);
		out.print("# Mean lead time (secondary)  = ");
		for (int r = 0; r < R; ++ r)
			out.format("%.1f ",meanLeadTime[r]);
		out.println();
		out.print("# Number of weeks cut off     = ");
		for (int r = 0; r < R; ++ r)
			out.format("%d ",nPeriodsCutOff[r]);
		out.println();
		out.println("# Number of periods simulated = " + simHorizon);
		out.println("#");
	}
	
	/** 
	 * A convenience method to define a single facility.
	 * @param p
	 * @param demandSeasonality
	 * @param meanLeadTimeSec
	 * @param nPeriodsCutOff
	 * @param supplyDemandRatio
	 * @param nWarehouseShipments
	 * @param nYearsToExpiry
	 * @return
	 * @throws Exception
	 */
	public static TestCaseInvSys singleFacility(double p, double demandSeasonality, 
			int leadTimePri, double meanLeadTimeSec, int nPeriodsCutOff,
			double supplyDemandRatio, int nWarehouseShipments,
			double nYearsToExpiry) throws Exception {
		double[] ds = new double[1];
		ds[0] = demandSeasonality;
		double[] mlt = new double[1];
		mlt[0] = meanLeadTimeSec;
		int[] npco = new int[1];
		npco[0] = nPeriodsCutOff;
		return new TestCaseInvSys(p, ds, leadTimePri, mlt, npco, supplyDemandRatio,
				nWarehouseShipments, nYearsToExpiry);
	}
}
