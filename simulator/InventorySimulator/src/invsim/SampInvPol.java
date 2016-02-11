/**
 * Sampling-based inventory policy. 
 */

package invsim;

import java.io.PrintStream;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public class SampInvPol extends InvPol {
	/** Number of periods in the planning horizon. */ 
	final int T;
	/** Number of samples to draw to compute shipments. */
	final int S;
	/** The first F shipments are fixed across scenarios. */
	final int F;

	/**
	 * Constructor.
	 * @param T Number of periods in horizon.
	 * @param S Number of samples to draw to compute shipments.
	 */
	SampInvPol(int T, int S, int F) {
		this.T = T;
		this.S = S;
		this.F = F;
	}

	@Override
	int[] computeShipments(InvSys sys) throws Exception {
		boolean isDone = false;
		while (!isDone) {
			try {
				return myComputeShipments(sys);
			} catch (Exception e) {
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
	
	private int[] myComputeShipments(InvSys sys) throws Exception {
		DetInvSys[] dis = new DetInvSys[S];
		for (int s = 0; s < S; ++s) {
			dis[s] = sys.getDetInvSys(T);
		}
		// Number of facilities.
		int R = dis[0].R;
		
		//dis[0].print();
		
		// Model
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		// Define the unmet demand decision variables.
		GRBVar[][][] U = new GRBVar[S][R][T];
		// Define the shipments decision variables.
		GRBVar[][][] X = new GRBVar[S][R][T];
		// Define the beginning and end of period inventory.
		GRBVar[][][] I = new GRBVar[S][R][T+1];
		GRBVar[][][] J = new GRBVar[S][R][T];

		// Create the variables for each realization.
		for (int s = 0; s < S; ++s) {
			double p = dis[s].p;
			long[][] D = dis[s].D;
			Shipment[] pipeline = dis[s].X;
			long[] I0 = dis[s].I0;
			int[][] L = dis[s].L;

			for (int r = 0; r < R; ++r) {
				for (int t = 0; t < T; ++t) {
					U[s][r][t] =
							model.addVar(0, D[r][t], p, GRB.CONTINUOUS, "U_" + Integer.toString(r) + "_" + Integer.toString(t));
					X[s][r][t] =
							model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "X_" + Integer.toString(r) + "_" + Integer.toString(t));
					I[s][r][t] =
							model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + Integer.toString(r) + "_" + Integer.toString(t));
					J[s][r][t] =
							model.addVar(0, GRB.INFINITY, 1, GRB.CONTINUOUS, "J_" + Integer.toString(r) + "_" + Integer.toString(t));
				}
				// Create these to make coding a little easier though they
				// are not used.
				I[s][r][T] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + Integer.toString(r) + "_T");
			}

			// Update model to integrate new variables
			model.update();

			// Set initial inventory constraints.
			GRBLinExpr[] invInit = new GRBLinExpr[R];    	    
			// Set beginning period inventory constraints.
			GRBLinExpr[][] invStart = new GRBLinExpr[R][T];

			// For each retailer...
			for (int r = 0; r < R; ++r) {
				// Set initial inventory constraint
				invInit[r] = new GRBLinExpr();
				invInit[r].addTerm(1, I[s][r][0]);
				invInit[r].addConstant(-I0[r]);

				// For each time period...
				for (int t = 0; t < T; ++t) {
					// Set up partially starting period inventory constraints.
					invStart[r][t] = new GRBLinExpr();
					invStart[r][t].addTerm(1, J[s][r][t]);

					// Set ending period inventory constraints.
					GRBLinExpr invEnd = new GRBLinExpr();
					invEnd.addTerm(1, I[s][r][t]);
					invEnd.addTerm(1, U[s][r][t]);
					invEnd.addConstant(-D[r][t]);
					model.addConstr(invEnd, GRB.EQUAL, J[s][r][t],
							"J_" + Integer.toString(s) + "_" + Integer.toString(r) + "_" + Integer.toString(t));

					// Set up tangent constraints.
					GRBLinExpr inv = new GRBLinExpr();
					inv.addTerm(-1, I[s][r][t]);
					inv.addConstant(D[r][t]);
					model.addConstr(inv, GRB.LESS_EQUAL, U[s][r][t],
							"U_" + Integer.toString(s) + "_" + Integer.toString(r) + "_" + Integer.toString(t));
				}

				// Set shipment arrivals.
				for (int t = 0; t < T; ++t) {
					// If it is possible to send a shipment during
					// this period.
					if (L[r][t] == DetInvSys.NO_LEAD_TIME) {
						model.addConstr(X[s][r][t], GRB.LESS_EQUAL, 0,
								"X_" + s + "_" + r + "_" + t);
					}
					else {
						// When the shipment will arrive.
						int u = t + L[r][t];
						//System.out.println("u = " + u);
						//System.out.println("T = " + T);
						if (u < T) {
							// Note that invStart[r][u-1] corresponds to the constraint
							// that sets I[r][u] = J[r][u-1] + X[r][t].
							invStart[r][u-1].addTerm(1, X[s][r][t]);
						}
					}
				}
			}

			// Set pipeline inventory arrivals.
			for (int i = 0; i < pipeline.length; ++i) {
				int r = pipeline[i].destination;
				int t = pipeline[i].periodSent + pipeline[i].leadTime;
				if (t == 0) {
					// Make the quantity arrive now.
					invInit[r].addConstant(-pipeline[i].quantity);
				} else {
					// Note that invStart[r][t-1] corresponds to the constraint
					// that sets I[r][t] = J[r][t-1] + pipeline quantity.
					invStart[r][t-1].addConstant(pipeline[i].quantity);
				}
			}

			// Now we add the initial inventory and inventory starting constraints.
			for (int r = 0; r < R; ++r) {
				model.addConstr(invInit[r], GRB.EQUAL, 0,
						"I0_" + Integer.toString(s) + "_" + Integer.toString(r));
				for (int t = 0; t < T; ++t) {
					model.addConstr(invStart[r][t], GRB.EQUAL, I[s][r][t+1],
							"I_" + Integer.toString(s) + "_" + Integer.toString(r) + "_" + Integer.toString(t));
				}
			}

			if (s != 0) {
				for (int r = 0; r < R; ++r) {
					for (int t = 0; t < F; ++t) {
						// Add constraint so all first-stage shipments are equal.
						model.addConstr(X[s][r][t], GRB.EQUAL, X[0][r][t],
								"Xfirst" + Integer.toString(s) + "_" + Integer.toString(r) + "_" + Integer.toString(t));
					}
				}
			}
		}

		// Solve the model.
		model.optimize();
		//model.write("my.lp");

		int[] Q = new int[R];
		for (int r = 0; r < R; ++r)
			Q[r] = (int) Math.round(X[0][r][0].get(GRB.DoubleAttr.X));

		//    	    // Print the decision variables.
		//    	    System.out.print("X =");
		//    	    for (int t = 0; t < T; ++t)
		//    	    	System.out.print(" " + X[t].get(GRB.DoubleAttr.X));
		//    	    System.out.println();
		//
		//    	    System.out.print("I =");
		//    	    for (int t = 0; t < T; ++t)
		//    	    	System.out.print(" " + I[t].get(GRB.DoubleAttr.X));
		//    	    System.out.println();
		//    	    
		//    	    System.out.print("J =");
		//    	    for (int t = 0; t < T; ++t)
		//    	    	System.out.print(" " + J[t].get(GRB.DoubleAttr.X));
		//    	    System.out.println();
		//    	    
		//    	    System.out.print("U =");
		//    	    for (int t = 0; t < T; ++t)
		//    	    	System.out.print(" " + U[t].get(GRB.DoubleAttr.X));
		//    	    System.out.println();

		return Q;
	}

	@Override
	void print(PrintStream out) {
		out.println("Sampling Inventory Policy");
		out.println("Number of samples = " + S);
		out.println("Number of periods fixed across scenarios = " + F);
		out.println("Horizon = " + T);
		out.println("\n\n");
	}

}
