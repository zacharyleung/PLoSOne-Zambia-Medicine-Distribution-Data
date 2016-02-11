package invsim;

import java.io.PrintStream;

public class ConstInvPol extends InvPol {
	int[] Q = new int[1];
	
	ConstInvPol(int Q) {
		this.Q[0] = Q;
	}
	
	int[] computeShipments(InvSys s) {
		return Q;
	}

	@Override
	void print(PrintStream out) {
		out.println("Constant " + Q[0] + " policy\n\n");
	}
}
