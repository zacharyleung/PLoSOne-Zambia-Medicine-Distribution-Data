package com.zacleung.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Scanner;

public abstract class Matrix {
	public static double[][] readDoubleMatrix(String inFile, int nRows, int nCols)
			throws Exception {
		return readDoubleMatrix(new File(inFile), nRows, nCols);
	}
	
	public static double[][] readDoubleMatrix(File inFile, int nRows, int nCols) 
			throws Exception {
		Scanner s = null;
		double[][] d = new double[nRows][nCols];

		try {
			s = new Scanner(new BufferedReader(new FileReader(inFile)));
			for (int i = 0; i < nRows; ++i)
				for (int j = 0; j < nCols; ++j)
					d[i][j] = s.nextDouble();
			return d;
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}
	
	public static void writeDoubleMatrix(String outFile, double[][] matrix)
			throws Exception {
		PrintStream out = null;
		try {
			out = new PrintStream(new FileOutputStream(outFile));
			for (int i = 0; i < matrix.length; ++i) {
				for (int j = 0; j < matrix[0].length; ++j) {
					out.printf("%.2e\t", matrix[i][j]);
				}
				out.println();
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

}
