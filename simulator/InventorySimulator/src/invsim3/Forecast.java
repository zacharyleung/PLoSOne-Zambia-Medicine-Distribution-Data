package invsim3;

import java.text.DecimalFormat;

public class Forecast {
	public int t;
	public double[] mean;
	public double[] var;
	
	Forecast(double[] mean, double[] var) {
		this(0, mean, var);
	}
	
	Forecast(int t, double[] mean, double[] var) {
		this.t = t;
		this.mean = mean;
		this.var = var;
	}
	
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.#");
		String out = "Forecast at period " + t + "\n";
		for (int i = 0; i < mean.length; ++i) {
			out += "Period = " + (t + i) + "\t";
			out += "Mean = " + df.format(mean[i]) + "\t";
			out += "Var = " + df.format(var[i]) + "\n";
		}
		return out;
	}
}
