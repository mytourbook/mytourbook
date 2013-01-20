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

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

/**
 * Controls to display rating stars
 */
public class RatingStars extends Canvas {

	public static final int	MAX_RATING_STARS	= 5;

	private static Image	_imageRatingStar;
	private static Image	_imageRatingStarAndHovered;
	private static Image	_imageRatingStarDisabled;
	private static Image	_imageRatingStarDelete;
	private static Image	_imageRatingStarHovered;
	private static Image	_imageRatingStarNotHovered;
	private static Image	_imageRatingStarNotHoveredButSet;

	private static int		_ratingStarImageWidth;
	private static int		_ratingStarImageHeight;

	private static int		MAX_RATING_STARS_WIDTH;

	/**
	 * Number of rating stars which are currently be set for the photo filter. When 0 this filter is
	 * disabled and all photos are displayed.
	 */
	private int				_filterRatingStars	= 3;

	/**
	 * Number of stars which are currently be hovered.
	 */
	private int				_hoveredStars;

	/**
	 * Is <code>true</code> when rating stars are enabled and can be modified.
	 */
	private boolean			_isEnabled			= true;

	static {

		final ImageRegistry imageRegistry = UI.IMAGE_REGISTRY;

		_imageRatingStar = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR);
		_imageRatingStarAndHovered = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_AND_HOVERED);
		_imageRatingStarDelete = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_DELETE);
		_imageRatingStarDisabled = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_DISABLED);
		_imageRatingStarHovered = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_HOVERED);
		_imageRatingStarNotHovered = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_NOT_HOVERED);
		_imageRatingStarNotHoveredButSet = imageRegistry.get(PhotoUI.PHOTO_RATING_STAR_NOT_HOVERED_BUT_SET);

		final Rectangle ratingStarBounds = _imageRatingStar.getBounds();
		_ratingStarImageWidth = ratingStarBounds.width;
		_ratingStarImageHeight = ratingStarBounds.height;

		MAX_RATING_STARS_WIDTH = _ratingStarImageWidth * MAX_RATING_STARS;
	}

	public RatingStars(final Composite parent) {

		super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_FOCUS);

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

		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(final MouseEvent e) {
				onMouseMove(e);
			}
		});

		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {
				onMouseExit(e);
			}
		});
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified when ratings stars has
	 * been modified by the user, by sending it one of the messages defined in the
	 * <code>SelectionListener</code> interface.
	 * <p>
	 * <code>widgetSelected</code> is called when the control is selected by the user.
	 * <code>widgetDefaultSelected</code> is not called.
	 * </p>
	 * <p>
	 * During <code>widgetSelected</code> the application can use <code>getSelection()</code> to
	 * determine the current selected state of the receiver.
	 * </p>
	 * 
	 * @param listener
	 *            the listener which should be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #removeSelectionListener
	 * @see SelectionEvent
	 */
	public void addSelectionListener(final SelectionListener listener) {

		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		final TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Selection, typedListener);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint, final boolean changed) {
		checkWidget();

		final int width = MAX_RATING_STARS_WIDTH;
		final int height = _ratingStarImageHeight;

		final Point e = new Point(width, height);

		return e;
	}

	/**
	 * @return Returns number of selected rating stars. Returns 0 when nothing is selected.
	 */
	public int getSelection() {

		return _filterRatingStars;
	}

	/**
	 * Send a selection event {@link SWT#Selection} or {@link SWT#DefaultSelection} for a gallery
	 * item.
	 * <p>
	 * {@link Event#data} contains the selected rating stars.
	 */
	private void notifySelectionListeners() {

		final Event e = new Event();

		e.widget = this;

		e.data = _filterRatingStars;

		try {
			notifyListeners(SWT.Selection, e);
		} catch (final RuntimeException ex) {
			StatusUtil.log(ex);
		}
	}

	private void onMouseDown(final MouseEvent mouseEvent) {

		final int mouseX = mouseEvent.x;

		final int oldFilterStars = _filterRatingStars;

		final int hittedStar = mouseX / _ratingStarImageWidth + 1;

		if (_filterRatingStars == hittedStar) {

			// remove filtered stars

			_filterRatingStars = 0;

		} else {

			_filterRatingStars = hittedStar;
		}

		if (oldFilterStars != _filterRatingStars) {

			redraw();

			notifySelectionListeners();
		}
	}

	private void onMouseExit(final MouseEvent e) {

		_hoveredStars = 0;

		redraw();
	}

	private void onMouseMove(final MouseEvent mouseEvent) {

		final int mouseX = mouseEvent.x;

		final int oldHoveredStars = _hoveredStars;

		_hoveredStars = mouseX / _ratingStarImageWidth + 1;

		// optimize drawing
		if (_hoveredStars != oldHoveredStars) {
			redraw();
		}

	}

	private void onPaint(final PaintEvent paintEvent) {

		final GC gc = paintEvent.gc;

		final boolean isStarHovered = _hoveredStars > 0;

		for (int starIndex = 0; starIndex < RatingStars.MAX_RATING_STARS; starIndex++) {

			Image starImage;

			if (_isEnabled) {

				if (isStarHovered) {

					if (starIndex < _hoveredStars) {

						if (starIndex < _filterRatingStars) {

							if (starIndex == _filterRatingStars - 1) {
								starImage = _imageRatingStarDelete;
							} else {
								starImage = _imageRatingStarAndHovered;
							}
						} else {
							starImage = _imageRatingStarHovered;
						}

					} else {
						if (starIndex < _filterRatingStars) {
							starImage = _imageRatingStarNotHoveredButSet;
						} else {
							starImage = _imageRatingStarNotHovered;
						}
					}

				} else {

					if (starIndex < _filterRatingStars) {
						starImage = _imageRatingStar;
					} else {
						starImage = _imageRatingStarNotHovered;
					}
				}

			} else {

				// rating stars cannot be set

				if (isStarHovered == false) {

					if (starIndex > 0) {

						// draw only one disabled star, thats enough

						return;
					}
				}

				starImage = _imageRatingStarDisabled;
			}

			// draw stars

			gc.drawImage(starImage, _ratingStarImageWidth * starIndex, 0);
		}
	}

	/**
	 * Removes the listener from the collection of listeners who will be notified when a rating star
	 * is selected by the user.
	 * 
	 * @param listener
	 *            the listener which should no longer be notified
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created
	 *                the receiver</li>
	 *                </ul>
	 * @see SelectionListener
	 * @see #addSelectionListener
	 */
	public void removeSelectionListener(final SelectionListener listener) {

		checkWidget();

		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		removeListener(SWT.Selection, listener);
	}

	public void setSelection(final int filterRatingStars) {

		_filterRatingStars = filterRatingStars;

		redraw();
	}
}
