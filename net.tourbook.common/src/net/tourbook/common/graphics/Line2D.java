/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

/*
 * Copied from com.sun.javafx.geom.Line2D and adjusted to int.
 */

package net.tourbook.common.graphics;

import org.eclipse.swt.graphics.Rectangle;

public class Line2D {

	/**
	 * The X coordinate of the start point of the line segment.
	 */
	public int				x1;

	/**
	 * The Y coordinate of the start point of the line segment.
	 */
	public int				y1;

	/**
	 * The X coordinate of the end point of the line segment.
	 */
	public int				x2;

	/**
	 * The Y coordinate of the end point of the line segment.
	 */
	public int				y2;

	/**
	 * The bitmask that indicates that a point lies to the left of this <code>Rectangle2D</code>.
	 */
	public static final int	OUT_LEFT	= 1;

	/**
	 * The bitmask that indicates that a point lies above this <code>Rectangle2D</code>.
	 */
	public static final int	OUT_TOP		= 2;

	/**
	 * The bitmask that indicates that a point lies to the right of this <code>Rectangle2D</code>.
	 */
	public static final int	OUT_RIGHT	= 4;

	/**
	 * The bitmask that indicates that a point lies below this <code>Rectangle2D</code>.
	 */
	public static final int	OUT_BOTTOM	= 8;

	/**
	 * Constructs and initializes a Line from the specified coordinates.
	 * 
	 * @param x1
	 *            the X coordinate of the start point
	 * @param y1
	 *            the Y coordinate of the start point
	 * @param x2
	 *            the X coordinate of the end point
	 * @param y2
	 *            the Y coordinate of the end point
	 */
	public Line2D(final int x1, final int y1, final int x2, final int y2) {

		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	/**
	 * {@inheritDoc}
	 */
	static int outcode(final int rx, final int ry, final int rwidth, final int rheight, final int x, final int y) {
		/*
		 * Note on casts to double below. If the arithmetic of x+w or y+h is done in int, then we
		 * may get integer overflow. By converting to double before the addition we force the
		 * addition to be carried out in double to avoid overflow in the comparison. See bug 4320890
		 * for problems that this can cause.
		 */

		int out = 0;

		if (rwidth <= 0) {
			out |= OUT_LEFT | OUT_RIGHT;
		} else if (x < rx) {
			out |= OUT_LEFT;
		} else if (x > rx + rwidth) {
			out |= OUT_RIGHT;
		}

		if (rheight <= 0) {
			out |= OUT_TOP | OUT_BOTTOM;
		} else if (y < ry) {
			out |= OUT_TOP;
		} else if (y > ry + rheight) {
			out |= OUT_BOTTOM;
		}

		return out;
	}

	/**
	 * Tests if the interior of the <code>Shape</code> intersects the interior of a specified
	 * rectangular area. The rectangular area is considered to intersect the <code>Shape</code> if
	 * any point is contained in both the interior of the <code>Shape</code> and the specified
	 * rectangular area.
	 * <p>
	 * The {@code Shape.intersects()} method allows a {@code Shape} implementation to conservatively
	 * return {@code true} when:
	 * <ul>
	 * <li>there is a high probability that the rectangular area and the <code>Shape</code>
	 * intersect, but
	 * <li>the calculations to accurately determine this intersection are prohibitively expensive.
	 * </ul>
	 * This means that for some {@code Shapes} this method might return {@code true} even though the
	 * rectangular area does not intersect the {@code Shape}. The { com.sun.javafx.geom.Area Area}
	 * class performs more accurate computations of geometric intersection than most {@code Shape}
	 * objects and therefore can be used if a more precise answer is required.
	 * 
	 * @param x
	 *            the X coordinate of the upper-left corner of the specified rectangular area
	 * @param y
	 *            the Y coordinate of the upper-left corner of the specified rectangular area
	 * @param w
	 *            the width of the specified rectangular area
	 * @param h
	 *            the height of the specified rectangular area
	 * @return <code>true</code> if the interior of the <code>Shape</code> and the interior of the
	 *         rectangular area intersect, or are both highly likely to intersect and intersection
	 *         calculations would be too expensive to perform; <code>false</code> otherwise.
	 */
	public boolean intersects(final int x, final int y, final int w, final int h) {

		int out1, out2;

		if ((out2 = outcode(x, y, w, h, x2, y2)) == 0) {
			return true;
		}

		int px = x1;
		int py = y1;

		int endlessCounter = 0;

		while ((out1 = outcode(x, y, w, h, px, py)) != 0) {

			// prevent endless loops, this happened during testing
			if (++endlessCounter > 1000) {
				return false;
			}

			if ((out1 & out2) != 0) {
				return false;
			}

			if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {

				px = x;
				if ((out1 & OUT_RIGHT) != 0) {
					px += w;
				}
				py = y1 + (px - x1) * (y2 - y1) / (x2 - x1);

			} else {

				py = y;
				if ((out1 & OUT_BOTTOM) != 0) {
					py += h;
				}
				px = x1 + (py - y1) * (x2 - x1) / (y2 - y1);
			}
		}

		return true;
	}

	public Rectangle rectangle() {
		return new Rectangle(x1, y1, x2 - x1, y2 - y1);
	}
}
