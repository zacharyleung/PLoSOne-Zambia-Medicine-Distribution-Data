package invsim;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.math.random.RandomDataImpl;

import d65helper.FlexArrayDouble;
import d65helper.FlexArrayLong;
import d65helper.Helper;
import d65helper.ZachRv;
import d65helper.ZachRvNormal;

/**
 * Stochastic Inventory System.
 * 
 * Implements the multiplicative MMFE to generate both demand and
 * forecasts.
 * 
 * @author Zack
 *
 */
public class StochInvSys extends InvSys {
	/** Number of retailers. */
	protected final int R;
	
	protected final String dateStart = "2009-07-01";
	
	/** The number of weeks in a "year." */
	protected final int Y;
	
	final double p;
	
	/** The mean demand in each week over one year. */
	final double[][] demandMeanYear;
	/**
	 * L[r][t][k] = probability that shipment sent to retailer r at the
	 * beginning of period t arrives at the beginning of period t + k.
	 */
	final double[][][] leadTimePmf;

	/**
	 * demandCv[t] = The coefficient of variation of demand if we are
	 * forecasting demand (t+1) periods ahead. 
	 */
	final double[] demandCv;
	
	/**
	 * The demand realization in period t affects the demand in
	 * the periods up to t + M - 1. 
	 */
	final int M;
	
	/**
	 * The forecast/demand ratio is the ratio of the demand
	 * variability that is explained by the forecast to the
	 * total variability of the demand.
	 * If demand is a normal(0,1) random variable, and the 
	 * fdRatio = alpha, then we observe a normal(0,alpha) part
	 * of the demand leaving a normal(0,sqrt(1-alpha)) part
	 * unobserved.
	 */
	final double fdRatio;

	
	final double leadTimePercentile = 0.95;
	
	
	/**
	 * The demand generation process is updated by nu = mu + C * Z.
	 * mu, sigma are vectors of length M.
	 */
	private final double[] sigmaD;
	private final double[] muD;
	
	

	
	/** The realized demand. */
	private FlexArrayLong demandReal;
	private FlexArrayDouble demandMean;
	/** Normal random variables for demand generation. */
	private FlexArrayDouble zD;
	/** Normal random variables for forecast generation.
	 * Note: has mean 0, std deviation 1. */
	private FlexArrayDouble zF;
	/** The start of week inventory at the warehouse. */
	private FlexArrayLong wareInvStart;
	/** The start of week inventory. */
	private FlexArrayLong retailInvStart;
	/** The end of week inventory. */
	private FlexArrayLong retailInvEnd;
	private FlexArrayLong demandUnmet;
	/** The realized lead times. */
	private FlexArrayLong leadTimeReal;
	/** The shipments. */
	private FlexArrayLong shipmentArray;
	
	private List<Shipment> shipmentList = new LinkedList<Shipment>();
	
	private Inventory wareInv;
	private Inventory[] retailInv;
	/**
	 * The warehouseShipments array has length equal to the number of
	 * periods in a year.  warehouseShipments[t] is equal to the
	 * shipment quantity that is received in period t of each year.
	 */
	private int[] warehouseShipments;
	private int tExpiry;
	
	private RandomDataImpl rand;
	
	/** Number of periods in simulation horizon. */
	protected int simHorizon;
	/** The current period. */
	private int currentPeriod;
	
	
	
	
	public StochInvSys(int seed, double p, double[][] demandMeanYear,
			double[][][] leadTimePmf, double[] demandCv, double fdRatio,
			int[] warehouseShipments, int tExpiry) {
		this.R = demandMeanYear.length;
		this.Y = demandMeanYear[0].length;
		this.p = p;
		this.demandMeanYear = demandMeanYear;
		this.demandCv = demandCv;
		this.leadTimePmf = leadTimePmf;
		this.M = demandCv.length - 1;
		this.fdRatio = fdRatio;
		this.warehouseShipments = warehouseShipments;
		this.tExpiry = tExpiry;
		
		sigmaD = new double[M];
		sigmaD[0] = Math.sqrt(Math.log(demandCv[0]*demandCv[0] + 1));
		for (int i = 1; i < M; ++i) {
			sigmaD[i] = Math.sqrt(Math.log(demandCv[i]*demandCv[i] + 1) -
					Math.log(demandCv[i-1]*demandCv[i-1] + 1));
		}
//		System.out.println("sigmaD = ");
//		for (int i = 0; i < M; ++i)
//			System.out.println(sigmaD[i]);
		muD = new double[M];
		for (int i = 0; i < M; ++i)
			muD[i] = -sigmaD[i]*sigmaD[i] / 2;
		

		
		rand = new RandomDataImpl();
		rand.reSeed(seed);
			
		wareInv = new Inventory();
		retailInv = new Inventory[R];
		for (int r = 0; r < R; ++r)
			retailInv[r] = new Inventory();
	}

	
	@Override
	public void simulate(int T, InvPol ip, boolean isDebug) throws Exception {
		simHorizon = T;
		int tStart = -2*Y;
		int tEnd = T + Y;
		// Generate two extra years of demand and forecasts before
		// the actual simulation horizon.
		initialize(tStart,tEnd);
		
		// Generate T periods of demand and demand forecasts.
		for (int t = -Y; t < T; ++t) {
			if (isDebug) {
				System.out.println("Current period = " + t);
				//wareInv.print();
			}
			
			// Remove expired drugs from the inventory and update
			// starting inventory levels
			wareInv.update(t);
			wareInvStart.set(0, t, wareInv.getInventoryLevel());
			for (int r = 0; r < R; ++r) {
				retailInv[r].update(t);
				retailInvStart.set(r, t, retailInv[r].getInventoryLevel());
			}
			
			// Shipments arrive at retailers
			for (ListIterator<Shipment> itr = shipmentList.listIterator(); itr.hasNext();) {
				Shipment s = itr.next();
				if (s.periodArrive == t) {
					int r = s.destination;
					retailInv[r].addDrugs(s.inventory);
					retailInvStart.set(r, t, retailInv[r].getInventoryLevel());
					itr.remove();
				}
			}
			// Shipment arrives at national warehouse
			int quantity = warehouseShipments[Helper.modulo(t, Y)]; 
			wareInv.addDrugs(quantity, t + tExpiry);
			
			// Compute shipments
			this.currentPeriod = t;
			int[] Xnew = ip.computeShipments(this);
			for (int r = 0; r < R; ++r) {
				
				if (Xnew[r] != 0 && leadTimeReal.get(r, t) != NO_LEAD_TIME) {
					Inventory shipment = wareInv.getDrugs(Xnew[r]);
					shipmentList.add(
							new Shipment(r, t, (int) leadTimeReal.get(r, t),
									shipment, leadTimePmf[r][Helper.modulo(t, Y)]));
					shipmentArray.set(r, t, Xnew[r]);
				}
			}
			
			// Demand arrives, update inventory
			for (int r = 0; r < R; ++r) {
				long d = demandReal.get(r, t);
				long i = retailInv[r].getInventoryLevel();
				retailInv[r].getDrugs(Math.min(d, i));
				demandUnmet.set(r, t, Math.round(Math.max(0, d - i)));
				retailInvEnd.set(r, t, Math.round(Math.max(0, i - d)));
			}
		} // end for (int t = -Y; t < T; ++t)
	}

	@Override
	public void printState() {
		// TODO Auto-generated method stub

	}

	@Override
	public Stats printStats(PrintStream out) {
		long[] Ur = new long[R];
		long[] Hr = new long[R];
		long[] Dr = new long[R];
		long D = 0;
		long U = 0;
		long H = 0;
		int T = simHorizon;
		for (int r = 0; r < R; ++r) {
			for (int t = 0; t < T; ++t) {
				Dr[r] += demandReal.get(r, t);
				Ur[r] += demandUnmet.get(r, t);
				Hr[r] += retailInvEnd.get(r, t);
				D += demandReal.get(r, t);
				U += demandUnmet.get(r, t);
				H += retailInvEnd.get(r, t);
			}
		}
		double d = (double) D / T;
		double u = (double) U / T;
		double h = (double) H / T;
		double s = 1 - u / d;
		double tc = p * u + h;
		printParameters(out);
		out.println("Stochastic Inventory System");
		out.println("Lost sales penalty = " + p);
		out.println("Number of periods = " + T);
		out.println("Mean unmet demand = " + u);
		out.println("Mean holding cost = " + h);
		out.println("Mean service level = " + s);
		out.println("Mean total cost   = " + (p * u + h));
		out.print("Retailer service level =");
		for (int r = 0; r < R; ++r) {
			out.format(" %.3f", 1.0 - (double) Ur[r] / Dr[r]);
		}
		out.println("\n\n");
		return new Stats(u,h,s,tc);
	}

	@Override
	public DltInvSys getDltInvSys(int T) {
		// For now, I am going to just set the cv of demand in
		// each future period as demandCv[M].  This is equivalent
		// to having no forecast accuracy, just using the demand
		// mean from the past.
		int tCur = currentPeriod;
		ZachRv[][] D = new ZachRv[R][T];
		
		for (int r = 0; r < R; ++r) {
			for (int u = 0; u < T; ++u) {
				double mean = demandMean.get(r, tCur+u);
				double sd = mean * demandCv[M];
				D[r][u] = new ZachRvNormal(mean, sd);
			}
		}
		

		// Format the actual realized lead times.
		int[][] myL = new int[R][T];
		double[][][] myLeadTimePmf = new double[R][T][];
		for (int r = 0; r < R; ++r) {
			for (int u = 0; u < T; ++u) {
				// If we can ship in this period.
				if (leadTimeReal.get(r, tCur + u) != NO_LEAD_TIME) {
					myL[r][u] = (int) leadTimeReal.get(r, tCur + u);
					// Make a copy of the lead time pmf.
					double[] pmf = leadTimePmf[r][Helper.modulo(tCur + u, Y)];
					myLeadTimePmf[r][u] = new double[pmf.length];
					for (int k = 0; k < pmf.length; ++k)
						myLeadTimePmf[r][u][k] = pmf[k];
				}
				else {
					myL[r][u] = NO_LEAD_TIME;
					myLeadTimePmf[r][u] = null;
				}
			}
		}
		
		int[] Xw = new int[T+1];
		// Start from k = 1 because whatever was scheduled to arrive
		// this week has already arrived.
		for (int k = 1; k < T+1; ++k)
			Xw[k] = warehouseShipments[Helper.modulo(tCur + k, Y)];
		
		return new DltInvSys(p, getInitInv(), getShipments(), D,
				myL, myLeadTimePmf, wareInv.getInventoryLevel(), Xw);
	}

	@Override
	public DetInvSys getDetInvSys(int T) throws Exception {
		int tCur = currentPeriod;
		double[][] dm = new double[R][T];
		double[][] cv = new double[R][T];
		for (int r = 0; r < R; ++r) {
			// Initialize the demand means.
			for (int u = 0; u < T; ++u) {
				dm[r][u] = demandMean.get(r, tCur+u);
			}
			// Initialize the demand coefficient of variations.
			for (int u = 0; u < T; ++u) {
				if (u < M)
					cv[r][u] = demandCv[u];
				else
					cv[r][u] = demandCv[M];
			}
			
			// Update the demand means based on realized demand in
			// periods t-(M-1),...,t-1.
			for (int m = 1; m < M; ++m) {
				double z = zD.get(r, tCur-m);
				//System.out.println("z = " + z);
				for (int n = m; n < M; ++n) {
					//System.out.println(dm[r][-m+n]);
					double y = muD[n] + sigmaD[n]*z;
					dm[r][-m+n] *= Math.exp(y);
					//System.out.println(dm[r][-m+n]);
				}
			}
			
			// Update the demand means based on realized forecast
			// info in periods t, t+1,..., t+(M-1).
			// TODO change the realized forecast info horizon.
			double[] sumvar = new double[T];
			for (int m = 0; m < M; ++m) {
				double z = zF.get(r, tCur+m);
				for (int n = 0; n < M; ++n) {
					double y = fdRatio * muD[n] + sigmaD[n]*z;
					sumvar[m+n] += (1-fdRatio)*(sigmaD[n]*sigmaD[n]);
					dm[r][m+n] *= Math.exp(y);
				}
			}
			for (int u = M; u < T; ++u)
				for (int m = 0; m < M && u+m < T; ++m)
					sumvar[u+m] += (sigmaD[m]*sigmaD[m]);
			
			// Compute the coefficient of variation for forecasting
			// demand in period t+n.
			for (int u = 0; u < T; ++u) {
				cv[r][u] = Math.sqrt(Math.exp(sumvar[u]) - 1);
			}
		}

//		// For debugging
//		for (int i = 0; i < T; ++i)
//			System.out.println(demandMean.get(0, t+i) + "\t" + dm[0][i] + "\t"
//					+ demandReal.get(0,t+i) + "\t"+ cv[0][i]);
		
		
		int[][] myL = new int[R][T];
		long[][] myD = new long[R][T];
		for (int r = 0; r < R; ++r) {
			for (int u = 0; u < T; ++u) {
				if (leadTimeReal.get(r, tCur + u) != NO_LEAD_TIME)
					myL[r][u] = (int) leadTimeReal.get(r, tCur + u);
				else
					myL[r][u] = NO_LEAD_TIME;
				double sigma = cv[0][u];
				double mu = -sigma*sigma/2;
				double logn = rand.nextGaussian(mu,sigma);
				myD[r][u] = Helper.randomRound(rand, dm[r][u] * Math.exp(logn));
			}
		}
		
		return new DetInvSys(p, wareInv.getInventoryLevel(), 
				getInitInv(),getShipments(),myD,dm,myL);
	}
	
	public void printGraph(String filename, InvPol invPol) throws Exception {
		PrintStream out = new PrintStream(new FileOutputStream(filename));
		
		printParameters(out);
		invPol.print(out);
		
		// Print header.
		out.print("Date\t\tIWare");
		for (int r = 0; r < R; ++r)
			out.print("\tDMean-" + r + "\tDReal-" + r
					+ "\tIStrt-" + r + "\tDUnmt-" + r
					+ "\tSOPro-" + r
					+ "\tShip-" + r);
		out.println();
		
		long[] iware = new long[Y];
		long[][] dm = new long[R][Y];
		long[][] dr = new long[R][Y];
		long[][] inv = new long[R][Y];
		long[][] du = new long[R][Y];
		long[][] x = new long[R][Y];
		double[] count = new double[Y];
		for (int t = 0; t < simHorizon; ++t) {
			int i = Helper.modulo(t, Y);
			++count[i];
			iware[i] += wareInvStart.get(0, t);
			for (int r = 0; r < R; ++r) {
				dm[r][i] += demandMean.get(r, t);
				dr[r][i] += demandReal.get(r, t);
				inv[r][i] += retailInvStart.get(r, t);
				du[r][i] += demandUnmet.get(r, t);
				x[r][i] += shipmentArray.get(r, t);
			}
		}
//		System.out.println(dr[0][0]);
//		System.out.println(inv[0][0]);
//		System.out.println(du[0][0]);
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = format.parse(dateStart);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		for (int i = 0; i < Y; ++i) {
			out.format("%s\t%.1f", format.format(calendar.getTime()), iware[i] / count[i]);
			// Update the date to the next date.
			int amountHours = 24 * 365 / Y;
			calendar.add(Calendar.HOUR, amountHours);
			for (int r = 0; r < R; ++r)
				out.format("\t%.1f\t%.1f\t%.1f\t%.1f\t%.3f\t%.1f",
						demandMean.get(r,i), dr[r][i] / count[i],
						inv[r][i] / count[i], du[r][i] / count[i],
						(double) du[r][i] / dr[r][i], x[r][i] / count[i]);
//				out.print("\t" + demandMean.get(r, i) + "\t" + demandReal.get(r, i)
//						+ "\t" + invStart.get(r,i) + "\t" + demandUnmet.get(r,i));
			out.println();
		}
		
		out.close();
	}
	
	@Override
	public double[] meanDemand(int tHistory) {
		double[] d = new double[R];
		double T = tHistory;
		for (int r = 0; r < R; ++r)
			for (int k = 0; k < T; ++k)
				d[r] += demandReal.get(r, currentPeriod - k - 1) / T; 
		return d;
	}

	private void initialize(int tStart, int tEnd) {
		demandReal = new FlexArrayLong(0,R,tStart,tEnd);
		demandMean = new FlexArrayDouble(0,R,tStart,tEnd);
		zD = new FlexArrayDouble(0,R,tStart,tEnd);
		zF = new FlexArrayDouble(0,R,tStart,tEnd);
		wareInvStart = new FlexArrayLong(0,1,tStart,tEnd);
		retailInvStart = new FlexArrayLong(0,R,tStart,tEnd);
		retailInvEnd = new FlexArrayLong(0,R,tStart,tEnd);
		demandUnmet = new FlexArrayLong(0,R,tStart,tEnd);
		leadTimeReal = new FlexArrayLong(0,R,tStart,tEnd);
		shipmentArray = new FlexArrayLong(0,R,tStart,tEnd);
		
		// Generate the random variables for demand and forecast
		// generation.
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				double f = rand.nextGaussian(0, 1);
				//double d = fdRatio * f + rand.nextGaussian(0, Math.sqrt(1-fdRatio));
				double d = rand.nextGaussian(0, 1);
				zD.set(r, t, d);
				zF.set(r, t, f);
			}
		}

		// Initialize the demandMean values.
		for (int r = 0; r < R; ++r) {
			for (int t = tStart; t < tEnd; ++t) {
				int i = Helper.modulo(t, Y);
				//System.out.println(i);
				demandMean.set(r, t, demandMeanYear[r][i]);
			}
		}
		
		// Generate the realized demands.
		for (int r = 0; r < R; ++r) {
			for (int t = tStart+M-1; t < tEnd; ++t) {
				double sumZ = 0;
				for (int m = 0; m < M; ++m)
					sumZ += (muD[m] + sigmaD[m] * zD.get(r, t-m));
				double mean = demandMean.get(r,t) * Math.exp(sumZ);
				// Round the mean to an integer for this period's demand.
				long d = Helper.randomRound(rand, mean);
				demandReal.set(r, t, d);
			}
		}
		
		// Generate the realized lead times.
		for (int r = 0; r < R; ++r) {
			for (int t = tStart+Y; t < tEnd; ++t) {
				int i = Helper.modulo(t, Y);
				if (leadTimePmf[r][i] == null)
					leadTimeReal.set(r, t, NO_LEAD_TIME);
				else {
					long l = Helper.randomPmf(rand, leadTimePmf[r][i]);
					leadTimeReal.set(r, t, l);
					//System.out.println("Lead time " + t + " = " + l);
				}
			}
		}
	}
	
    private Shipment[] getShipments() {
    	Shipment[] X = shipmentList.toArray(new Shipment[0]);
    	// Update the shipment sent periods so that the current
    	// period is 0.
    	for (int i = 0; i < X.length; ++i)
    		X[i] = new Shipment(X[i].destination,
    				X[i].periodSent-currentPeriod,X[i].leadTime,
    				X[i].inventory,X[i].leadTimePmf);
    	return X;
    }
    
    private long[] getInitInv() {
    	long[] myI = new long[R];
    	for (int r = 0; r < R; ++r)
    		myI[r] = retailInvStart.get(r, currentPeriod);
    	return myI;
    }
    
    protected void printParameters(PrintStream out) {
    	out.println("# StochInvSys");
    	out.println("# Unmet demand penalty = " + p);
    }
   

}
