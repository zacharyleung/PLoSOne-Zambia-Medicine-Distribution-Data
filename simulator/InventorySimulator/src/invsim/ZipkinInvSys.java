package invsim;

import java.io.PrintStream;

import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.MathException;

import d65helper.ZachRv;
import d65helper.ZachRvPoisson;

/**
 * A scenario with a single facility, deterministic lead times,
 * and stationary demand.
 * 
 * @author Zack
 *
 */

public class ZipkinInvSys extends InvSys {

	// Variables to store the system state.
	String demandDistribution;
	/** X[k] = Shipment made at the beginning of period t+k+1. */
	int[] X;
	/** Lead time. */
	int L;
	/** Current inventory level. */
	long I = 0;
	/** Number of time periods completed. */
	int T = 0;
	/** Lost sales penalty cost as a multiple of holding cost. */
	int p = 0;
	
	final static int DEMAND_MEAN = 5;
	
	RandomDataImpl rand;
	
	// Statistics.
	long D = 0;
	long U = 0;
	long H = 0;
	
	public ZipkinInvSys(String demandDistribution, int L, int p, long seed) {
		this.demandDistribution = demandDistribution;
		this.L = L;
		this.p = p;
		X = new int[L];
		rand = new RandomDataImpl();
		rand.reSeed(seed);
		if (!isPoisson() && !isGeometric())
			throw new IllegalArgumentException("Demand distribution argument is invalid!");
	}
	
	@Override
	public void simulate(int T, InvPol ip, boolean isDebug) throws Exception {
		for (int t = 0; t < T; ++t) {
			// The shipment arrives.
			I += X[0];
			for (int l = 0; l < L-1; ++l)
				X[l] = X[l+1];
			X[L-1] = 0;
			
			// Compute the next order.
			int[] Xnew = ip.computeShipments(this);
			X[L-1] = Xnew[0];
			
			if (isDebug)
				printState();
			
			// The demand arrives.
			long d = nextDemand();
			
			// Update statistics.
			D += d;
			U += Math.max(0, d - I);
			I = Math.max(0, I - d);
			H += I;
		}
		this.T += T;
	}

	@Override
	public void printState() {
		System.out.print("(I,X) = " + I);
		for (int l = 0; l < L; l++)
			System.out.print(" " + X[l]);
		System.out.println();
	}

	@Override
	public Stats printStats(PrintStream out) {
		double u = (double) U / T;
		double h = (double) H / T;
		out.println("Zipkin Inventory System");
		out.println("Demand distribution = " + demandDistribution);
		out.println("Lost sales penalty = " + p);
		out.println("Lead time = " + L);
		out.println("Number of periods = " + T);
		out.println("Mean unmet demand = " + u);
		out.println("Mean holding cost = " + h);
		out.println("Mean total cost   = " + (p * u + h));
		out.println("\n\n");
		return null;
	}
	
	@Override
	public DltInvSys getDltInvSys(int T) {
		int[][] myL = new int[1][T];
		ZachRv[][] myD = new ZachRv[1][T];
		for (int t = 0; t < T; ++t) {
			myL[0][t] = L;
			if (isPoisson())
				myD[0][t] = new ZachRvPoisson(5);
			else
				myD[0][t] = null;//new ZachRvPoisson(5);
		}
		
		return null; //new DltInvSys(p,getInitInv(),getShipments(),myD,myL);
	}

	public DetInvSys getDetInvSys(int T) throws Exception {
		int[][] myL = new int[1][T];
		long[][] myD = new long[1][T];
		double[][] dMean = new double[1][T];
		for (int t = 0; t < T; ++t) {
			myL[0][t] = L;
			myD[0][t] = nextDemand();
			dMean[0][t] = 5;
		}
		
		return new DetInvSys(p, Long.MAX_VALUE, getInitInv(),getShipments(),myD,dMean,myL);
	}

	private boolean isPoisson() {
		return demandDistribution.equals("poisson");
	}
	
	private boolean isGeometric() {
		return demandDistribution.equals("geometric");
	}

	private long nextDemand() throws Exception {
		if (isPoisson()) {
			return rand.nextPoisson(DEMAND_MEAN);
		} else { // is geometric.
			double q = 1.0 / (DEMAND_MEAN + 1);
			return rand.nextPascal(1,q);
		}
	}


    private Shipment[] getShipments() {
    	Shipment[] myX = new Shipment[L];
    	for (int t = 0; t < L; ++t) {
    		myX[t] = new Shipment(0,t+1-L,L, 
    				new Inventory(X[t], Integer.MAX_VALUE), null);
    	}
    	return myX;
    }
    
    private long[] getInitInv() {
    	long[] myI = new long[1];
    	myI[0] = I;
    	return myI;
    }

	@Override
	public double[] meanDemand(int tHistory) {
		double[] d = new double[1];
		d[0] = 5;
		return d;
	}
   

}
