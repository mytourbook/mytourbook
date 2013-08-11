/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package net.tourbook.map3.shape;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.MultiResolutionPath;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.Iterator;

/**
 *
 */

/**
 * This is a copy from gov.nasa.worldwind.render.MultiResolutionPath (11.8.2013), it creates
 * positions which can be outside of the viewport but is not so fast as the original implementation
 * which creates positions which are only within the viewport.
 * <p>
 * <b>Original comment</b>
 * <p>
 * A Version of {@link Path} that provides level-of-detail. Positions in a {@code Path} are filtered
 * based on a "skip count", the number of positions to skip between positions that are drawn. The
 * skip-count algorithm can be replaced by the application. The default algorithm skips up to four
 * positions, depending on the eye distance from the positions. Also, if the segment between any two
 * positions is too small to be distinguished, it is not drawn. See
 * {@link #makePositions(DrawContext, gov.nasa.worldwind.render.Path.PathData)}.
 * 
 * @author tag
 * @version $Id: MultiResolutionPath.java 514 2012-04-11 16:30:58Z tgaskins $
 */
public class MTMultiResolutionPath extends MultiResolutionPath {

	public MTMultiResolutionPath(final Iterable<? extends Position> positions) {
		super(positions);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to skip positions from this Path's original positions list. Positions are skipped
	 * first according to this Path's skipCountComputer. The skipCountComputer determines how many
	 * positions this path skips between tessellated positions. Any positions remaining after this
	 * step are skipped if the segment they are part of is either very small or not visible.
	 */
	@Override
	protected void makePositions(final DrawContext dc, final PathData pathData) {

		final Iterator<? extends Position> iter = this.positions.iterator();
		Position posA = iter.next();
		int ordinalA = 0;
		Color colorA = this.getColor(posA, ordinalA);

		this.addTessellatedPosition(posA, colorA, ordinalA, pathData); // add the first position of the path

		final int skipCount = this.skipCountComputer.computeSkipCount(dc, pathData);

//		System.out.println(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] \tskipCount: " + skipCount);
//		// TODO remove SYSTEM.OUT.PRINTLN

		// Tessellate each segment of the path.
		Vec4 ptA = this.computePoint(dc.getTerrain(), posA);

		for (int i = 1; iter.hasNext(); i++) {

			final Position posB = iter.next();

			if (i % skipCount != 0 && iter.hasNext()) {
				continue;
			}

//			final int ordinalB = ordinalA + 1;
			final int ordinalB = i;
			final Color colorB = this.getColor(posB, ordinalB);
			final Vec4 ptB = this.computePoint(dc.getTerrain(), posB);

			// If the segment is very small or not visible, don't tessellate, just add the segment's end position.
			if (this.isSmall(dc, ptA, ptB, 8)) {

				continue;

			} else if (!this.isSegmentVisible(dc, posA, posB, ptA, ptB)) {

				this.addTessellatedPosition(posB, colorB, ordinalB, pathData);

			} else {

				this.makeSegment(dc, posA, posB, ptA, ptB, colorA, colorB, ordinalA, ordinalB, pathData);
			}

			posA = posB;
			ptA = ptB;
			ordinalA = ordinalB;
			colorA = colorB;
		}
	}

}
