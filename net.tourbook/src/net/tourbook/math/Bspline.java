package net.tourbook.math;

//Bspline.java: B-spline curve fitting.
//Uses: Point2D (Section 1.5).

//Copied from Section 4.2 of
// Ammeraal, L. (1998) Computer Graphics for Java Programmers,
//    Chichester: John Wiley.

import java.awt.Canvas;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D.Float;
import java.util.Vector;


public class Bspline extends Frame {
	public static void main(final String[] args) {
		new Bspline();
	}

	Bspline() {
		super("Define points; press any key after the final one"); //$NON-NLS-1$
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				System.exit(0);
			}
		});
		setSize(500, 300);
		add("Center", new CvBspline()); //$NON-NLS-1$
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		show();
	}
}

class CvBspline extends Canvas {
	
	Vector	V	= new Vector();
//	int		np	= 0, centerX, centerY;
	float	rWidth	= 10.0F, rHeight = 7.5F, eps = rWidth / 100F, pixelSize;
	boolean	ready	= false;

//	CvBspline() {
//		addMouseListener(new MouseAdapter() {
//			@Override
//			public void mousePressed(final MouseEvent evt) {
//				final float x = fx(evt.getX()), y = fy(evt.getY());
//				if (ready) {
//					V.removeAllElements();
//					np = 0;
//					ready = false;
//				}
//				V.addElement(new Float(x, y));
//				np++;
//				repaint();
//			}
//		});
//		addKeyListener(new KeyAdapter() {
//			@Override
//			public void keyTyped(final KeyEvent evt) {
//				evt.getKeyChar();
//				if (np >= 4)
//					ready = true;
//				repaint();
//			}
//		});
//	}

	void bspline(final Graphics g, final Float[] P) {
		
		final int m = 50, n = P.length;
		float xA, yA, xB, yB, xC, yC, xD, yD, a0, a1, a2, a3, b0, b1, b2, b3, x = 0, y = 0, x0, y0;
//		boolean first = true;
		
		for (int i = 1; i < n - 2; i++) {
			
			xA = P[i - 1].x;
			xB = P[i].x;
			xC = P[i + 1].x;
			xD = P[i + 2].x;
			yA = P[i - 1].y;
			yB = P[i].y;
			yC = P[i + 1].y;
			yD = P[i + 2].y;
			a3 = (-xA + 3 * (xB - xC) + xD) / 6;
			b3 = (-yA + 3 * (yB - yC) + yD) / 6;
			a2 = (xA - 2 * xB + xC) / 2;
			b2 = (yA - 2 * yB + yC) / 2;
			a1 = (xC - xA) / 2;
			b1 = (yC - yA) / 2;
			a0 = (xA + 4 * xB + xC) / 6;
			b0 = (yA + 4 * yB + yC) / 6;
			
			for (int j = 0; j <= m; j++) {
				
				x0 = x;
				y0 = y;
				
				final float t = (float) j / (float) m;
				x = ((a3 * t + a2) * t + a1) * t + a0;
				y = ((b3 * t + b2) * t + b1) * t + b0;
				
//				if (first) {
//					first = false;
//				} else {
//					g.drawLine(iX(x0), iY(y0), iX(x), iY(y));
//				}
			}
		}
	}

//	float fx(final int X) {
//		return (X - centerX) * pixelSize;
//	}
//
//	float fy(final int Y) {
//		return (centerY - Y) * pixelSize;
//	}
//
//	void initgr() {
//		final Dimension d = getSize();
//		final int maxX = d.width - 1, maxY = d.height - 1;
//		pixelSize = Math.max(rWidth / maxX, rHeight / maxY);
//		centerX = maxX / 2;
//		centerY = maxY / 2;
//	}
//
//	int iX(final float x) {
//		return Math.round(centerX + x / pixelSize);
//	}
//
//	int iY(final float y) {
//		return Math.round(centerY - y / pixelSize);
//	}
//
//	@Override
//	public void paint(final Graphics g) {
//		initgr();
//		final int left = iX(-rWidth / 2), right = iX(rWidth / 2), bottom = iY(-rHeight / 2), top = iY(rHeight / 2);
//		g.drawRect(left, top, right - left, bottom - top);
//		final Float[] P = new Float[np];
//		V.copyInto(P);
//		if (!ready) {
//			for (int i = 0; i < np; i++) { // Show tiny rectangle around point:
//				g.drawRect(iX(P[i].x) - 2, iY(P[i].y) - 2, 4, 4);
//				if (i > 0)
//					// Draw line P[i-1]P[i]:
//					g.drawLine(iX(P[i - 1].x), iY(P[i - 1].y), iX(P[i].x), iY(P[i].y));
//			}
//		}
//		if (np >= 4)
//			bspline(g, P);
//	}
}
