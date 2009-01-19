package net.tourbook.math;

/*      This example of the use of CubicSpline demonstrates
 interpolation within a data set of the refractive index of
 fused quartz tabulated as a function of wavelength.

 Michael T Flanagan

 20 May 2002
 Updated 29 April 2005, 4 February 2006

 */

public class CubicSplineExample {

	public static void main(final String arg[]) {

		// Array of wavelengths (m)
		final double[] wavelength = { 185.0e-9, 214.0e-9, 275.0e-9, 361.0e-9, 509.0e-9, 589.0e-9, 656.0e-9 };

		// Array of corresponding refractive indices
		final double[] refrindex = { 1.57464, 1.53386, 1.49634, 1.47503, 1.4619, 1.4583, 1.4564 };

		// Interpolation variables
		double x1, y1;

		// Create a CubicSpline instance and initialise it to the data stored in the arrays wavelength and refrindex
		final CubicSpline cs = new CubicSpline(wavelength, refrindex);

		// First interpolation at a wavelength of 250 nm
		//   also calculates the required derivatives
		x1 = 2.5e-7;
		y1 = cs.interpolate(x1);
		System.out.println("The refractive index of fused quartz at " + x1 * 1.0e9 + " nm is " + y1);

		// Second interpolation at a wavelength of 590 nm
		//  uses the derivatives calculated in the first call
		x1 = 5.9e-7;
		y1 = cs.interpolate(x1);
		System.out.println("The refractive index of fused quartz at " + x1 * 1.0e9 + " nm is " + y1);
	}
}
