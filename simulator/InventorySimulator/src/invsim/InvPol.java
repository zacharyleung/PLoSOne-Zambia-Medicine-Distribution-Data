package invsim;

import java.io.PrintStream;

public abstract class InvPol {
    abstract int[] computeShipments(InvSys sys) throws Exception;
    
    /**
     * Print inventory policy name and parameters to the print stream.
     * A "#" is added at the beginning of each line. 
     */
    abstract void print(PrintStream out);
}
