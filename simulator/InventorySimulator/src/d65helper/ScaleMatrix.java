package d65helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.zacleung.util.Matrix;


public class ScaleMatrix {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		String inputFile;
//		String outputFile;
//		
//		inputFile = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48\\demand-means.txt";
//		outputFile = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48\\demand-means-48.txt";
//		scaleMatrix(inputFile, outputFile, 212, 365, 48);
//
//		inputFile = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48\\accessibility.txt";
//		outputFile = "C:\\Users\\zacleung\\Dropbox\\zambia\\java-inv-sim\\input\\212-48\\accessibility-48.txt";
//		stretchMatrix(inputFile, outputFile, 212, 12, 48);

	}

	private static void scaleMatrix(String inputFile, String outputFile,
			int nRows, int nColumnsOld, int nColumnsNew) throws Exception {
		PrintStream out = null;
		try {
			double[][] d = Matrix.readDoubleMatrix(inputFile, nRows, nColumnsOld);
			double[][] dNew = new double[nRows][nColumnsNew];

			out = new PrintStream(new FileOutputStream(outputFile));

			for (int r = 0; r < nRows; ++r) {
				double[] dailyCutUp = new double[nColumnsOld * nColumnsNew];

				for (int i = 0; i < nColumnsOld; ++i)
					for (int j = i * nColumnsNew; j < (i+1) * nColumnsNew; ++j)
						dailyCutUp[j] = d[r][i] / nColumnsNew;

				for (int i = 0; i < nColumnsNew; ++i) {
					for (int j = i * nColumnsOld; j < (i+1) * nColumnsOld; ++j) {
						dNew[r][i] += dailyCutUp[j];
					}
					out.format("%.3e\t", dNew[r][i]);
				}
				out.println();
			}

		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void stretchMatrix(String inputFile, String outputFile,
			int nRows, int nColumnsOld, int nColumnsNew) throws Exception {
		PrintStream out = null;
		try {
			double[][] d = Matrix.readDoubleMatrix(inputFile, nRows, nColumnsOld);
			double[][] dNew = new double[nRows][nColumnsNew];

			out = new PrintStream(new FileOutputStream(outputFile));

			for (int r = 0; r < nRows; ++r) {
				double[] dailyCutUp = new double[nColumnsOld * nColumnsNew];

				for (int i = 0; i < nColumnsOld; ++i)
					for (int j = i * nColumnsNew; j < (i+1) * nColumnsNew; ++j)
						dailyCutUp[j] = d[r][i];

				for (int i = 0; i < nColumnsNew; ++i) {
					for (int j = i * nColumnsOld; j < (i+1) * nColumnsOld; ++j) {
						dNew[r][i] += dailyCutUp[j];
					}
					dNew[r][i] /= nColumnsOld;
					out.format("%.3e\t", dNew[r][i]);
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

