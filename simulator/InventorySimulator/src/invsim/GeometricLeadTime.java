package invsim;

import org.apache.commons.math.random.RandomDataImpl;

import d65helper.FlexArray1D;
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
	private double[] access;
	/** Number of periods in a year. */
	private int Y;
	/** The warehouse can make a shipment every this number of periods. */
	private int nPeriodsInCycle;
	private int cycleOffset;
	private int minLeadTime;
	private double meanLeadTime;
	/** Visit probability in each week. */
	private double v;
	/** Realized lead times. */
	private FlexArray1D<Integer> realLeadTime;

	public GeometricLeadTime(int nPeriodsInCycle, int cycleOffset,
			int minLeadTime, double meanLeadTime, double[] accessibility) {
		this.nPeriodsInCycle = nPeriodsInCycle;
		this.cycleOffset = cycleOffset;
		this.minLeadTime = minLeadTime;
		this.meanLeadTime = meanLeadTime;
		this.access = accessibility;
		this.Y = access.length;

		// The probability of visiting in each week
		this.v = 1 / (meanLeadTime + 1 - minLeadTime);

		// Parameter checking
		if (cycleOffset < 0 || cycleOffset >= Y) {
			throw new IllegalArgumentException(
					"Invalid cycleOffset " + cycleOffset + " Y " + Y);
		}

		if (minLeadTime > meanLeadTime) {
			throw new IllegalArgumentException(
					"minLeadTime " + minLeadTime
					+ " exceeds meanLeadTime " + meanLeadTime + "!");
		}

//		for (int t = tStart; t < tEnd; ++t) {
//			System.out.println(t + " " + realLeadTime.get(t));
//		}
	}

	/**
	 * Generate lead time randomness.
	 * @param randomSeed
	 * @param tStart
	 * @param tEnd
	 */
	public void generate(int randomSeed, int tStart, int tEnd) {
		// Generate random numbers
		RandomDataImpl rand = new RandomDataImpl();
		rand.reSeed(randomSeed);

		//System.out.println("GeometricLeadTime.generate()");
		
		FlexArray1D<Boolean> isVisited = new FlexArray1D<>(tStart, tEnd);
		for (int t = tStart; t < tEnd; ++t) {
			// If there is a visit in this week
			if (rand.nextUniform(0, 1) < v * getAccessibility(t)) {
				isVisited.set(t, true);
				//System.out.println("Visit at week " + t);
			} else {
				isVisited.set(t, false);
			}
		}

		realLeadTime = new FlexArray1D<>(tStart, tEnd);
		for (int t = tStart; t < tEnd; ++t) {
			// By default, there is no lead time
			realLeadTime.set(t, NO_LEAD_TIME);
			// But if it is a shipment week and we can find a future
			// visit, then set the lead time
			if (isTotalShipmentPeriod(t)) {
				for (int y = t + minLeadTime; y < tEnd; ++y) {
					if (isVisited.get(y)) {
						realLeadTime.set(t, y - t);
						//System.out.format("Lead time %d = %d\n", t, y - t);
						break;
					}
				}
			}
		}
	}

	@Override
	public double[][] getTotalLeadTimePmf(int t, int nPeriods) {

		double[][] result = new double[nPeriods][Y];
		for (int i = 0; i < nPeriods; ++i) {
			// A shipment is only possible if it is this period in
			// the cycle
			if (isTotalShipmentPeriod(t + i)) {
				// Remaining probability.
				double rp = 1;
				for (int y = minLeadTime; y < Y-1; ++y) {
					double a = getAccessibility(t + i + y); 
					if (a > 0) {
						// If the remaining probability is really small,
						// then if it is accessible, make the delivery
						// happen
						//System.out.println(rp);
						if (rp < 0.001) {
							result[i][y] = rp;
							rp = 0;
							break;
						} else {
							result[i][y] = rp * v * a;
							rp -= result[i][y];
						}
					}
					//System.out.format("%d %d %.6f\n",t,y,leadTimePmf[0][t][y]);
				}
				// Assign whatever probability remains to the last
				// period of the year
				result[i][Y - 1] = rp;
			}
		}

		return result;
	}

	@Override
	public int getTotalLeadTime(int t) {
		return realLeadTime.get(t);
	}

	@Override
	public boolean isTotalShipmentPeriod(int t) {
		return Helper.modulo(t, nPeriodsInCycle) == cycleOffset;
	}

	public double getAccessibility(int t) {
		return access[Helper.modulo(t, Y)];
	}

	/**
	 * A simple test that the GeometricLeadTime object works
	 * correctly without bugs.
	 * @param args
	 */
	public static void main(String[] args) {
		//test1();
		test2();
	}

	private static void test1() {
		int tStart = -5;
		int tEnd = 100000;
		double[] access = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
		GeometricLeadTime geo = new GeometricLeadTime(5, 3, 2,
				4, access);
		geo.generate(0, tStart, tEnd);

		double[][] pmf = geo.getTotalLeadTimePmf(0, 10);
		for (int i = 0; i < pmf.length; ++i) {
			for (int j = 0; j < pmf[i].length; ++j) {
				System.out.format("%.4f\t", pmf[i][j]);
			}
			System.out.println();
		}

		int count = 0;
		double sum = 0;
		for (int t = tStart; t < tEnd; ++t) {
			//System.out.println(t);
			int l = geo.getTotalLeadTime(t); 
			if (l != GeometricLeadTime.NO_LEAD_TIME) {
				sum += l;
				++count;
			}
		}
		System.out.println("Mean lead time = " + sum / count);
	}

	private static void test2() {
		int tStart = -5;
		int tEnd = 100000;
		double[] access = {1, 1, 1, 1, 1, 0, 0, 0.5, 0.5, 0.5};
		GeometricLeadTime geo = new GeometricLeadTime(5, 3, 2,
				4, access);
		geo.generate(0, tStart, tEnd);
		
		double[][] pmf = geo.getTotalLeadTimePmf(0, 10);
		for (int i = 0; i < pmf.length; ++i) {
			for (int j = 0; j < pmf[i].length; ++j) {
				System.out.format("%.4f\t", pmf[i][j]);
			}
			System.out.println();
		}

		int count = 0;
		double sum = 0;
		for (int t = tStart; t < tEnd; ++t) {
			//System.out.println(t);
			int l = geo.getTotalLeadTime(t); 
			if (l != GeometricLeadTime.NO_LEAD_TIME) {
				sum += l;
				++count;
			}
		}
		System.out.println("Mean lead time = " + sum / count);
	}

	@Override
	public int getNumberOfPeriodsInYear() {
		return Y;
	}

	@Override
	public int getPrimaryLeadTime(int d, int t) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSecondaryLeadTime(int r, int t) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTotalLeadTime(int r, int t) {
		// TODO Auto-generated method stub
		return 0;
	}
}
