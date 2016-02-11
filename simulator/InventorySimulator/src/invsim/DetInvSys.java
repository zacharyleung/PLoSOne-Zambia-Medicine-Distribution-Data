/**
 * Deterministic Inventory System.
 * 
 * In a deterministic inventory system, the demands and lead times are
 * completely determined.
 */
package invsim;

public class DetInvSys extends AbstractInvSys {
	/** Number of retailers. */
	final int R;
	/** Number of periods in horizon. */
	final int T;
	/** Penalty cost for one unit of unmet demand. */
	final double p;
	/** Initial inventory level at the warehouse. */
	final long Iware;
	/** Initial inventory levels. */
	final long[] I0;
	/** Array of shipments in the pipeline. */
	final Shipment[] X;
	/** Demand in each facility and each period. */
	final long[][] D;
	final double[][] dMean;
	/** Deterministic lead time for shipment to facility in period. */
	final int[][] L;
	///** The smallest unit of inventory that can be shipped. */
	//final int invUnit;
	
	DetInvSys(double p, long Iware, long[] I0, Shipment[] X, long[][] D,
			double[][] dMean, int[][] L) {
		this.p  = p;
		this.Iware = Iware;
		this.I0 = I0;
		this.X = X;
		this.D = D;
		this.dMean = dMean;
		this.L = L;
		
		R = I0.length;
		T = L[0].length;
	}
	
	public void print() {
		System.out.println("\n\n");
		System.out.println("-------------------------------");
		System.out.println("Printing DetInvSys");
		System.out.println("Shipments");
		for (int i = 0; i < X.length; ++i)
			System.out.println(X[i]);
		System.out.println("Lead times");
		for (int i = 0; i < L.length; ++i) {
			for (int j = 0; j < L[i].length; ++j) {
				System.out.print(L[i][j] + "\t");
			}
			System.out.println();
		}
		System.out.println("-------------------------------");
		System.out.println("\n\n");
	}

}
