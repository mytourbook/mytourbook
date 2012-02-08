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

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.List;

import net.tourbook.photo.manager.ILoadCallBack;
import net.tourbook.photo.manager.Photo;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoLoadingState;
import net.tourbook.photo.manager.PhotoManager;

import org.apache.commons.sanselan.Sanselan;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;

/**
 * This class is a compilation from different source codes:
 * 
 * <pre>
 * org.eclipse.swt.examples.fileviewer
 * org.sharemedia.gui.libraryviews.GalleryLibraryView
 * org.dawb.common
 * org.apache.commons.sanselan
 * </pre>
 */
public class PicDirImages {

	private GalleryItem						_rootItem;

	private AbstractGalleryItemRenderer		_itemRenderer;
	private AbstractGridGroupRenderer		_groupRenderer;

	private int								_photoSize			= 64;

	private File[]							_photoFiles;
	private FileFilter						_fileFilter;

	/*
	 * UI resources
	 */
	private Color							_bgColor			= new Color(Display.getDefault(), 0x50, 0x50, 0x50);

	/*
	 * UI controls
	 */
	private Display							_display;
	private Composite						_uiContainer;
	private Gallery							_gallery;
	private CLabel							_lblStatusInfo;
	private ProgressBar						_progbarLoading;

	/*
	 * worker thread management
	 */
	/**
	 * Worker start time
	 */
	private long							_workerStart;
	/**
	 * Lock for all worker control data and state
	 */
	private final Object					_workerLock			= new Object();

	/**
	 * The worker's thread
	 */
	private volatile Thread					_workerThread		= null;

	/**
	 * True if the worker must exit on completion of the current cycle
	 */
	private volatile boolean				_workerStopped		= false;

	/**
	 * True if the worker must cancel its operations prematurely perhaps due to a state update
	 */
	private volatile boolean				_workerCancelled	= false;

	/**
	 * Worker state information -- this is what gets synchronized by an update
	 */
	private volatile File					_workerStateDir		= null;

	/**
	 * State information to use for the next cycle
	 */
	private volatile File					_workerNextFolder	= null;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable					_workerRunnable;

	{
		_workerRunnable = new Runnable() {
			public void run() {

				while (!_workerStopped) {

					synchronized (_workerLock) {
						_workerCancelled = false;
						_workerStateDir = _workerNextFolder;
					}

					workerExecute();

					synchronized (_workerLock) {
						try {
							if ((!_workerCancelled) && (_workerStateDir == _workerNextFolder)) {

								/*
								 * wait until the next images should be displayed
								 */

								_workerLock.wait();
							}
						} catch (final InterruptedException e) {}
					}
				}

				_workerThread = null;

				/*
				 * wake up UI thread in case it is in a modal loop awaiting thread termination (see
				 * workerStop())
				 */
				_display.wake();
			}
		};
	}

	/**
	 * 
	 */
	public static final Comparator<File>	NATURAL_SORT		= new SortNatural<File>(true);

	/**
	 * 
	 */
	public static Comparator<File>			DATE_SORT;
	{
		DATE_SORT = new Comparator<File>() {
			@Override
			public int compare(final File one, final File two) {

				if (_workerCancelled) {
					// couldn't find another way how to stop sorting
					return 0;
				}

				final long diff = one.lastModified() - two.lastModified();

				if (diff == 0) {
					return NATURAL_SORT.compare(one, two);
				}

				if (diff > 0) {
					return (int) two.lastModified();
				}
				return -1;
			}
		};
	}
//	private Comparator<File>				_currentComparator	= SortingUtils.DATE_SORT;
	private Comparator<File>				_currentComparator	= DATE_SORT;

	private class LoadImageCallback implements ILoadCallBack {

		private GalleryItem	__galleryItem;

		/**
		 * @param gallery
		 * @param galleryItem
		 */
		public LoadImageCallback(final GalleryItem galleryItem) {

			__galleryItem = galleryItem;
		}

		@Override
		public void imageIsLoaded() {

			Display.getDefault().syncExec(new Runnable() {

				public void run() {

					if (__galleryItem.isDisposed() || _gallery.isDisposed()) {
						return;
					}

					final Rectangle bounds = __galleryItem.getBounds();

//					System.out.println("redraw: " + bounds);
//					// TODO remove SYSTEM.OUT.PRINTLN

					_gallery.redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
				}
			});
		}

// ORIGINAL
//		public void mediaLoaded(final IMedia media, final int definition, final Image img) {
//			ImageService.getInstance().acquire(img);
//			GalleryLibraryView.this.galleryImageCache.setImage(media, definition, img);
//
//			Display.getDefault().syncExec(new Runnable() {
//
//				public void run() {
//					if (LoadItemCallback.this.item.isDisposed() || LoadItemCallback.this.callbackGallery.isDisposed()) {
//						return;
//					}
//
//					final Rectangle bounds = LoadItemCallback.this.item.getBounds();
//
//					LoadItemCallback.this.callbackGallery
//							.redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
//				}
//			});
//		}
	}

	/**
	 * This will be configured from options but for now it is any image accepted.
	 * 
	 * @return
	 */
	private FileFilter createFileFilter() {

		return new FileFilter() {
			@Override
			public boolean accept(final File pathname) {

				if (pathname.isDirectory()) {
					return false;
				}

				if (pathname.isHidden()) {
					return false;
				}

				final String name = pathname.getName();
				if (name == null || name.length() == 0) {
					return false;
				}

				if (name.startsWith(".")) {
					return false;
				}

				if (Sanselan.hasImageFileExtension(pathname)) {
					return true;
				}

				return false;
			}
		};
	}

	void createUI(final Composite parent) {

		_uiContainer = parent;
		_display = parent.getDisplay();

		_fileFilter = createFileFilter();

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_Gallery(container);
			createUI_20_StatusLine(container);
		}
	}

	/**
	 * Create gallery
	 */
	private void createUI_10_Gallery(final Composite parent) {

		_gallery = new Gallery(parent, SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_gallery);
		_gallery.setBackground(_bgColor);
		_gallery.setLowQualityOnUserAction(true);
		_gallery.setHigherQualityDelay(500);
		_gallery.setAntialias(SWT.OFF);
		_gallery.setInterpolation(SWT.LOW);
		_gallery.setVirtualGroups(true);
		_gallery.setVirtualGroupDefaultItemCount(1);
		_gallery.setVirtualGroupsCompatibilityMode(true);

		_gallery.addListener(SWT.SetData, new Listener() {
			public void handleEvent(final Event event) {
				onSetData(event);
			}
		});

		_gallery.addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				onPaintItem(event);
			}
		});

		/*
		 * set renderer
		 */
		_itemRenderer = new PhotoRenderer();
		final PhotoRenderer photoRenderer = (PhotoRenderer) _itemRenderer;
		photoRenderer.setShowLabels(false);
		photoRenderer.setBackgroundColor(_bgColor);
//		photoRenderer.setDropShadows(true);
//		photoRenderer.setDropShadowsSize(5);
		_gallery.setItemRenderer(_itemRenderer);

		_groupRenderer = new NoGroupRendererMT();
		_groupRenderer.setItemSize((int) (_photoSize * (float) 15 / 11), _photoSize);
		_groupRenderer.setAutoMargin(true);
		_groupRenderer.setMinMargin(0);

		_gallery.setGroupRenderer(_groupRenderer);

		_rootItem = new GalleryItem(_gallery, SWT.VIRTUAL);

		_gallery.setItemCount(1);
	}

	private void createUI_20_StatusLine(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: info
			 */
			_lblStatusInfo = new CLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblStatusInfo);

			/*
			 * progress bar
			 */
			_progbarLoading = new ProgressBar(container, SWT.HORIZONTAL | SWT.SMOOTH);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(_progbarLoading);
		}
	}

	void dispose() {

		_bgColor.dispose();
		workerStop();
	}

	private void onPaintItem(final Event event) {

		final GalleryItem galleryItem = (GalleryItem) event.item;

		if (galleryItem != null && galleryItem.getParentItem() != null) {

			/*
			 * check if the photo image is available, if not, image must be loaded
			 */

			final Photo photo = (Photo) galleryItem.getData();

			final int imageQuality = _photoSize > PhotoManager.THUMBNAIL_DEFAULT_SIZE
					? PhotoManager.IMAGE_QUALITY_600
					: PhotoManager.IMAGE_QUALITY_THUMB_160;

			// check if image is already loaded or has an loading error
			final PhotoLoadingState photoLoadingState = photo.getLoadingState()[imageQuality];
			if (photoLoadingState == PhotoLoadingState.IMAGE_HAS_A_LOADING_ERROR
					|| photoLoadingState == PhotoLoadingState.IMAGE_IS_BEING_LOADED) {
				return;
			}

			final Image photoImage = PhotoImageCache.getImage(photo.getImageKey(imageQuality));
			if (photoImage == null || photoImage.isDisposed()) {

				// the requested image is not available in the image cache -> image must be loaded

				PhotoManager.loadImage(photo, imageQuality, new LoadImageCallback(galleryItem));
			}

// ORIGINAL
//			final IMedia m = (IMedia) galleryItem.getData(DATA_MEDIA);
//			final int definition = itemHeight > 140 ? IConstants.IMAGE_LOW : IConstants.IMAGE_THUMB;
//
//			Image img = getImageCache().getImage(m, definition);
//
//			if (img == null) {
//				img = getImageCache().getImage(m, itemHeight > 140 ? IConstants.IMAGE_THUMB : IConstants.IMAGE_LOW);
//
//				final LoadItemCallback callback = new LoadItemCallback(_gallery, galleryItem);
//
//				if (img == null && definition == IConstants.IMAGE_LOW) {
//					MediaDownload.getInstance().load(m, IConstants.IMAGE_THUMB, callback);
//				}
//				MediaDownload.getInstance().load(m, definition, callback);
//			}
		}
	}

	private void onSetData(final Event event) {

		final GalleryItem galleryItem = (GalleryItem) event.item;

		if (galleryItem.getParentItem() == null) {

			/*
			 * It's a group
			 */

			galleryItem.setItemCount(_photoFiles.length);

		} else {

			/*
			 * It's an item
			 */

			final GalleryItem parentItem = galleryItem.getParentItem();
			final int galleryItemIndex = parentItem.indexOf(galleryItem);

			final Photo photo = new Photo(_photoFiles[galleryItemIndex], galleryItemIndex);
			galleryItem.setData(photo);

			galleryItem.setText(photo.getFileName());
		}
	}

	void setThumbnailSize(final int imageSize) {

		_photoSize = imageSize;

		_groupRenderer.setItemSize((int) (_photoSize * (float) 15 / 11), _photoSize);
	}

	/**
	 * Display images for the selected folder.
	 * 
	 * @param dir
	 */
	void showImages(final File dir) {

		PhotoManager.stopImageLoading();

		workerUpdate(dir);
	}

	/**
	 * Updates the gallery contents
	 */
	private void workerExecute() {

		_workerStart = System.currentTimeMillis();

		File[] newPhotoFiles = null;

		if (_workerStateDir != null) {

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					_lblStatusInfo.setText("reading: " + _workerStateDir.getAbsolutePath());
				}
			});

			// We make file list in this thread for speed reasons
			final List<File> files = SortingUtils.getSortedFileList(_workerStateDir, _fileFilter, _currentComparator);

			if (_workerCancelled) {
				return;
			}

			if (files == null) {
				// prevent NPE
				newPhotoFiles = new File[0];
			} else {
				newPhotoFiles = files.toArray(new File[files.size()]);
			}

			_photoFiles = newPhotoFiles;

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiContainer.isDisposed()) {
						return;
					}

					// this will update the gallery
					_gallery.clearAll();

					/*
					 * update status info
					 */
					final long timeDiff = System.currentTimeMillis() - _workerStart;
					final String timeDiffText = Long.toString(timeDiff)
							+ " ms  :  "
							+ Integer.toString(_photoFiles.length)
							+ "  :  "
							+ _workerStateDir.getAbsolutePath();

					_lblStatusInfo.setText(timeDiffText);
				}
			});
		}
	}

	/**
	 * Stops the worker and waits for it to terminate.
	 */
	private void workerStop() {

		if (_workerThread == null) {
			return;
		}

		synchronized (_workerLock) {

			_workerCancelled = true;
			_workerStopped = true;

			_workerLock.notifyAll();
		}

		while (_workerThread != null) {
			if (!_display.readAndDispatch()) {
				_display.sleep();
			}
		}
	}

	/**
	 * Notifies the worker that it should update itself with new data. Cancels any previous
	 * operation and begins a new one.
	 * 
	 * @param newFolder
	 *            the new base directory for the table, null is ignored
	 */
	private void workerUpdate(final File newFolder) {

		if (newFolder == null) {
			return;
		}

		if ((_workerNextFolder != null) && (_workerNextFolder.equals(newFolder))) {
			return;
		}

		synchronized (_workerLock) {

			_workerNextFolder = newFolder;

			_workerStopped = false;
			_workerCancelled = true;

			_workerLock.notifyAll();
		}

		if (_workerThread == null) {
			_workerThread = new Thread(_workerRunnable, "PicDirImages: retrieve files");
			_workerThread.start();
		}
	}
}
