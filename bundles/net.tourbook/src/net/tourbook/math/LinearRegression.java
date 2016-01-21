package net.tourbook.math;

/**
 * For calculating a simple linear regression. Adapted from
 * http://www-stat.stanford.edu/~naras/java/course/lec2/ Balasubramanian Narasimhan, Stanford
 */
public class LinearRegression {

	private double[]	x;
	private double[]	y;
	private double		meanX;
	private double		meanY;
	private double		slope;
	private double		intercept;
	private double		stndDevX;
	private double		stndDevY;

	public LinearRegression(final double[] x, final double[] y) {
		this.x = x;
		this.y = y;
		compute();
	}

//	/** An example. */
//	public static void main(String[] args) {
//		double[] x = { 38, 56, 59, 64, 74 };
//		double[] y = { 41, 63, 70, 72, 84 };
//		LinearRegression lr = new LinearRegression(x, y);
//		System.out.println(lr.getRoundedModel());
//		System.out.println("calculate y given an x of 38 " + lr.calculateY(38));
//		System.out.println("calculate x given a y of 41 " + lr.calculateX(41));
//	}

	/**
	 * Return approximated X value, good for a single interpolation, multiple calls are inefficient!
	 */
	public static double interpolateX(	final double x1,
										final double y1,
										final double x2,
										final double y2,
										final double fixedY) {
		final double[] x = { x1, x2 };
		final double[] y = { y1, y2 };
		final LinearRegression lr = new LinearRegression(x, y);
		return lr.calculateX(fixedY);
	}

	/**
	 * Return approximated Y value, good for a single interpolation, multiple calls are inefficient!
	 */
	public static double interpolateY(	final double x1,
										final double y1,
										final double x2,
										final double y2,
										final double fixedX) {
		final double[] x = { x1, x2 };
		final double[] y = { y1, y2 };
		final LinearRegression lr = new LinearRegression(x, y);
		return lr.calculateY(fixedX);
	}

	/** Calculate X given Y. */
	public double calculateX(final double y) {
		return (y - intercept) / slope;
	}

	/** Calculate Y given X. */
	public double calculateY(final double x) {
		return slope * x + intercept;
	}

	/** Performs linear regression */
	private void compute() {

		final double n = x.length;
		double sumy = 0.0, sumx = 0.0, sumx2 = 0.0, sumy2 = 0.0, sumxy = 0.0;

		for (int i = 0; i < n; i++) {
			sumx += x[i];
			sumx2 += x[i] * x[i];
			sumy += y[i];
			sumy2 += y[i] * y[i];
			sumxy += x[i] * y[i];
		}

		meanX = sumx / n;
		meanY = sumy / n;

		slope = (sumxy - sumx * meanY) / (sumx2 - sumx * meanX);
		intercept = meanY - slope * meanX;

		stndDevX = Math.sqrt((sumx2 - sumx * meanX) / (n - 1));
		stndDevY = Math.sqrt((sumy2 - sumy * meanY) / (n - 1));
	}

	public double getIntercept() {
		return intercept;
	}

	/** Returns Y=mX+b with full precision, no rounding of numbers. */
	public String getModel() {
		return "Y= " + slope + "X + " + intercept + " RSqrd=" + getRSquared(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public double getRSquared() {
		final double r = slope * stndDevX / stndDevY;
		return r * r;
	}

//	/** Returns Y=mX+b */
//	public String getRoundedModel() {
//		return "Y= "
//				+ Num.formatNumber(slope, 3)
//				+ "X + "
//				+ Num.formatNumber(intercept, 3)
//				+ " RSqrd="
//				+ Num.formatNumber(getRSquared(), 3);
//	}

	//getters
	public double getSlope() {
		return slope;
	}

	public double[] getX() {
		return x;
	}

	/** Nulls the x and y arrays. Good to call before saving. */
	public void nullArrays() {
		x = null;
		y = null;
	}
}
