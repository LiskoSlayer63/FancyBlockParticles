package com.TominoCZ.FBP.math;

public class FBPMathHelper {
	public static double add(double d, double add) {
		if (d < 0.0D)
			return d - add;

		return d + add;
	}

	public static double round(double d, int decimals) {
		int i = (int) Math.round(d * Math.pow(10, decimals));
		return ((double) i) / Math.pow(10, decimals);
	}
}