package net.tourbook.math;

//Interactive 2D B-spline, Evgeny Demidov  5 August 2001
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.StringTokenizer;

public class Bspline2nd extends java.applet.Applet implements MouseMotionListener {

	Image		buffImage;
	Graphics	buffGraphics;
	Color		col[];

	int			n	= 1, k = 2, n1, nt, Tmin, Tmax, w, h, h1, w2;
	double[]	Px, Py, ti;
	double		N[][];

	@Override
	public void destroy() {
		removeMouseMotionListener(this);
	}

	public void drawFun() {
		final double step = (ti[nt - 1] - ti[0]) / (w2 - .9);
		double t = ti[0];
		final Color[] iColor = {
				Color.red,
				new Color(0f, .7f, 0f),
				Color.blue,
				Color.magenta,
				new Color(0f, .8f, .8f),
				new Color(.9f, .9f, 0f),
				Color.gray };
		buffGraphics.clearRect(0, 0, w, h);
		Tmin = (int) ((ti[k - 1] - ti[0]) / step) + 1;
		Tmax = (int) ((ti[n1] - ti[0]) / step);
		int i1 = 0;

		for (int l = 0; l < w2; l++) {
			while (t >= ti[i1])
				i1++;
			final int i = i1 - 1;
			col[l] = iColor[(i + 8 - k) % 7];
			for (int j = 0; j < nt; j++)
				N[j][l] = 0;
			N[i][l] = 1;

			for (int m = 2; m <= k; m++) { //  basis functions calculation
				int jb = i - m + 1;
				if (jb < 0)
					jb = 0;
				for (int j = jb; j <= i; j++) {
					N[j][l] = N[j][l]
							* (t - ti[j])
							/ (ti[j + m - 1] - ti[j])
							+ N[j + 1][l]
							* (ti[j + m] - t)
							/ (ti[j + m] - ti[j + 1]);
				}
			}

			t += step;
		}
		for (int j = 0; j < n1; j++) {
			buffGraphics.setColor(iColor[j % 7]);
			t = ti[0];
			int to = (int) t;
			for (int l = 1; l < w2; l++) {
				t += step;
				final int t1 = (int) t;
				buffGraphics.drawLine(to, h1 - (int) (h1 * N[j][l - 1]), t1, h1 - (int) (h1 * N[j][l]));
				to = t1;
			}
		}
		for (int l = k; l <= n1; l++) {
			buffGraphics.setColor(iColor[(l - k) % 7]);
			buffGraphics.drawLine((int) ti[l - 1], 1, (int) ti[l], 1);
		}
		buffGraphics.setColor(Color.black);
		for (int l = 0; l < nt; l++)
			buffGraphics.drawRect((int) ti[l] - 1, 0, 3, 3);
	}

	public void drawSpline() {
		int X, Y;
		buffGraphics.clearRect(0, 0, w2, h);
		buffGraphics.setColor(Color.blue);
		for (int i = 0; i < n1; i++) {
			X = (int) Px[i];
			Y = h1 - (int) Py[i];
			buffGraphics.drawRect(X - 1, Y - 1, 3, 3);
		}
		if (k > 2) {
			int Xo = (int) Px[0], Yo = h1 - (int) Py[0];
			for (int i = 1; i < n1; i++) {
				X = (int) Px[i];
				Y = h1 - (int) Py[i];
				buffGraphics.drawLine(Xo, Yo, X, Y);
				Xo = X;
				Yo = Y;
			}
		}
		double sX = 0, sY = 0;
		for (int j = 0; j < n1; j++) {
			sX += Px[j] * N[j][Tmin];
			sY += Py[j] * N[j][Tmin];
		}
		int Xold = (int) sX, Yold = h1 - (int) sY;
		for (int k = Tmin + 1; k <= Tmax; k++) {
			sX = 0;
			sY = 0;
			for (int j = 0; j < n1; j++) {
				sX += Px[j] * N[j][k];
				sY += Py[j] * N[j][k];
			}
			X = (int) sX;
			Y = h1 - (int) sY;
			buffGraphics.setColor(col[k]);
			if ((X < w2) && (Xold < w2))
				buffGraphics.drawLine(Xold, Yold, X, Y);
			Xold = X;
			Yold = Y;
		}
	}

	@Override
	public void init() {
		w = Integer.parseInt(getParameter("width"));
		h = Integer.parseInt(getParameter("height"));
		h1 = h - 1;
		w2 = w / 2;
		String s = getParameter("N");
		if (s != null) {
			final StringTokenizer st = new StringTokenizer(s);
			n = Integer.parseInt(st.nextToken());
			k = Integer.parseInt(st.nextToken());
		}
		n1 = n + 1;
		nt = n + k + 1;
		Px = new double[n1];
		Py = new double[n1];
		ti = new double[nt + k];
		col = new Color[w2];
		N = new double[nt + 1][w2];
		s = getParameter("pts");
		if (s != null) {
			final StringTokenizer st = new StringTokenizer(s);
			for (int i = 0; i < n1; i++) {
				Px[i] = w2 * Double.valueOf(st.nextToken()).doubleValue();
				Py[i] = h1 * Double.valueOf(st.nextToken()).doubleValue();
			}
		} else {
			Px[0] = .2 * w2;
			Px[1] = .8 * w2;
			Py[0] = .8 * h1;
			Py[1] = .2 * h1;
		}
		s = getParameter("knots");
		if (s != null) {
			final StringTokenizer st = new StringTokenizer(s);
			for (int i = 0; i < nt; i++)
				ti[i] = Double.valueOf(st.nextToken()).doubleValue();
		} else {
			ti[0] = 0;
			ti[1] = 1;
			ti[2] = 2;
			ti[3] = 3;
		}
		final double to = ti[0], dt = ti[nt - 1] - to;
		for (int i = 0; i < nt; i++)
			ti[i] = w2 + w2 * (ti[i] - to) / dt;
		buffImage = createImage(w, h);
		buffGraphics = buffImage.getGraphics();
		setBackground(Color.white);
		buffGraphics.clearRect(0, 0, w, h);
		addMouseMotionListener(this);
		drawFun();
		drawSpline();
	}

	public void mouseDragged(final MouseEvent e) {
		int y = h1 - e.getY();
		if (y < 0)
			y = 0;
		if (y > h1)
			y = h1;
		int x = e.getX();
		int iMin = 0;
		double Rmin = 1e10, r2, xi, yi;
		if (x < w2) {
			if (x > w2 - 10)
				return;
			if (x < 0)
				x = 0;
			for (int i = 0; i < n1; i++) {
				xi = (x - Px[i]);
				yi = (y - Py[i]);
				r2 = xi * xi + yi * yi;
				if (r2 < Rmin) {
					iMin = i;
					Rmin = r2;
				}
			}
			Px[iMin] = x;
			Py[iMin] = y;
		} else {
			if (x > w)
				x = w;
			for (int i = 0; i < nt; i++)
				if ((r2 = Math.abs(ti[i] - x)) < Rmin) {
					iMin = i;
					Rmin = r2;
				}
			ti[iMin] = x;
			drawFun();
		}
		drawSpline();
		repaint();
	}

	public void mouseMoved(final MouseEvent e) {} //1.1 event handling

	@Override
	public void paint(final Graphics g) {
		g.drawImage(buffImage, 0, 0, this);
//  showStatus( " " + x +"  " + y);
	}

	@Override
	public void update(final Graphics g) {
		paint(g);
	}

}
