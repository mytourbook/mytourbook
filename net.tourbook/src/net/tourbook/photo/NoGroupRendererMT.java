/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import net.tourbook.photo.gallery.GalleryMTItem;
import net.tourbook.photo.gallery.NoGroupRenderer;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class NoGroupRendererMT extends NoGroupRenderer {

	/**
	 * Threshold for how often the event loop is spun, in ms.
	 */
	private static int	T_THRESH	= 100;

	/**
	 * Maximum amount of time to spend processing events, in ms.
	 */
	private static int	T_MAX		= 50;

	/**
	 * Last time the event loop was spun.
	 */
	private long		lastTime	= System.currentTimeMillis();

	@Override
	public void draw(	final GC gc,
						final GalleryMTItem group,
						final int x,
						final int y,
						final int clipX,
						final int clipY,
						final int clipWidth,
						final int clipHeight) {

//		System.out.println("draw\tclipX:"
//				+ clipX
//				+ "  clipY:"
//				+ clipY
//				+ "  clipWidth:"
//				+ clipWidth
//				+ "  clipHeight:"
//				+ clipHeight);
//		// TODO remove SYSTEM.OUT.PRINTLN

		// get items in the clipping area
		final int[] visibleIndexes = getVisibleItems(group, x, y, clipX, clipY, clipWidth, clipHeight, OFFSET);

		if (visibleIndexes != null && visibleIndexes.length > 0) {

			final long start = System.currentTimeMillis();

			for (int itemIndex = visibleIndexes.length - 1; itemIndex >= 0; itemIndex--) {

				// Draw item
				final GalleryMTItem galleryItem = group.getItem(visibleIndexes[itemIndex]);

				final boolean isSelected = group.isSelected(galleryItem);

				drawItem(gc, visibleIndexes[itemIndex], isSelected, group, OFFSET);

				// check if a paint interruption occured
				if (itemIndex % 100 == 0) {
//					runEventLoop();
//					if (group.getGallery().isPaintingInterrupted()) {
//
//						System.out.println("time\t"
//								+ (System.currentTimeMillis() - start)
//								+ " ms - isPaintingInterrupted\t");
//						// TODO remove SYSTEM.OUT.PRINTLN
//
//
//						return;
//					}
				}
			}

			setAllVisibleItems(group, x, y);
		}

	}

	/**
	 * Runs an event loop.
	 */
	private void runEventLoop() {
		// Only run the event loop so often, as it is expensive on some platforms
		// (namely Motif).
		final long t = System.currentTimeMillis();
		if (t - lastTime < T_THRESH) {
			return;
		}
		lastTime = t;
		// Run the event loop.
		final Display disp = Display.getDefault();
		if (disp == null) {
			return;
		}

		//Initialize an exception handler from the window class.
//		final ExceptionHandler handler = ExceptionHandler.getInstance();

		for (;;) {
			try {
				if (!disp.readAndDispatch()) {
					break;
				}
			} catch (final Throwable e) {//Handle the exception the same way as the workbench
//				handler.handleException(e);
				break;
			}

			// Only run the event loop for so long.
			// Otherwise, this would never return if some other thread was
			// constantly generating events.
			if (System.currentTimeMillis() - t > T_MAX) {
				break;
			}
		}
	}
	public void setAllVisibleItems(final GalleryMTItem groupItem, final int x, final int y) {

		final Rectangle clientArea = groupItem.getGallery().getClientAreaCached();

		final int[] visibleIndexes = getVisibleItems(groupItem, x, y, 0, 0, clientArea.width, clientArea.height, OFFSET);

		if (visibleIndexes != null && visibleIndexes.length > 0) {

			final GalleryMTItem[] visibleItems = new GalleryMTItem[visibleIndexes.length];

			for (int itemIndex = visibleIndexes.length - 1; itemIndex >= 0; itemIndex--) {
				visibleItems[itemIndex] = groupItem.getItem(visibleIndexes[itemIndex]);
			}

			groupItem.setVisibleItems(visibleItems);
		}
	}

}
