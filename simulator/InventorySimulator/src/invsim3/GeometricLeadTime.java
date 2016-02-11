package invsim3;

import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.zacleung.util.ArrayUtils;

import d65helper.FlexArray2D;
import d65helper.Helper;

/**
 * This class implements geometric lead times.
 * 
 * Every C periods, the warehouse can make a shipment.
 * 
 * There is a constant probability p of making a delivery each
 * period.  The probability may be reduced by the accessibility
 * a in that period, to a * p.
 *  
 * @author zacleung
 */
public class GeometricLeadTime extends LeadTime {
	private double[][] accessibility;
	private int[] primaryLeadTime;
	/** The warehouse can make a shipment every this number of periods. */
	private double[] meanSecondaryLeadTime;
	/** Visit probability in each week. */
	private double[] v;
	/** Realized total lead times. */
	private FlexArray2D<Integer> realTotalLeadTime;
	/** Realized secondary lead times. */
	private FlexArray2D<Integer> realSecondaryLeadTime;

	GeometricLeadTime(int C, int[] OP, int[] OS, int D, int Y, int[] R2D,
			int[] primaryLeadTime,
			double[] meanSecondaryLeadTime, double[][] accessibility) {
		super(C, OP, OS, D, Y, R2D);

		this.primaryLeadTime = primaryLeadTime;
		this.meanSecondaryLeadTime = meanSecondaryLeadTime;
		this.accessibility = accessibility;

		// The probability of visiting in each week
		this.v = new double[R];
		for (int r = 0; r < R; ++r) {
			v[r] = 1.0 / (meanSecondaryLeadTime[r] + 0.0);
		}
	}

	public static class Builder {
		private int _numberOfPeriodsInCycle;
		private int[] _primaryCycleOffset;
		private int[] _secondaryCycleOffset;
		private int _communicationDelayInPeriods;
		private int[] _retailerToDistrict;
		private int[] _primaryLeadTime;
		private double[] _meanSecondaryLeadTime;
		private double[][] _accessibility;

		public Builder numberOfPeriodsInCycle(int numberOfPeriodsInCycle) {
			_numberOfPeriodsInCycle = numberOfPeriodsInCycle;
			return this;
		}
		
		public Builder primaryCycleOffset(int[] primaryCycleOffset) {
			_primaryCycleOffset = primaryCycleOffset.clone();
			return this;
		}
		
		public Builder secondaryCycleOffset(int[] secondaryCycleOffset) {
			_secondaryCycleOffset = secondaryCycleOffset.clone();
			return this;
		}
		
		
		public Builder communicationDelayInPeriods(int communicationDelayInPeriods) {
			_communicationDelayInPeriods = communicationDelayInPeriods;
			return this;
		}
		
		public Builder retailerToDistrict(int[] retailerToDistrict) {
			_retailerToDistrict = retailerToDistrict.clone();
			return this;
		}
		
		public Builder primaryLeadTime(int[] primaryLeadTime) {
			_primaryLeadTime = primaryLeadTime.clone();
			return this;
		}
		
		public Builder meanSecondaryLeadTime(double[] meanSecondaryLeadTime) {
			_meanSecondaryLeadTime = meanSecondaryLeadTime.clone();
			return this;
		}
		
		public double[] getMeanSecondaryLeadTime() {
			return ArrayUtils.copyOf(_meanSecondaryLeadTime);
		}
		
		public Builder accessibility(double[][] accessibility) {
			_accessibility = new double[accessibility.length][];
			for (int i = 0; i < accessibility.length; ++i) {
				_accessibility[i] = accessibility[i].clone();
			}
			return this;
		}
		
		public GeometricLeadTime build() {
			int numberOfPeriodsInYear = _accessibility[0].length;
			return new GeometricLeadTime(
					_numberOfPeriodsInCycle,
					_primaryCycleOffset,
					_secondaryCycleOffset,
					_communicationDelayInPeriods,
					numberOfPeriodsInYear,
					_retailerToDistrict,
					_primaryLeadTime,
					_meanSecondaryLeadTime,
					_accessibility);
		}
	}
	
	/**
	 * Generate lead time randomness.
	 * @param randomSeed
	 * @param tStart
	 * @param tEnd
	 */
	public void generate(int tStart, int tEnd, int randomSeed) {
		// Generate random numbers
		RandomDataImpl rand = new RandomDataImpl();
		rand.reSeed(randomSeed);

		//System.out.println("GeometricLeadTime.generate()");

		FlexArray2D<Boolean> isVisited = new FlexArray2D<>(0, R, tStart, tEnd);
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				// If there is a visit in this week
				if (rand.nextUniform(0, 1) < v[r] * getAccessibility(r, t)) {
					isVisited.set(r, t, true);
					//System.out.println("Visit at week " + t);
				} else {
					isVisited.set(r, t, false);
				}
			}
		}

		realTotalLeadTime = new FlexArray2D<>(0, R, tStart, tEnd);
		for (int r = 0; r < R; ++r) {
			int d = R2D[r];
			for (int t = tStart; t < tEnd; ++t) {
				// By default, there is no lead time
				realTotalLeadTime.set(r, t, NO_LEAD_TIME);
				// But if it is a shipment week and we can find a future
				// visit, then set the lead time
				if (isTotalShipmentPeriod(r, t)) {
					// Note the y = ... + 1 as shipments can't arrive at the
					// retailer in the same period that they reach the district
					int plt = 2 * D + primaryLeadTime[d];
					for (int y = t + plt + 1; y < tEnd; ++y) {
						if (isVisited.get(r, y)) {
							realTotalLeadTime.set(r, t, y - t);
							//System.out.format("r = %d, t = %d, Lead time = %d\n", r, t, y - t);
							break;
						}
					}
				}
			}
		}

		realSecondaryLeadTime = new FlexArray2D<>(0, R, tStart, tEnd);
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				// By default, there is no lead time
				realSecondaryLeadTime.set(r, t, NO_LEAD_TIME);
				// But if it is a shipment week and we can find a future
				// visit, then set the lead time
				if (isSecondaryShipmentPeriod(r, t)) {
					// Note the y = ... + 1 as shipments can't arrive at the
					// retailer in the same period that they reach the district
					for (int y = t + D + 1; y < tEnd; ++y) {
						if (isVisited.get(r, y)) {
							realSecondaryLeadTime.set(r, t, y - t);
							//System.out.format("r = %d, t = %d, Lead time = %d\n", r, t, y - t);
							break;
						}
					}
				}
			}
		}

	}

	@Override
	public double[] getTotalLeadTimePmf(int r, int t) {
		// If this is not a total shipment period, we can quit here
		if (!isTotalShipmentPeriod(r, t)) {
			return new double[0];
		}

		// Remaining probability.
		double rp = 1;
		// Note the y = ... + 1 as shipments can't arrive at the
		// retailer in the same period that they reach the district
		int d = R2D[r];
		int plt = 2 * D + primaryLeadTime[d];
		LinkedList<Double> list = new LinkedList<>();
		int y;
		for (y = t; y < t + plt + 1; ++y) {
			list.add(0.0);
		}
		for (; ; ++y) {
			double a = getAccessibility(r, y);
//			System.out.println("GeometricLeadTime.getTotalLeadTimePmf()");
//			System.out.format("y = %d, a = %.2f", y, a);
//			System.out.println();
			
			// If the remaining probability is really small,
			// then if it is accessible, make the delivery
			// happen
			//System.out.println(rp);
			if (a > 0 && rp < 0.001) {
				list.add(rp);
				rp = 0;
				break;
			} else {
				list.add(rp * v[r] * a);
				rp *= (1 - v[r] * a);
			}
			//System.out.format("%d %d %.6f\n",t,y,leadTimePmf[0][t][y]);
		}

		double[] array = new double[list.size()];
		int i = 0;
		for (Double thedouble : list) {
			array[i] = thedouble.doubleValue();
			++i;
		}
		return array;
	}

	@Override
	public double getAccessibility(int r, int t) {
		return accessibility[r][Helper.modulo(t, Y)];
	}

	public double[] getMeanSecondaryLeadTime() {
		return meanSecondaryLeadTime.clone();
	}
	
	/**
	 * A simple test that the GeometricLeadTime object works
	 * correctly without bugs.
	 * @param args
	 */
	public static void main(String[] args) {
		test1();
		//test2();
	}

	private static void test1() {
		int C = 5;
		int[] OP = {0};
		int[] OS = {0};
		int D = 0;
		int Y = 10;
		int[] R2D = {0};
		int[] primaryLeadTime = {2};
		double[] meanSecondaryLeadTime = {2};
		double[][] accessibility = {{0, 0, 0, 0, 0, 1, 1, 1, 1, 1}};

		int tStart = -5;
		int tEnd = 10;

		GeometricLeadTime geo = new GeometricLeadTime(C, OP, OS, D, Y, R2D,
				primaryLeadTime, meanSecondaryLeadTime, accessibility);

		geo.generate(tStart, tEnd, 0);

		System.out.println("Total lead time pmf");
		for (int i = 0; i < 10; ++i) {
			double[] pmf = geo.getTotalLeadTimePmf(0, i);
			System.out.print("Week " + i + " ");
			for (int j = 0; j < pmf.length; ++j) {
				System.out.format("%.4f\t", pmf[j]);
			}
			System.out.println();
		}

		int count = 0;
		double sum = 0;
		for (int t = tStart; t < tEnd; ++t) {
			//System.out.println(t);
			int l = geo.getTotalLeadTime(0, t); 
			if (l != GeometricLeadTime.NO_LEAD_TIME) {
				sum += l;
				++count;
			}
		}
		System.out.println("Mean lead time = " + sum / count);
	}

	private static void test2() {
		//		int tStart = -5;
		//		int tEnd = 100000;
		//		double[] access = {1, 1, 1, 1, 1, 0, 0, 0.5, 0.5, 0.5};
		//		GeometricLeadTime geo = new GeometricLeadTime(5, 3, 2,
		//				4, access);
		//		geo.generate(tStart, tEnd, 0);
		//
		//		double[][] pmf = geo.getTotalLeadTimePmf(0, 10);
		//		for (int i = 0; i < pmf.length; ++i) {
		//			for (int j = 0; j < pmf[i].length; ++j) {
		//				System.out.format("%.4f\t", pmf[i][j]);
		//			}
		//			System.out.println();
		//		}
		//
		//		int count = 0;
		//		double sum = 0;
		//		for (int t = tStart; t < tEnd; ++t) {
		//			//System.out.println(t);
		//			int l = geo.getTotalLeadTime(t); 
		//			if (l != GeometricLeadTime.NO_LEAD_TIME) {
		//				sum += l;
		//				++count;
		//			}
		//		}
		//		System.out.println("Mean lead time = " + sum / count);
	}

	@Override
	public int getPrimaryLeadTime(int d, int t) {
		if (isPrimaryShipmentPeriod(d, t)) {
			return D + primaryLeadTime[d];
		} else {
			return NO_LEAD_TIME;
		}
	}

	@Override
	public int getSecondaryLeadTime(int r, int t) {
		return realSecondaryLeadTime.get(r, t);
	}

	@Override
	public int getTotalLeadTime(int r, int t) {
		return realTotalLeadTime.get(r, t);
	}

	@Override
	public double getMeanAccessibility(int r) {
		SummaryStatistics stats = new SummaryStatistics();
		for (int y = 0; y < Y; ++y)
			stats.addValue(accessibility[r][y]);
		
		return stats.getMean();
	}
}
