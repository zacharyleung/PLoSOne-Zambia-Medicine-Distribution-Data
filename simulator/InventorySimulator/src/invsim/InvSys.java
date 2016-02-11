package invsim;

import java.io.PrintStream;

public abstract class InvSys extends AbstractInvSys {
	/** Simulate T periods of the inventory policy ip. */
	public abstract void simulate(int T, InvPol ip, boolean isDebug) throws Exception;
	
	///** Get the total demand. */
	//long getDemand();
	
	///** Get the unmet demand. */
	//long getUnmetDemand();
	
	///** Get the total holding cost. */
	//long getHoldingCost();
	
	/** Print the state. */
	public abstract void printState();
	
	public void printStats() {
		printStats(System.out);
	}
	
	public abstract Stats printStats(PrintStream out);
	
	public abstract DltInvSys getDltInvSys(int T);
	
	public abstract DetInvSys getDetInvSys(int T) throws Exception;
	
	public abstract double[] meanDemand(int tHistory);
	
	public class Stats {
		public final double holdingCost;
		public final double unmetDemand;
		public final double serviceLevel;
		public final double totalCost;
		
		Stats(double unmetDemand, double holdingCost, double serviceLevel,
				double totalCost) {
			this.unmetDemand = unmetDemand;
			this.holdingCost = holdingCost;
			this.serviceLevel = serviceLevel;
			this.totalCost = totalCost;
	}
	}
}
