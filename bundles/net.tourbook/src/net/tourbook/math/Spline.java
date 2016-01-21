package net.tourbook.math;

//This applet implements interpolation using relaxed cubic splines
//The user can click on six points and move them around and the
//spline will be redrawn in real time
import java.applet.Applet;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;

public class Spline extends Applet {

	Point[]				points;		// points to be interpolated
	Point[]				control;		// control points
	int					numpoints;
	Image				offscreenImg;	// used in double buffering
	Graphics			offscreenG;
	double				t;				// time variable
	static final double	k	= .05;		// partition length
	int					moveflag;		// point movement

	// this method initializes the applet
	@Override
	public void init() {

		// start off with 6 points
		points = new Point[6];
		control = new Point[6];
		numpoints = 6;

		moveflag = numpoints;

		final int increment = ((size().width - 60) / (numpoints - 1));
		for (int i = 0; i < numpoints; i++) {
			points[i] = new Point((i * increment) + 30, (size().height / 2));
		}
	}

	@Override
	public void paint(final Graphics g) {

		// points to be plotted
		int x1, y1, x2, y2;

		// Change interpolating points into control points
		control[0] = new Point(points[0].x, points[0].y);
		control[numpoints - 1] = new Point(points[numpoints - 1].x, points[numpoints - 1].y);
		x1 = (int) (1.6077
				* points[1].x
				- .26794
				* points[0].x
				- .43062
				* points[2].x
				+ .11483
				* points[3].x
				- .028708
				* points[4].x + .004785 * points[5].x);

		y1 = (int) (1.6077
				* points[1].y
				- .26794
				* points[0].y
				- .43062
				* points[2].y
				+ .11483
				* points[3].y
				- .028708
				* points[4].y + .004785 * points[5].y);

		control[1] = new Point(x1, y1);
		x1 = (int) (-.43062
				* points[1].x
				+ .07177
				* points[0].x
				+ 1.7225
				* points[2].x
				- .45933
				* points[3].x
				+ .11483
				* points[4].x - .019139 * points[3].x);

		y1 = (int) (-.43062
				* points[1].y
				+ .07177
				* points[0].y
				+ 1.7225
				* points[2].y
				- .45933
				* points[3].y
				+ .11483
				* points[4].y - .019139 * points[3].y);

		control[2] = new Point(x1, y1);
		x1 = (int) (.11483
				* points[1].x
				- .019139
				* points[0].x
				- .45933
				* points[2].x
				+ 1.7225
				* points[3].x
				- .43062
				* points[4].x + .07177 * points[5].x);

		y1 = (int) (.11483
				* points[1].y
				- .019139
				* points[0].y
				- .45933
				* points[2].y
				+ 1.7225
				* points[3].y
				- .43062
				* points[4].y + .07177 * points[5].y);

		control[3] = new Point(x1, y1);
		x1 = (int) (-.028708
				* points[1].x
				+ .004785
				* points[0].x
				+ .114835
				* points[2].x
				- .43062
				* points[3].x
				+ 1.6077
				* points[4].x - .26794 * points[5].x);

		y1 = (int) (-.028708
				* points[1].y
				+ .004785
				* points[0].y
				+ .114835
				* points[2].y
				- .43062
				* points[3].y
				+ 1.6077
				* points[4].y - .26794 * points[5].y);

		control[4] = new Point(x1, y1);
		// Plot points
		for (int i = 0; i < numpoints; i++) {
			offscreenG.fillOval(points[i].x - 2, points[i].y - 2, 4, 4);
		}

		// draw n bezier curves using Bernstein Polynomials
		x1 = points[0].x;
		y1 = points[0].y;
		for (int i = 1; i < numpoints; i++) {
			for (t = i - 1; t <= i; t += k) {
				x2 = (int) (points[i - 1].x + (t - (i - 1))
						* (-3 * (double) points[i - 1].x + 3 * (.6667 * control[i - 1].x + .3333 * control[i].x) + (t - (i - 1))
								* (3
										* (double) points[i - 1].x
										- 6
										* (.6667 * control[i - 1].x + .3333 * control[i].x)
										+ 3
										* (.3333 * control[i - 1].x + .6667 * control[i].x) + (-(double) points[i - 1].x
										+ 3
										* (.6667 * control[i - 1].x + .3333 * control[i].x)
										- 3
										* (.3333 * control[i - 1].x + .6667 * control[i].x) + points[i].x)
										* (t - (i - 1)))));
				y2 = (int) (points[i - 1].y + (t - (i - 1))
						* (-3 * (double) points[i - 1].y + 3 * (.6667 * control[i - 1].y + .3333 * control[i].y) + (t - (i - 1))
								* (3
										* (double) points[i - 1].y
										- 6
										* (.6667 * control[i - 1].y + .3333 * control[i].y)
										+ 3
										* (.3333 * control[i - 1].y + .6667 * control[i].y) + (-(double) points[i - 1].y
										+ 3
										* (.6667 * control[i - 1].y + .3333 * control[i].y)
										-

										3
										* (.3333 * control[i - 1].y + .6667 * control[i].y) + points[i].y)
										* (t - (i - 1)))));
				offscreenG.drawLine(x1, y1, x2, y2);
				x1 = x2;
				y1 = y2;
			}
		}
		// draw buffered image to screen
		g.drawImage(offscreenImg, 0, 0, this);
	}

}
