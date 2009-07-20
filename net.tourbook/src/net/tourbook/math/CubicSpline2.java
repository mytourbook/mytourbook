package net.tourbook.math;

/*
*
*   Class CubicSpline
*
*   Class for performing an interpolation using a cubic spline
*   setTabulatedArrays and interpolate adapted from Numerical Recipes in C
*
*   WRITTEN BY: Michael Thomas Flanagan
*
*   DATE:	May 2002
*   UPDATE: 20 May 2003
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's JAVA library on-line web page:
*   CubicSpline.html
*
*   Copyright (c) May 2003  Michael Thomas Flanagan
*
*   PERMISSION TO COPY:
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/


public class CubicSpline2{

    	private int npoints = 0;        // no. of tabulated points
    	private double[] y = null;      // y=f(x) tabulated function
    	private double[] x = null;      // x in tabulated function f(x)
    	private double[] y2 = null;     // returned second derivatives of y
    	private double yp1 = 0.0D;      // first derivative at point one
                                    	  // default value = zero (natural spline)
    	private double ypn = 0.0D;      // first derivative at point n
                                    	  // default value = zero (natural spline)

    	//  Returns an interpolated value of y for a value of x (xx) from a tabulated function y=f(x)
    	//  after the derivatives (deriv) have been calculated independently of and calcDeriv().
    	public static double interpolate(final double xx, final double[] x, final double[] y, final double[] deriv){
        	if(((x.length != y.length) || (x.length != deriv.length)) || (y.length != deriv.length)){
            		throw new IllegalArgumentException("array lengths are not all equal"); //$NON-NLS-1$
        	}
	    	final int n = x.length;
        	double  h=0.0D, b=0.0D, a=0.0D, yy = 0.0D;

            int k=0;
	    	int klo=0;
	    	int khi=n-1;
        	while(khi-klo > 1){
		    	k=(khi+klo) >> 1;
		    	if(x[k] > xx){
			    	khi=k;
		    	}
	        	else{
	            		klo=k;
		    	}
        	}
        	h=x[khi]-x[klo];

	    	if (h == 0.0){
	        	throw new IllegalArgumentException("Two values of x are identical"); //$NON-NLS-1$
	    	}
	    	else{
	        	a=(x[khi]-xx)/h;
            		b=(xx-x[klo])/h;
	        	yy=a*y[klo]+b*y[khi]+((a*a*a-a)*deriv[klo]+(b*b*b-b)*deriv[khi])*(h*h)/6.0;
	    	}
	    	return yy;
	}

    	// Create a one dimensional array of cubic spline objects of length n each of array length m
    	// Primarily for use in BiCubicSpline
    	public static CubicSpline2[] oneDarray(final int n, final int m){
        	final CubicSpline2[] a =new CubicSpline2[n];
	    	for(int i=0; i<n; i++){
	        	a[i]=CubicSpline2.zero(m);
        	}
        	return a;
    	}

    	// Returns a new CubicSpline setting array lengths to n and all array values to zero with natural spline default
    	// Primarily for use in BiCubicSpline
    	public static CubicSpline2 zero(final int n){
        	final CubicSpline2 aa = new CubicSpline2(n);
        	return aa;
    	}

    	// Constructors
    	// Constructor with data arrays initialised to arrays x and y
    	public CubicSpline2(final double[] x, final double[] y){
        	this.npoints=x.length;
        	if(this.npoints!=y.length)throw new IllegalArgumentException("Arrays x and y are of different length"); //$NON-NLS-1$
        	this.x = new double[npoints];
        	this.y = new double[npoints];
        	this.y2 = new double[npoints];
        	for(int i=0; i<this.npoints; i++){
            		this.x[i]=x[i];
            		this.y[i]=y[i];
        	}
        	this.yp1=1e40;
        	this.ypn=1e40;
    	}

    	// Constructor with data arrays initialised to zero
    	// Primarily for use by BiCubicSpline
    	public CubicSpline2(final int npoints){
        	this.npoints=npoints;
        	this.x = new double[npoints];
        	this.y = new double[npoints];
        	this.y2 = new double[npoints];
        	this.yp1=1e40;
        	this.ypn=1e40;
    	}

    	//  Calculates the second derivatives of the tabulated function
    	//  for use by the cubic spline interpolation method (.interpolate)
    	public void calcDeriv(){
	    	double	p=0.0D,qn=0.0D,sig=0.0D,un=0.0D;
	    	final double[] u = new double[npoints];

	    	if (yp1 > 0.99e30){
	    		y2[0]=u[0]=0.0;
	    	}
	    	else{
			this.y2[0] = -0.5;
		    	u[0]=(3.0/(this.x[1]-this.x[0]))*((this.y[1]-this.y[0])/(this.x[1]-this.x[0])-this.yp1);
	    	}

	    	for(int i=1;i<=this.npoints-2;i++){
		    	sig=(this.x[i]-this.x[i-1])/(this.x[i+1]-this.x[i-1]);
		    	p=sig*this.y2[i-1]+2.0;
		    	this.y2[i]=(sig-1.0)/p;
		    	u[i]=(this.y[i+1]-this.y[i])/(this.x[i+1]-this.x[i]) - (this.y[i]-this.y[i-1])/(this.x[i]-this.x[i-1]);
		    	u[i]=(6.0*u[i]/(this.x[i+1]-this.x[i-1])-sig*u[i-1])/p;
	    	}

	    	if (this.ypn > 0.99e30){
		    	qn=un=0.0;
	    	}
	    	else{
		    	qn=0.5;
		    	un=(3.0/(this.x[npoints-1]-this.x[this.npoints-2]))*(this.ypn-(this.y[this.npoints-1]-this.y[this.npoints-2])/(this.x[this.npoints-1]-x[this.npoints-2]));
	    	}

	    	this.y2[this.npoints-1]=(un-qn*u[this.npoints-2])/(qn*this.y2[this.npoints-2]+1.0);
	    	for(int k=this.npoints-2;k>=0;k--){
		    	this.y2[k]=this.y2[k]*this.y2[k+1]+u[k];
	    	}
    	}

    	// Returns the internal array of second derivatives
    	public double[] getDeriv(){
        	return this.y2;
    	}

    	//  INTERPOLATE
    	//  Returns an interpolated value of y for a value of xfrom a tabulated function y=f(x)
    	//  after the data has been entered via a constructor and the derivatives calculated and
    	//  stored by calcDeriv().
    	public double interpolate(final double xx){
	    	double h=0.0D,b=0.0D,a=0.0D, yy=0.0D;

        	if (xx<this.x[0] || xx>this.x[this.npoints-1]){
	        	throw new IllegalArgumentException("x is outside the range of data points"); //$NON-NLS-1$
	    	}

	    	int k=0;
	    	int klo=0;
	    	int khi=this.npoints-1;
	    	while (khi-klo > 1){
		    	k=(khi+klo) >> 1;
		    	if(this.x[k] > xx){
			    	khi=k;
		    	}
		    	else{
			    	klo=k;
		    	}
	    	}
	    	h=this.x[khi]-this.x[klo];

	    	if (h == 0.0){
	        	throw new IllegalArgumentException("Two values of x are identical: point "+klo+ " ("+this.x[klo]+") and point "+khi+ " ("+this.x[khi]+")" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	    	}
	    	else{
	        	a=(this.x[khi]-xx)/h;
	        	b=(xx-this.x[klo])/h;
	        	yy=a*this.y[klo]+b*this.y[khi]+((a*a*a-a)*this.y2[klo]+(b*b*b-b)*this.y2[khi])*(h*h)/6.0;
	    	}
	    	return yy;
    	}

    	//  METHODS
    	// Resets the x y data arrays - primarily for use in BiCubicSpline
    	public void resetData(final double[] x, final double[] y){
        	if(x.length!=y.length)throw new IllegalArgumentException("Arrays x and y are of different length"); //$NON-NLS-1$
        	if(this.npoints!=x.length)throw new IllegalArgumentException("Original array length not matched by new array length"); //$NON-NLS-1$
        	for(int i=0; i<this.npoints; i++){
            		this.x[i]=x[i];
            		this.y[i]=y[i];
        	}
    	}

    	// Enters the first derivatives of the cubic spline at
    	// the first and last point of the tabulated data
    	// Overrides a natural spline
    	public void setDerivLimits(final double yp1, final double ypn){
        	this.yp1=yp1;
        	this.ypn=ypn;
    	}

}
