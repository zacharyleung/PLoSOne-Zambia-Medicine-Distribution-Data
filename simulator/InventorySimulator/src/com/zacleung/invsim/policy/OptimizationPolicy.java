package com.zacleung.invsim.policy;

import invsim3.Forecast;
import invsim3.LeadTime;
import invsim3.NationalWarehouse;
import invsim3.Retailer;
import invsim3.Shipment;
import invsim3.Simulator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


import d65helper.FlexArray1D;
import d65helper.FlexArray2D;
import d65helper.ZachRv;
import d65helper.ZachRvConstantInteger;
import d65helper.ZachRvEmpirical;
import d65helper.ZachRvNormal;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

/**
 * How OptimizationPolicy works.
 * 
 * This code was based on the DeiInvPol code.  In that code, the time
 * horizon was always assumed to be [0,T).  The reason for making this
 * design decision was that when we refer to period t, we want to just
 * write the variable I[t].  If we used the horizon
 * [tCurrent, tCurrent + T), then I[] can be a huge array if tCurrent
 * is very large.
 * 
 * However, in this code, we use the horizon [tCurrent, tCurrent + T).
 * By using FlexArray objects, I[] can be an array of size T. 
 * 
 * 
 * @author zacleung
 *
 */
public class OptimizationPolicy extends XDockPolicy {
	// DEI parameters
	/** Number of periods in the planning horizon. */
	private final int T;
	private final int tangentIncrement;
	private final TangentType tangentType;
	private final LeadTimeType leadTimeType;
	private final double leadTimePercentile;
	private final double sdScale = 1;
	/**
	 * Penalty for one unit of lost sales as a multiple of the holding
	 * cost which is assumed to be 1.
	 */
	private final double unmetPenalty;
	private int count = 0;

	public static final int NO_LEAD_TIME = Integer.MIN_VALUE;

	// Private inventory system variables.
	private FlexArray2D<ZachRv> D;
	private int R;
	private FlexArray2D<Boolean> hasArrival;
	private NationalWarehouse warehouse;
	private Retailer[] retailers;
	private LeadTime leadTime;
	private int tCurrent;
	private int tStart;
	private int tEnd;

	// Private Gurobi variables. 
	private GRBModel model;
	/** Define the unmet demand decision variables. */
	private FlexArray2D<GRBVar> U;
	/** Define the shipments decision variables. */
	private FlexArray2D<GRBVar> X;
	/** Define the beginning of period inventory variables. */
	private FlexArray2D<GRBVar> I;
	/** Define the end of period inventory variables. */
	private FlexArray2D<GRBVar> J;
	/** Beginning of period inventory variables at warehouse. */
	private FlexArray1D<GRBVar> Iw;

	OptimizationPolicy(int horizon, double unmetPenalty,
			TangentType tangentType, int tangentIncrement,
			LeadTimeType leadTimeType, double leadTimePercentile)
					throws IllegalArgumentException {
		T = horizon;
		this.unmetPenalty = unmetPenalty;
		this.tangentType = tangentType;
		this.tangentIncrement = tangentIncrement;
		this.leadTimeType = leadTimeType;
		this.leadTimePercentile = leadTimePercentile;
	}

	@Override
	public int[] computeShipments(Simulator simulator) throws Exception {
		boolean isDone = false;
		while (!isDone) {
			try {
				return myComputeShipments(simulator);
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

	int[] myComputeShipments(Simulator simulator) throws Exception {
		// Model
		GRBEnv env = new GRBEnv();
		model = new GRBModel(env);

		warehouse = simulator.national;
		retailers = simulator.retailers;
		leadTime = simulator.leadTime;
		tCurrent = simulator.getCurrentTimePeriod();
		tStart = tCurrent;
		tEnd = tCurrent + T;

		//System.out.println("OptimizationPolicy.myComputeShipments()");
		//System.out.println("tCurrent = " + tCurrent);

		R = simulator.R;
		D = new FlexArray2D<>(0, R, tStart, tEnd);
		for (int r = 0; r < R; ++r) {
			Forecast f = retailers[r].demand.getForecast(tCurrent, T);
			for (int t = tStart; t < tEnd; ++t) {
				int k = t - tStart;
				D.set(r, t, new ZachRvNormal(f.mean[k], Math.sqrt(f.var[k])));
			}
		}

		long[] I0 = new long[R];
		for (int r = 0; r < R; ++r) {
			I0[r] = retailers[r].getInventoryLevel();
		}

		FlexArray1D<Integer> Xw = new FlexArray1D<>(tStart, tEnd + 1);
		for (int t = tStart; t < tEnd + 1; ++t) {
			Xw.set(t, warehouse.getShipmentSchedule(t, 1)[0]);
		}

		// If it is not possible to send a shipment in this period,
		// then don't run the optimization.
		boolean shouldRun = false;
		for (int r = 0; r < R; ++r) {
			if (leadTime.isTotalShipmentPeriod(r, tCurrent)) {
				shouldRun = true;
			}
		}
		if (!shouldRun) {
			int[] result = new int[R];
			return result;
		}

		// Define the unmet demand decision variables.
		U = new FlexArray2D<>(0, R, tCurrent, tEnd);
		// Define the shipments decision variables.
		X = new FlexArray2D<>(0, R, tCurrent, tEnd);
		// Define the beginning and end of period inventory.
		I = new FlexArray2D<>(0, R, tCurrent, tEnd + 1);
		J = new FlexArray2D<>(0, R, tCurrent, tEnd);
		for (int r = 0; r < R; ++r) {
			double meanAccess = leadTime.getMeanAccessibility(r);
			for (int t = tStart; t < tEnd; ++t) {
				double hc;
				// Upper bound used to be D.get(r, t).getMean()
				U.set(r, t, 
						model.addVar(0, GRB.INFINITY, unmetPenalty, GRB.CONTINUOUS, "U_" + r + "_" + t));
				X.set(r , t,
						model.addVar(0, GRB.INFINITY, 0.01, GRB.CONTINUOUS, "X_" + r + "_" + t));
				I.set(r, t, 
						model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + r + "_" + t));

				// Use mean accessibility for holding cost weight.
				hc = meanAccess;
				// Use this period accessibility for holding cost weight.
				hc = leadTime.getAccessibility(r, t);
				J.set(r, t,
						model.addVar(0, GRB.INFINITY, hc, GRB.CONTINUOUS, "J_" + r + "_" + t));
			}
			// Create these to make coding a little easier though they
			// are not used.
			I.set(r, tEnd,
					model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "I_" + r + "_" + tEnd));
		}

		Iw = new FlexArray1D<>(tStart, tEnd + 1);
		for (int t = tStart; t < tEnd + 1; ++t) {
			Iw.set(t, model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "Iw_" + t));
		}

		// Update model to integrate new variables
		model.update();

		// Bounds on the unmet demand
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				double d;
				if (tangentType == TangentType.ACTUAL) {
					d = retailers[r].getDemand(t);
				} else {
					d = D.get(r, t).getMean();
				}
				model.addConstr(U.get(r, t), GRB.LESS_EQUAL, d, "Uub_" + r + "_" + t);
			}
		}

		// Warehouse initial inventory constraint
		model.addConstr(Iw.get(tStart), GRB.EQUAL, warehouse.getInventoryLevel(), "Iw0");

		// Warehouse inventory balance constraint
		// Iw[t+1] = Iw[t] - sum_r X[r][t] + Xw[t+1]
		for (int t = tStart; t < tEnd; ++t) {
			GRBLinExpr inv = new GRBLinExpr();
			inv.addTerm(1, Iw.get(t));
			for (int r = 0; r < R; ++r)
				inv.addTerm(-1, X.get(r, t));
			inv.addConstant(Xw.get(t+1));
			model.addConstr(inv, GRB.EQUAL, Iw.get(t + 1), "Iw_" + t);
		}

		// Warehouse can't ship more than inventory constraint
		// Iw[t] - sum_r X[r][t] >= 0
		for (int t = tStart; t < tEnd; ++t) {
			GRBLinExpr inv = new GRBLinExpr();
			inv.addTerm(1, Iw.get(t));
			for (int r = 0; r < R; ++r) {
				inv.addTerm(-1, X.get(r, t));
			}
			model.addConstr(inv, GRB.GREATER_EQUAL, 0, "Jw_" + t);
		}

		// For each retailer...
		for (int r = 0; r < R; ++r) {
			// Set initial inventory constraint
			model.addConstr(I.get(r, tStart), GRB.EQUAL, I0[r], "I0_" + r);
		}

		addLeadTimeConstrs();
		addTangents();

		// Solve the model.
		model.optimize();

		// For debugging
		//model.write("my" + tStart + ".lp");
		//model.write("my.lp");
		//printSolution();

		int optimstatus = model.get(GRB.IntAttr.Status);
		if (optimstatus == GRB.Status.INFEASIBLE) {
			System.out.println("Model is infeasible");

			model.write("my.lp");
			// Compute and write out IIS
			model.computeIIS();
			model.write("model.ilp");
			return null;
		}

		int[] Q = new int[R];
		for (int r = 0; r < R; ++r)
			Q[r] = (int) Math.floor(X.get(r, tStart).get(GRB.DoubleAttr.X));

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
		FlexArray2D<Integer> myL = new FlexArray2D<>(0, R, tStart, tEnd);

		// Create the hasArrival FlexArray and initialize it to zero
		hasArrival = new FlexArray2D<>(0, R, tStart, tEnd);
		for (int r = 0; r < R; ++r)
			for (int t = tStart; t < tEnd; ++t)
				hasArrival.set(r, t, false);

		for (int r = 0; r < R; ++r) {
			// Do something special for the first shipment.
			boolean isFirst = true;
			for (int t = tStart; t < tEnd; ++t) {
				double[] pmf = leadTime.getTotalLeadTimePmf(r, t);

				//System.out.println("OptimizationPolicy.addLeadTimeConstrs()");
				//System.out.println("t = " + (tCurrent + k));
				// If this is a shipment period
				if (leadTime.isTotalShipmentPeriod(r, t)) {
					ZachRvEmpirical emp = new ZachRvEmpirical(pmf);
					int l = -5;

					switch(leadTimeType) {
					case ACTUAL:
						l = leadTime.getTotalLeadTime(r, t);
						break;
					case MEAN_LEAD_TIME:
						l = (int) emp.getMean();
						break;
					case CONSERVATIVE:
						if (isFirst) {
							// Set the min lead time for the first shipment
							//l = emp.getMin();
							l = (int) emp.inverseCumulativeProbability(1 - leadTimePercentile);
							isFirst = false;
						} else {
							// Set a given percentile (e.g. 0.98) for the second
							// shipment
							l = (int) emp.inverseCumulativeProbability(leadTimePercentile);
						}
					}
					myL.set(r, t, l);
					//					System.out.println("OptimizationPolicy.addLeadTimeConstrs()");
					//					System.out.format("myL[%d][%d] = %d\n", r, t, l);
				}
				else { // Otherwise indicate there is no lead time
					myL.set(r, t, NO_LEAD_TIME);
				}
			}
		}

		// Set beginning period inventory constraints.
		FlexArray2D<GRBLinExpr> invStart = new FlexArray2D<>(0, R, tStart, tEnd);

		// For each retailer...
		for (int r = 0; r < R; ++r) {
			// For each time period...
			for (int t = tStart; t < tEnd; ++t) {
				// Set up partially starting period inventory constraints.
				invStart.set(r, t, new GRBLinExpr());
				invStart.get(r, t).addTerm(1, J.get(r, t));

				// Set ending period inventory constraints.
				GRBLinExpr invEnd = new GRBLinExpr();
				invEnd.addTerm(1, I.get(r, t));
				invEnd.addTerm(1, U.get(r, t));

				if (tangentType == TangentType.ACTUAL) {
					invEnd.addConstant(-retailers[r].getDemand(t));	
				} else {
					invEnd.addConstant(-D.get(r,t).getMean());
				}
				model.addConstr(invEnd, GRB.EQUAL, J.get(r, t), "J_" + r + "_" + t);
			}

			// Set shipment arrivals.
			for (int t = tStart; t < tEnd; ++t) {
				if (myL.get(r, t) != NO_LEAD_TIME) {
					// When the shipment will arrive.
					int u = t + myL.get(r, t);
					//System.out.println("u = " + u);
					//System.out.println("T = " + T);
					if (u < tEnd) {
						// Note that invStart[r][u-1] corresponds to the constraint
						// that sets I[r][u] = J[r][u-1] + X[r][t].
						invStart.get(r, u - 1).addTerm(1, X.get(r, t));
						hasArrival.set(r, u, true);
						//						System.out.println("OptimizationPolicy.addLeadTimeConstrs()");
						//						System.out.format("Shipment from period %d arrives in peirod %d", t, u);
						//						System.out.println();
					}
				} else {
					model.addConstr(X.get(r, t), GRB.LESS_EQUAL, 0, 
							"X_" + r + "_" + t);
				}
			}
		}

		// Set pipeline inventory arrivals.
		for (int r = 0; r < R; ++r) {
			for (Shipment shipment : retailers[r].getShipments()) {
				// Lead time of shipment pipeline[i].
				double[] pmf = shipment.leadTimePmf;
				ZachRvEmpirical emp = new ZachRvEmpirical(pmf);
				int minLeadTime = tStart + 1 - shipment.periodSent;
				ZachRvEmpirical empCond = emp.getConditionalRv(minLeadTime);
				
				int l = NO_LEAD_TIME; // This will cause a runtime error!

				switch(leadTimeType) {
				case ACTUAL:
					l = shipment.leadTime;
					break;
				case MEAN_LEAD_TIME:
					l = (int) Math.ceil(empCond.getMean());
					break;
				case CONSERVATIVE:
					// OLD CODE
					// If the shipment hasn't yet arrived, the earliest it
					// could arrive is the next period.
//					for (l = tStart + 1 - shipment.periodSent; l < pmf.length; ++l) {
//						if (pmf[l] > 0) {
//							break;
//						}
//					}
					
					// First, condition the lead time distribution based on
					// the shipment not yet arriving
					
					// Then, get the (1 - alpha)-fractile of this conditional
					// distribution
					l = (int) empCond.inverseCumulativeProbability(1 - leadTimePercentile);
				}


				int t = shipment.periodSent + l;


				if (t <= tCurrent) {
					t = tCurrent + 1;
				}

				//				System.out.println("OptimizationPolicy.addLeadTimeConstrs()");
				//				System.out.println(shipment);
				//				System.out.format("l = %d", l);
				//				System.out.println();
				//				System.out.format("arrival = %d", t);
				//				System.out.println();

				// Note that invStart[r][t-1] corresponds to the constraint
				// that sets I[r][t] = J[r][t-1] + pipeline quantity.
				// Don't add the shipment arriving if it happens beyond
				// the time horizon.
				if (t < tEnd) {
					invStart.get(r, t - 1).addConstant(shipment.quantity);
					hasArrival.set(r, t, true);
				}
			}
		}

		// Now we add the initial inventory and inventory starting constraints.
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				model.addConstr(invStart.get(r, t), GRB.EQUAL,
						I.get(r, t + 1),
						"I_" + r + "_" + t);
			}
		}    	    


	}

	private void addTangents() throws Exception {
		double p = unmetPenalty;

		switch(tangentType) {
		case ACTUAL:
			for (int r = 0; r < R; ++r) {
				for (int t = tStart; t < tEnd; ++t) {
					GRBLinExpr inv = new GRBLinExpr();

					int actualDemand = retailers[r].demand.getDemand(t, 1)[0];

					// Add constraint D[r][t] - I[r][t] <= U[r][t]
					inv.addTerm(-1, I.get(r, t));
					inv.addConstant(actualDemand);
					model.addConstr(inv, GRB.LESS_EQUAL, U.get(r, t),
							"U_" + r + "_" + t);

					// Add constraint U[r][t] <= D[r][t]
					model.addConstr(U.get(r, t), GRB.LESS_EQUAL, actualDemand,
							"U_" + r + "_" + t);

				}
			}
			break;

		case MEAN_DEMAND:
			for (int r = 0; r < R; ++r) {
				for (int t = tStart; t < tEnd; ++t) {
					GRBLinExpr inv = new GRBLinExpr();
					inv.addTerm(-1, I.get(r, t));
					inv.addConstant(D.get(r, t).getMean());
					model.addConstr(inv, GRB.LESS_EQUAL, U.get(r, t),
							"U_" + r + "_" + t);
				}
			}
			break;

		case SINGLE_PERIOD:			
			for (int r = 0; r < R; ++r) {
				for (int t = tStart; t < tEnd; ++t) {
					myAddTangents(r, t, t+1);
				}
			}
			break;

		case MULTI_PERIOD:
			for (int r = 0; r < R; ++r) {
				int t1 = tStart; // Shipment arrival
				int t2; // Next shipment arrival
				while (t1 < tEnd) {
					t2 = t1 + 1;

					// Find the next shipment arrival, or the end of
					// the horizon.
					while (true) {
						if (t2 == tEnd) {
							break;
						} else if (hasArrival.get(r, t2)) {
							break;
						}
						++t2;
					}

					myAddTangents(r, t1, t2);

					// Update t1
					t1 = t2;
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
			mean += D.get(r, k).getMean();
			sumvar += Math.pow(D.get(r, k).getStandardDeviation(), 2);
		}
		double sd = Math.sqrt(sumvar);

		// Worst case
		double cv = D.get(r, t2 - 1).getStandardDeviation() / D.get(r, t2 - 1).getMean();
		cv = 0.1; // Use this as a parameter.
		ZachRv demand;
		if (sd < 0.1) {
			int demandInt = (int) Math.floor(mean);
			demand = new ZachRvConstantInteger(demandInt);
		} else {
			demand = new ZachRvNormal(mean, sdScale * sd);
		}

		//		System.out.println("OptimizationPolicy.myAddTangents()");
		//		System.out.format("Adding constraints for retailer %d: [%d,%d)\n", r, t1, t2);
		//		System.out.format("mean = %.1f, sd = %.1f\n",mean,sd);
		// Set up tangent constraints.
		// At the inventory level i, u is the expected unmet demand.
		double uOffset = demand.getUnmetDemand(0) - mean;
		double u = mean;
		int iPast = 0;
		double uPast = u;
		// If we start at 0, we may run into numerical difficulties.
		for (int i = (int) Math.ceil(mean); ; i += tangentIncrement) {
			u = demand.getUnmetDemand(i) - uOffset;

			// Compute the coefficients.
			double b = (u - uPast) / (i - iPast);
			double a = u - b * i;

			// To debug
			//			System.out.println("OptimizationPolicy.myAddTangents()");
			//			System.out.format("iPast = %d\tuPast = %.4e\n", iPast, uPast);
			//			System.out.format("i     = %d\tu     = %.4e\n", i, u);
			//			System.out.format("a + b * iPast = %.4e\n", a + b * iPast);
			//			System.out.format("uPast         = %.4e\n", uPast);
			//			System.out.format("a + b * i     = %.4e\n", a + b * i);
			//			System.out.format("u             = %.4e\n", u);
			//			System.out.format("i = %6d\tGradient = %.4e\tIntercept = %.8e\n",i,b,a);
			//				Helper.waitForUser();


			// Add the constraint -U_[t,u) + a + b * I_t <= 0.
			GRBLinExpr inv = new GRBLinExpr();
			for (int tau = t1; tau < t2; ++tau)
				inv.addTerm(-1, U.get(r, tau));
			inv.addConstant(a);
			inv.addTerm(b, I.get(r, t1));
			//			System.out.println("OptimizationPolicy.myAddTangents()");
			//			System.out.println(inv);
			model.addConstr(inv, GRB.LESS_EQUAL, 0,
					"U_" + r + "_" + t1 + "_" + i);


			// Update past (i,u) pair
			iPast = i;
			uPast = u;

			if (-b < 1.0 / unmetPenalty)
				break;
		}
	}

	@Override
	public void print(PrintStream out) {
		out.println("# Optimization Policy");
		out.println("# T                    = " + T);
		out.println("# Unmet demand penalty = " + unmetPenalty);
		out.println("# Tangent type         = " + tangentType);
		out.println("# Tangent increment    = " + tangentIncrement);
		out.println("# Lead time type       = " + leadTimeType);
		out.println("# Lead time percentile = " + leadTimePercentile);
	}

	void printSolution() {
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream("dei-sol-" + tStart + ".txt"));

			out.print("Period");
			for (int r = 0; r < R; ++r) {
				out.format("\tHasArr%d\tShip%d\tUnmet%d\tInv%d\tInvEnd%d",
						r, r, r, r, r);
			}
			out.println();
			for (int t = tStart; t < tEnd; ++t) {
				for (int r = 0; r < R; ++r) {
					out.format("%d", t);
					out.format("\t%b\t%.1f\t%.1f\t%.1f\t%.1f",
							hasArrival.get(r, t),
							X.get(r, t).get(GRB.DoubleAttr.X),
							U.get(r, t).get(GRB.DoubleAttr.X),
							I.get(r, t).get(GRB.DoubleAttr.X),
							J.get(r, t).get(GRB.DoubleAttr.X));
				}
				out.println();
			}
			out.close();
		} catch (Exception e) {}
	}

	public static enum TangentType {
		MEAN_DEMAND, SINGLE_PERIOD, MULTI_PERIOD, ACTUAL,
	}

	public static enum LeadTimeType {
		ACTUAL, MEAN_LEAD_TIME, CONSERVATIVE  
	}

}
