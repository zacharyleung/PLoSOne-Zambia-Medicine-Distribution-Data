package com.zacleung.invsim.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.zacleung.util.MyString;

import invsim3.*;

/**
 * Generate a trace of the simulation results.
 * A software trace means a low-level log of how the program was
 * executed.
 * @author zacharyleung
 *
 */

public class Trace {
	/**
	 * Generate a trace of the simulation results from the simulator
	 * object and print the results into a file.
	 * Prints the trace in long format.
	 * @param simulator
	 * @param filename
	 */
	public static void trace(Simulator simulator, File file, boolean append) 
			throws Exception {
		PrintStream out = new PrintStream(
				new FileOutputStream(file, append));
		
		// read fields from simulator
		NationalWarehouse national = simulator.national;
		Retailer[] retailers = simulator.retailers;
		District[] districts = simulator.districts;
		int tStart = simulator.getStartPeriod();
		int tEnd = simulator.getEndPeriod();
		
		// Print parameters
		out.print(simulator.getParameters());
		
		// Print header
		out.println("period,variable,value");
		
		for (int t = tStart; t < tEnd; ++t) {
			// for the national warehouse
			out.format("%d,nsi,%d%n", t, 
					national.getStartInventory(t));

			// for the districts
			for (int d = 0; d < districts.length; ++d) {
				out.format("%d,d%dsi,%d%n", t, d,
						districts[d].getStartInventory(t));
				out.format("%d,d%do,%d%n", t, d,
						districts[d].getOrders(t));
				out.format("%d,d%ds,%d%n", t, d,
						districts[d].getShipmentQuantity(t));
			}
			
			// for the retailer
			for (int r = 0; r < retailers.length; ++r) {
				out.format("%d,r%dsi,%d%n", t, r,
						retailers[r].getStartInventory(t));
				out.format("%d,r%dd,%d%n", t, r,
						retailers[r].getDemand(t));
				out.format("%d,r%dud,%d%n", t, r,
						retailers[r].getUnmetDemand(t));
				out.format("%d,r%ds,%d%n", t, r,
						retailers[r].getShipmentQuantity(t));
			}
		}

		out.close();
	}
}
