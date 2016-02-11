package invsim3;

import java.io.File;

import com.zacleung.util.Matrix;
import com.zacleung.util.Vector;


import d65helper.Helper;

public abstract class SimulatorParser {
	/**
	 * Create a simulator based on input files in a containing folder.
	 * The simulator will have Y periods in a year.
	 */
	public static Simulator.Builder readInputFiles(String folder, int delay,
			double supplyDemandRatio, double[] forecastVariance,
			double[] forecastAccuracy, int forecastLevel,
			int shiftLeft) throws Exception {

		return readInputFiles(new File(folder), delay, supplyDemandRatio,
				forecastVariance, forecastAccuracy, forecastLevel,
				shiftLeft);
	}

	public static Simulator.Builder readInputFiles(File folder, int delay,
			double supplyDemandRatio, double[] forecastVariance,
			double[] forecastAccuracy, int forecastLevel,
			int shiftLeft) throws Exception {
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
		.numberOfPeriodsInCycle(4)
		.primaryCycleOffset(primaryCycleOffset)
		.secondaryCycleOffset(secondaryCycleOffset)
		.primaryLeadTime(primaryLeadTime)
		.meanSecondaryLeadTime(meanSecondaryLeadTime)
		.retailerToDistrict(retailerToDistrict)
		.accessibility(accessibility);

		Simulator.Builder builder = new Simulator.Builder()
		.withNumberOfWarehouseShipmentsPerYear(4)
		.withSupplyDemandRatio(100)
		.withForecastAccuracy(forecastAccuracy)
		.withForecastVariance(forecastVariance)
		.withDemandMean(demandMean)
		.withLeadTimeBuilder(leadTimeBuilder);

		return builder;
	}

}
