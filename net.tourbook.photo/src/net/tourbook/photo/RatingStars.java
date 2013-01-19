/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Controls to display rating stars
 */
public class RatingStars extends Canvas {

	private static Image	_imageRatingStar;
	private static Image	_imageRatingStarAndHovered;
	private static Image	_imageRatingStarDisabled;
	private static Image	_imageRatingStarDelete;
	private static Image	_imageRatingStarHovered;
	private static Image	_imageRatingStarNotHovered;
	private static Image	_imageRatingStarNotHoveredButSet;

//	static {
//
//		_imageRatingStar = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR);
//		_imageRatingStarAndHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_AND_HOVERED);
//		_imageRatingStarDelete = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_DELETE);
//		_imageRatingStarDisabled = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_DISABLED);
//		_imageRatingStarHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_HOVERED);
//		_imageRatingStarNotHovered = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_NOT_HOVERED);
//		_imageRatingStarNotHoveredButSet = UI.IMAGE_REGISTRY.get(PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET);
//
//		final Rectangle ratingStarBounds = _imageRatingStar.getBounds();
//		_ratingStarImageWidth = ratingStarBounds.width;
//		_ratingStarImageHeight = ratingStarBounds.height;
//
//		MAX_RATING_STARS_WIDTH = _ratingStarImageWidth * MAX_RATING_STARS;
//	}

	public RatingStars(final Composite parent) {

		super(parent, SWT.DOUBLE_BUFFERED);

		addListener();
	}

	private void addListener() {
		addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				onPaint(e);
			}
		});

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				onMouseDown(e);
			}
		});

	}

	private void onMouseDown(final MouseEvent mouseEvent) {
		// TODO Auto-generated method stub

	}

	private void onPaint(final PaintEvent paintEvent) {
		// TODO Auto-generated method stub

	}
}
