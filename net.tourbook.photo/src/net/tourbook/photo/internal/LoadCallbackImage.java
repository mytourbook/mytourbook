package net.tourbook.photo.internal;

import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;

import org.eclipse.swt.widgets.Display;

class LoadCallbackImage implements ILoadCallBack {

	/**
	 * 
	 */
	private final PhotoGallery	_photoGallery;
	private GalleryMT20Item		_galleryItem;

	/**
	 * @param galleryItem
	 * @param photoGallery
	 *            TODO
	 */
	public LoadCallbackImage(final PhotoGallery photoGallery, final GalleryMT20Item galleryItem) {

		_photoGallery = photoGallery;
		_galleryItem = galleryItem;
	}

	@Override
	public void callBackImageIsLoaded(final boolean isUpdateUI) {

		if (isUpdateUI == false) {
			return;
		}

		// mark image area as needed to be redrawn
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				if (_photoGallery.isDisposed()) {
					return;
				}

				/*
				 * Visibility check must be done in the UI thread because scrolling the gallery can
				 * reposition the gallery item. This can be a BIG problem because the redraw()
				 * method is painting the background color at the specified rectangle, it cost me a
				 * lot of time to figure this out.
				 */
				final boolean isItemVisible = _galleryItem.gallery.isItemVisible(_galleryItem);

				if (isItemVisible) {

					// redraw gallery item WITH background
					_photoGallery.redrawGallery(
							_galleryItem.viewPortX,
							_galleryItem.viewPortY,
							_galleryItem.width,
							_galleryItem.height,
							false);
				}
			}
		});

		_photoGallery.jobUILoading_20_Schedule();
	}
}
