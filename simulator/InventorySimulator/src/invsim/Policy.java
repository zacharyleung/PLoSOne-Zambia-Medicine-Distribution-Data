package invsim;

import java.io.PrintStream;

public abstract class Policy {
    /**
     * Print inventory policy name and parameters to the print stream.
     * A "#" is added at the beginning of each line. 
     */
    abstract public void print(PrintStream out);
}
