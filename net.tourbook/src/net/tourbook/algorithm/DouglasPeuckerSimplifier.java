/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/

package net.tourbook.algorithm;

import java.util.ArrayList;


public class DouglasPeuckerSimplifier {

	/**
	 * Approximation tolerance
	 */
	private int			tolerance;

	private Point[]		graphPoints;

	/**
	 * Contains true for each point in graphPoints which remains for the
	 * simplifyed graph
	 */
	private boolean[]	usedPoints;


	public DouglasPeuckerSimplifier(int tolerance, Point[] graphPoints) {
		this.tolerance = tolerance;
		this.graphPoints = graphPoints;
	}


	public Object[] simplify() {

		// set all used points to false
		usedPoints = new boolean[graphPoints.length];
		for (int iPoint = 0; iPoint < graphPoints.length; iPoint++) {
			usedPoints[iPoint] = false;
		}

		// start and end points are used
		usedPoints[0] = true;
		usedPoints[usedPoints.length - 1] = true;

		// simplify between start and end
		simplifySection(0, graphPoints.length - 1);

		// create a point list with all simplified points
		ArrayList<Point> simplifiedPoints = new ArrayList<Point>();
		for (int iPoint = 0; iPoint < graphPoints.length; iPoint++) {
			if (usedPoints[iPoint]) {
				Point graphPoint = graphPoints[iPoint];
				simplifiedPoints.add(new Point(graphPoint.x, graphPoint.y, iPoint));
			}
		}

		return simplifiedPoints.toArray();
	}


	private void simplifySection(int startIndex, int endIndex) {

		if (startIndex >= endIndex + 1) {
			// nothing can be simplified
			return;
		}

		// check for adequate approximation by segment S from v[j] to v[k]
		// index of vertex farthest from S
		int maxi = startIndex;

		// tolerance squared of farthest vertex
		long maxd2 = 0;

		// tolerance squared
		long tol2 = tolerance * tolerance;

		// Segment S = { v[j], v[k] }; // segment from v[j] to v[k]
		Point startPoint = graphPoints[startIndex];
		Point endPoint = graphPoints[endIndex];

		// Vector u = S.P1 - S.P0; // segment direction vector
		Vector u = endPoint.diff(startPoint);

		// double cu = dot(u, u); // segment length squared
		long cu = u.dot(u);

		// test each vertex v[i] for max distance from S
		// compute using the Feb 2001 Algorithm's dist_Point_to_Segment()
		// Note: this works in any dimension (2D, 3D, ...)
		Vector w;
		Point Pb; // base of perpendicular from v[i] to S
		long cw, dv2; // dv2 = distance v[i] to S squared
		float b;

		for (int i = startIndex + 1; i < endIndex; i++) {

			final Point currentPoint = graphPoints[i];

			// compute distance squared dv2

			// w = v[i] - S.P0;
			w = currentPoint.diff(startPoint);

			// cw = dot(w, u);
			cw = w.dot(u);

			if (cw <= 0) {
				// dv2 = d2(v[i], S.P0);
				dv2 = currentPoint.d2(startPoint);

			} else if (cu <= cw) {

				// dv2 = d2(v[i], S.P1);
				dv2 = currentPoint.d2(endPoint);

			} else {

				b = (float) cw / cu;
				// Pb = S.P0 + b * u;
				Pb = startPoint.add(u.dot(b));

				// dv2 = d2(v[i], Pb);
				dv2 = currentPoint.d2(Pb);
			}

			// test with current max distance squared
			if (dv2 <= maxd2) {
				continue;
			}

			// v[i] is a new max vertex
			maxi = i;
			maxd2 = dv2;
		}

		if (maxd2 > tol2) {
			// error is worse than the tolerance

			// split the polyline at the farthest vertex from S
			// mk[maxi] = 1; // mark v[maxi] for the simplified polyline
			usedPoints[maxi] = true;

			// recursively simplify the two subpolylines at v[maxi]

			// polyline v[j] to v[maxi]
			simplifySection(startIndex, maxi);

			// polyline v[maxi] to v[k]
			simplifySection(maxi, endIndex);

		} else {
			// else the approximation is OK, so ignore intermediate vertices
		}
	}
}
