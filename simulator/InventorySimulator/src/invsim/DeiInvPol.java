package invsim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import d65helper.ZachRv;
import d65helper.ZachRvEmpirical;
import d65helper.ZachRvNormal;

import gurobi.*;

public class DeiInvPol extends InvPol {
	// DEI parameters
	/** Number of periods in the planning horizon. */
	private final int T;
	private final int tangentIncrement;
	private final String tangentType;
	private final String leadTimeType;
	private final double leadTimePercentile = 0.99;
	private final double sdScale = 1;
	/**
	 * Penalty for one unit of lost sales as a multiple of the holding
	 * cost which is assumed to be 1.
	 */
	private final double unmetPenalty;
	private int count = 0;
	
	// Constants
	public static final String MEAN_DEMAND = "mean demand";
	public static final String SINGLE_PERIOD = "single period";
	public static final String MULTI_PERIOD = "multi period";
	public static final String ACTUAL = "actual";
	public static final String MEAN_LEAD_TIME = "mean";
	public static final String CONSERVATIVE = "conservative";
	
	// Private inventory system variables.
	private ZachRv[][] D;
	private int R;
	private Shipment[] pipeline;
	/** Actual (realized) lead times. */
	private int[][] L;
	private double[][][] leadTimePmf;
	private boolean[][] hasArrival;
	
	// Private Gurobi variables. 
	private GRBModel model;
	/** Define the unmet demand decision variables. */
	private GRBVar[][] U;
	/** Define the shipments decision variables. */
	private GRBVar[][] X;
	/** Define the beginning of period inventory variables. */
	private GRBVar[][] Ir;
	/** Define the end of period inventory variables. */
	private GRBVar[][] J;
	/** Beginning of period inventory variables at warehouse. */
	private GRBVar[] Iw;
	
	
	
	DeiInvPol(int horizon, double unmetPenalty, String tangentType,
			int tangentIncrement, String leadTimeType)
			throws IllegalArgumentException {
		T = horizon;
		this.unmetPenalty = unmetPenalty;
		this.tangentType = tangentType;
		this.tangentIncrement = tangentIncrement;
		this.leadTimeType = leadTimeType;
		
		if (tangentType.equals(MEAN_DEMAND)) {}
		else if (tangentType.equals(SINGLE_PERIOD)) {} 
		else if (tangentType.equals(MULTI_PERIOD)) {}
		else
			throw new IllegalArgumentException("Invalid tangentType!");
		
		if (leadTimeType.equals(ACTUAL)) {}
		else if (leadTimeType.equals(CONSERVATIVE)) {}
		else if (leadTimeType.equals(MEAN_LEAD_TIME)) {}
		else
			throw new IllegalArgumentException("Invalid leadTimeType!");		
	}
	
	@Override
	int[] computeShipments(InvSys sys) throws Exception {
		boolean isDone = false;
		while (!isDone) {
			try {
				return myComputeShipments(sys);
			} catch (IOException e) {
				System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n");
				System.out.println("-----------------------------------");
				System.out.println("Gurobi crashed...");
				System.out.println(e.toString());
				try {
					Thread.sleep(1000 * 5);
				} catch (InterruptedException ie) {}
				System.out.println("-----------------------------------");
				System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n");
			}
		}
		return null;
	}

	
    int[] myComputeShipments(InvSys sys) throws Exception {
    	DltInvSys dis = sys.getDltInvSys(T);

    	// Model
    	GRBEnv env = new GRBEnv();
    	model = new GRBModel(env);

    	D = dis.D;
    	pipeline = dis.X;
    	long[] I0 = dis.I0;
    	L = dis.L;
    	leadTimePmf = dis.leadTimePmf;
    	// Number of facilities.
    	R = dis.R;
    	int[] Xw = dis.Xw;
    	
    	// If it is not possible to send a shipment in this period,
    	// then don't run the optimization.
    	boolean shouldRun = false;
    	for (int r = 0; r < R; ++r)
    		if (L[r][0] != InvSys.NO_LEAD_TIME)
    			shouldRun = true;
    	if (!shouldRun) {
    		int[] X = new int[R];
    		return X;
    	}

    	// Define the unmet demand decision variables.
    	U = new GRBVar[R][T];
    	// Define the shipments decision variables.
    	X = new GRBVar[R][T];
    	// Define the beginning and end of period inventory.
    	Ir = new GRBVar[R][T+1];
    	J = new GRBVar[R][T];
    	for (int r = 0; r < R; ++r) {
    		for (int t = 0; t < T; ++t) {
    			U[r][t] =
    					model.addVar(0, D[r][t].getMean(), unmetPenalty, GRB.CONTINUOUS, "U_" + Integer.toString(r) + "_" + Integer.toString(t));
    			X[r][t] =
    					model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "X_" + Integer.toString(r) + "_" + Integer.toString(t));
    			Ir[r][t] =
    					model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + Integer.toString(r) + "_" + Integer.toString(t));
    			J[r][t] =
    					model.addVar(0, GRB.INFINITY, 1, GRB.CONTINUOUS, "J_" + Integer.toString(r) + "_" + Integer.toString(t));
    		}
    		// Create these to make coding a little easier though they
    		// are not used.
    		Ir[r][T] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + Integer.toString(r) + "_T");
    	}

    	Iw = new GRBVar[T+1];
    	for (int t = 0; t < T+1; ++t) {
    		Iw[t] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "Iw_" + t);
    	}

    	// Update model to integrate new variables
    	model.update();

    	// Warehouse initial inventory constraint
    	model.addConstr(Iw[0], GRB.EQUAL, dis.Iw0, "Iw0");
    	
    	// Warehouse inventory balance constraint
    	for (int t = 0; t < T; ++t) {
    		GRBLinExpr inv = new GRBLinExpr();
    		inv.addTerm(1, Iw[t]);
    		for (int r = 0; r < R; ++r)
    			inv.addTerm(-1, X[r][t]);
    		inv.addConstant(Xw[t+1]);
    		model.addConstr(inv, GRB.EQUAL, Iw[t+1], "Iw_" + t);
    	}
    	
    	// For each retailer...
    	for (int r = 0; r < R; ++r) {
       		// Set initial inventory constraint
    		model.addConstr(Ir[r][0], GRB.EQUAL, I0[r], "I0_" + r);
    	}

    	addLeadTimeConstrs();
    	addTangents();
  	
    	// Solve the model.
    	model.optimize();
    	
    	// For debugging
    	//model.write("my" + count++ + ".lp");
    	//model.write("my.lp");
    	//printSolution();
    	
    	int[] Q = new int[R];
    	for (int r = 0; r < R; ++r)
    		Q[r] = (int) Math.floor(X[r][0].get(GRB.DoubleAttr.X));
    	
    	model.dispose();
    	env.dispose();
    	
    	return Q;
    }
    
    /**
     * Add to the model constraints related to the lead times, in
     * particular when pipeline shipments and new shipments arrive.
     * @throws Exception
     */
    private void addLeadTimeConstrs() throws Exception {
    	int[][] myL = new int[R][T];
    	hasArrival = new boolean[R][T];
    	
    	for (int r = 0; r < R; ++r) {
        	// Do something special for the first shipment.
			boolean isFirst = true;
    		for (int t = 0; t < T; ++t) {
    			double[] pmf = leadTimePmf[r][t];
    			
    			if (pmf == null) {
					myL[r][t] = InvSys.NO_LEAD_TIME;
    			}
    			else {
    				isFirst = false;
    				ZachRvEmpirical emp = new ZachRvEmpirical(pmf);
    				if (leadTimeType.equals(ACTUAL)) {
    					myL[r][t] = L[r][t];
    				} else if (leadTimeType.equals(CONSERVATIVE)) {
    					if (isFirst) {
    						// Set the min lead time for the first shipment
    						myL[r][t] = emp.getMin();
    					} else {
    						// Set a given percentile (e.g. 0.98) for the second
    						// shipment
    						myL[r][t] = (int)
    								emp.inverseCumulativeProbability(leadTimePercentile);
    					}
    				} else if (leadTimeType.equals(MEAN_LEAD_TIME)) {
    					myL[r][t] = (int) Math.round(emp.getMean()); 
    				}
    			}
    		}
    	}
    	    	
    	// Set beginning period inventory constraints.
    	GRBLinExpr[][] invStart = new GRBLinExpr[R][T];

    	// For each retailer...
    	for (int r = 0; r < R; ++r) {
    		// For each time period...
    		for (int t = 0; t < T; ++t) {
    			// Set up partially starting period inventory constraints.
    			invStart[r][t] = new GRBLinExpr();
    			invStart[r][t].addTerm(1, J[r][t]);

    			// Set ending period inventory constraints.
    			GRBLinExpr invEnd = new GRBLinExpr();
    			invEnd.addTerm(1, Ir[r][t]);
    			invEnd.addTerm(1, U[r][t]);
    			invEnd.addConstant(-D[r][t].getMean());
    			model.addConstr(invEnd, GRB.EQUAL, J[r][t], "J_" + Integer.toString(r) + "_" + Integer.toString(t));
    		}

    		// Set shipment arrivals.
    		for (int t = 0; t < T; ++t) {
    			if (myL[r][t] != DltInvSys.NO_LEAD_TIME) {
    				// When the shipment will arrive.
    				int u = t + myL[r][t];
    				//System.out.println("u = " + u);
    				//System.out.println("T = " + T);
    				if (u < T) {
    					// Note that invStart[r][u-1] corresponds to the constraint
    					// that sets I[r][u] = J[r][u-1] + X[r][t].
    					invStart[r][u-1].addTerm(1, X[r][t]);
    					hasArrival[r][u] = true;
    				}
    			} else {
    				model.addConstr(X[r][t], GRB.LESS_EQUAL, 0, 
    						"X_" + r + "_" + t);
    			}
    		}
    	}
    	
    	// Set pipeline inventory arrivals.
    	for (int i = 0; i < pipeline.length; ++i) {
    		// Lead time of shipment pipeline[i].
    		double[] pmf = pipeline[i].leadTimePmf;
    		int l = -5; // This will cause a runtime error!
    		if (leadTimeType.equals(ACTUAL)) {
    			l = pipeline[i].leadTime;
    		} else if (leadTimeType.equals(CONSERVATIVE)) {
    			// If the shipment hasn't yet arrived, the earliest it
    			// could arrive is the next period.
    			for (l = 1 - pipeline[i].periodSent; l < pmf.length; ++l)
    				if (pmf[l] > 0)
    					break;
    		} else if (leadTimeType.equals(MEAN_LEAD_TIME)) {
    			ZachRvEmpirical emp = new ZachRvEmpirical(pmf);
    			l = (int) Math.ceil(emp.getConditionalMean(1 - pipeline[i].periodSent));
    		}
    		int r = pipeline[i].destination;
    		int t = pipeline[i].periodSent + l;
   			// Note that invStart[r][t-1] corresponds to the constraint
   			// that sets I[r][t] = J[r][t-1] + pipeline quantity.
    		invStart[r][t-1].addConstant(pipeline[i].quantity);
   			hasArrival[r][t] = true;
    	}

    	// Now we add the initial inventory and inventory starting constraints.
    	for (int r = 0; r < R; ++r) {
    		for (int t = 0; t < T; ++t) {
    			model.addConstr(invStart[r][t], GRB.EQUAL, Ir[r][t+1], "I_" + Integer.toString(r) + "_" + Integer.toString(t));
    		}
    	}    	    


    }

    private void addTangents() throws Exception {
    	double p = unmetPenalty;
    	
    	if (tangentType.equals(MEAN_DEMAND)) {
    		for (int r = 0; r < R; ++r) {
    			for (int t = 0; t < T; ++t) {
    				GRBLinExpr inv = new GRBLinExpr();
    				inv.addTerm(-1, Ir[r][t]);
    				inv.addConstant(D[r][t].getMean());
    				model.addConstr(inv, GRB.LESS_EQUAL, U[r][t],
    						"U_" + r + "_" + t);
    			}
    		}
    	} else if (tangentType.equals(SINGLE_PERIOD)) {
    		for (int r = 0; r < R; ++r) {
    			for (int t = 0; t < T; ++t) {
    				myAddTangents(r,t,t+1);
    			}
    		}
    	} else if (tangentType.equals(MULTI_PERIOD)) {
    		for (int r = 0; r < R; ++r) {
    			int t1 = 0; // Shipment arrival
    			int t2 = 1; // Next shipment arrival
    			while (t2 <= T) {
    				// Find the next shipment arrival, or the end of
    				// the horizon.
    				while (t2 <= T-1) {
    					if (hasArrival[r][t2])
    						break;
    					++t2;
    				}
    					
    				myAddTangents(r, t1,t2);
    				
    				// Update t1 and t2.
    				t1 = t2;
    				t2 = t1 + 1;
    			}
    		}
    	}

    }
    
    /**
     * Add tangents for U_[t1,t2) >= a + b * I_t1. 
     * @param t1
     * @param t2
     */
    private void myAddTangents(int r, int t1, int t2) throws Exception {
    	// Aggregate demand in the periods [t,u).
		// The worst case if demand is perfectly correlated.
		// I'm now trying the case when demand is independent.
		double mean = 0;
		double sumvar = 0;
		for (int k = t1; k < t2; ++k) {
			mean += D[r][k].getMean();
			sumvar += Math.pow(D[r][k].getStandardDeviation(), 2);
		}
		double sd = Math.sqrt(sumvar);
		// Worst case
		double cv = D[r][t2-1].getStandardDeviation() / D[r][t2-1].getMean();
		cv = 0.1; // Use this as a parameter.
		ZachRv demand = new ZachRvNormal(mean, sdScale * sd);
		
		//System.out.format("Adding constraints for [%d,%d)\n",t1,t2);
		//System.out.format("mean = %.1f, sd = %.1f\n",mean,sd);
		// Set up tangent constraints.
		// At the inventory level i, u is the expected unmet demand.
		double u = mean;
		int iPast = 0;
		double uPast = u;
		// If we start at 0, we may run into numerical difficulties.
		for (int i = (int) mean; ; i += tangentIncrement) {
			u = demand.getUnmetDemand(i);
			
			// Compute the coefficients.
			double b = (u - uPast) / (i - iPast);
			double a = u - b * i;
			
			// Add the constraint -U_[t,u) + a + b * I_t <= 0.
			GRBLinExpr inv = new GRBLinExpr();
			for (int tau = t1; tau < t2; ++tau)
				inv.addTerm(-1, U[r][tau]);
			inv.addConstant(a);
			inv.addTerm(b, Ir[r][t1]);
			model.addConstr(inv, GRB.LESS_EQUAL, 0,
					"U_" + Integer.toString(r) + "_" + Integer.toString(t1) + "_" + Integer.toString(i));

			// To debug
//				System.out.format("iPast = %d\tuPast = %.4e\n", iPast, uPast);
//				System.out.format("i     = %d\tu     = %.4e\n", i, u);
//				System.out.format("a + b * iPast = %.4e\n", a + b * iPast);
//				System.out.format("uPast         = %.4e\n", uPast);
//				System.out.format("a + b * i     = %.4e\n", a + b * i);
//				System.out.format("u             = %.4e\n", u);
//				System.out.format("i = %6d\tGradient = %.4e\tIntercept = %.8e\n",i,b,a);
//				Helper.waitForUser();
			
			// Update past (i,u) pair
			iPast = i;
			uPast = u;
			
			if (-b < 1.0 / unmetPenalty)
				break;
		}
    }
    
	@Override
	void print(PrintStream out) {
		out.println("# DEI inventory policy");
		out.println("# T                    = " + T);
		out.println("# Unmet demand penalty = " + unmetPenalty);
		out.println("# Tangent type         = " + tangentType);
		out.println("# Tangent increment    = " + tangentIncrement);
		out.println("# Lead time type       = " + leadTimeType);
		out.println("# Lead time percentile = " + leadTimePercentile);
		out.println("#");
	}
	
	void printSolution() {
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream("dei-sol-" + count + ".txt"));

			out.println("Week\tHasArr\tShip\tUnmet\tInv\tInvEnd");
			for (int t = 0; t < T; ++t) {
				out.format("%d\t%b\t%.1f\t%.1f\t%.1f\t%.1f\n",t,
						hasArrival[0][t],
						X[0][t].get(GRB.DoubleAttr.X),
						U[0][t].get(GRB.DoubleAttr.X),
						Ir[0][t].get(GRB.DoubleAttr.X),
						J[0][t].get(GRB.DoubleAttr.X));
			}
			out.close();
		} catch (Exception e) {}
	}
	
}
