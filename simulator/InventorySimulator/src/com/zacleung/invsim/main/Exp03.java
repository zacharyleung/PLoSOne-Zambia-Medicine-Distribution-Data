package com.zacleung.invsim.main;

import invsim3.GeometricLeadTime;
import invsim3.LeadTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.common.base.Stopwatch;
import com.zacleung.util.Matrix;
import com.zacleung.util.Vector;

import d65helper.Helper;

/**
 * Simulate a single retailer for some number of years.
 * Write an output file of (timestep, lead time).
 * 
 * 10^5 years requires 4 minutes.
 * 
 * @author zacharyleung
 *
 */
public class Exp03 {
	private static int numberOfYears = (int) Math.pow(10, 4);
	private static String outputFile = "output/exp03/java.txt";
	
	public static void main(String[] args) throws Exception {
		Stopwatch stopwatch = new Stopwatch().start();
		System.out.println("Running Exp03.java...");
		System.out.printf("number of years = %d\n", numberOfYears);
		
		PrintStream out = new PrintStream(
				new FileOutputStream(
						new File(outputFile)));
		
		String folder = "input/1-48-v3-all";
		int shiftLeft = 0;
		File file = null;

		file = new File(folder, "number-of-years.dat");
		int Y = Helper.readInt(file);

		file = new File(folder, "number-of-districts.dat");
		int D = Helper.readInt(file);

		file = new File(folder, "number-of-retailers.dat");
		int R = Helper.readInt(file);

		file = new File(folder, "accessibility.dat");
		double[][] accessibility = Matrix.readDoubleMatrix(file, R, Y);
		for (int r = 0; r < R; ++r)
			for (int t = 0; t < Y; ++t)
				accessibility[r][t] /= 100;

		file = new File(folder, "demand-means.dat");
		double[][] demandMean = Matrix.readDoubleMatrix(file, R, Y);

		file = new File(folder, "retailers-to-districts.dat");
		int[] retailerToDistrict = Helper.readInt2D(file, 1, R)[0];


		file = new File(folder, "districts.dat");
		double[][] temp = Matrix.readDoubleMatrix(file, D, 3);
		int[] primaryCycleOffset = new int[D];
		int[] primaryLeadTime = new int[D];
		for (int d = 0; d < D; ++d) {
			primaryCycleOffset[d] = (int) Math.round(temp[d][0]);
			primaryLeadTime[d] = (int) Math.round(temp[d][1]);
		}
		double[] meanSecondaryLeadTime = new double[R];
		for (int r = 0; r < R; ++r) {
			int d = retailerToDistrict[r];
			meanSecondaryLeadTime[r] = temp[d][2];
		}

		// If we are in a district system, then the retailers order
		// during the first week of every cycle
		int[] secondaryCycleOffset = new int[R];
		for (int r = 0; r < R; ++r)
			secondaryCycleOffset[r] = 0;

		// Implement the circular shift left
		for (int r = 0; r < R; ++r) {
			demandMean[r] = Vector.circularShiftLeft(demandMean[r], shiftLeft);
			accessibility[r] = Vector.circularShiftLeft(accessibility[r], shiftLeft);
		}
		
		GeometricLeadTime.Builder leadTimeBuilder = new GeometricLeadTime.Builder()
		.numberOfPeriodsInCycle(1)
		.primaryCycleOffset(primaryCycleOffset)
		.secondaryCycleOffset(secondaryCycleOffset)
		.primaryLeadTime(primaryLeadTime)
		.meanSecondaryLeadTime(meanSecondaryLeadTime)
		.retailerToDistrict(retailerToDistrict)
		.accessibility(accessibility);

		LeadTime leadTime = leadTimeBuilder.build();
		
		leadTime.generate(0, 48 * (numberOfYears + 1), 0);
		
		out.println("timestep,lead_time");
		for (int i = 0; i < 48 * numberOfYears; ++i) {
			out.printf("%d,%d\n", i, leadTime.getTotalLeadTime(0, i));
		}
		
		out.close();
		
		System.out.println("Exp03.java finished");
		System.out.println("Time elapsed = " + stopwatch.stop());
	}

}
