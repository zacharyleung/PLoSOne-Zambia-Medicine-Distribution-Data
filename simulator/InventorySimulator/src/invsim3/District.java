package invsim3;

import d65helper.FlexArray1D;

public class District extends Facility {
	
	public void generate(int randomSeed) {
		super.generate(randomSeed);
		
		int tStart = simulator.getStartPeriod();
		int tEnd = simulator.getEndPeriod();
		int Y = simulator.getNumberOfPeriodsInYear();

		// Generate two extra years of order info for districts
		orders = new FlexArray1D<>(tStart - 2 * Y, tEnd);
		for (int t = tStart - 2 * Y; t < tEnd; ++t) {
			orders.set(t, (long) 0);
		}
	}
}
