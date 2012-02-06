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

import net.tourbook.photo.manager.Photo;

import org.apache.commons.sanselan.Sanselan;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 * The original source code is in org.eclipse.swt.examples.fileviewer
 */
//public class PicDirImages{
	public class PicDirImages_OLD_from_fileviewer_with_worker{

	private NoGroupRenderer				_groupRenderer;
	private DefaultGalleryItemRenderer	_itemRenderer;

	private GalleryItem					rootItem;

	private final ImageCache			_imageCache			= new ImageCache();

	/*
	 * UI resources
	 */
	private Composite					_uiParent;
	private Display						_display;
	private Gallery						_gallery;

	/*
	 * worker thread management
	 */
	/**
	 * Lock for all worker control data and state
	 */
	private final Object				_workerLock			= new Object();

	/**
	 * The worker's thread
	 */
	private volatile Thread				_workerThread		= null;

	/**
	 * True if the worker must exit on completion of the current cycle
	 */
	private volatile boolean			_workerStopped		= false;

	/**
	 * True if the worker must cancel its operations prematurely perhaps due to a state update
	 */
	private volatile boolean			_workerCancelled	= false;

	/**
	 * Worker state information -- this is what gets synchronized by an update
	 */
	private volatile File				_workerStateDir		= null;

	/**
	 * State information to use for the next cycle
	 */
	private volatile File				_workerNextDir		= null;

	/**
	 * Manages the worker's thread
	 */
	private final Runnable				_workerRunnable;

	{
		_workerRunnable = new Runnable() {
			public void run() {

				while (!_workerStopped) {

					synchronized (_workerLock) {
						_workerCancelled = false;
						_workerStateDir = _workerNextDir;
					}

					workerExecute();

					synchronized (_workerLock) {
						try {
							if ((!_workerCancelled) && (_workerStateDir == _workerNextDir)) {

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

//	void createUI(final Composite parent) {
//
//		_uiParent = parent;
//		_display = parent.getDisplay();
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		{
//
//			/*
//			 * sample snippet for a nebula gallery (http://www.eclipse.org/nebula/, requires
//			 * org.eclipse.nebula.widgets.gallery_1.0.0.200802110300.jar)
//			 */
//			final Gallery gallery = new Gallery(container, SWT.V_SCROLL | SWT.VIRTUAL);
//
//			// Renderers
//			DefaultGalleryGroupRenderer gr = new DefaultGalleryGroupRenderer();
//			gr.setItemSize(64, 64);
//			gr.setMinMargin(3);
//			DefaultGalleryItemRenderer ir = new DefaultGalleryItemRenderer();
//
//			gallery.setGroupRenderer(gr);
//			gallery.setItemRenderer(ir);
//
//			gallery.addListener(SWT.SetData, new Listener() {
//
//				public void handleEvent(Event event) {
//					final Event eventCopy = event;
//					getShell().getDisplay().asyncExec(new Thread() {
//						private org.eclipse.swt.graphics.Image loadImage() {
//							org.eclipse.swt.graphics.Image image = null;
//							File file = new File("/home/jstaerk/Dokumente/usegroup/RE-OTB-08-18.pdf");
//							try {
//								RandomAccessFile raf;
//								raf = new RandomAccessFile(file, "r");
//								FileChannel channel = raf.getChannel();
//								ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
//								PDFFile pdffile = new PDFFile(buf);
//
//								// draw the first page to an image
//								PDFPage page = pdffile.getPage(0);
//
//								//get the width and height for the doc at the default zoom
//								Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page
//										.getBBox()
//										.getHeight());
//
//								//generate the image
//								java.awt.Image img = page.getImage(rect.width, rect.height, //width & height
//										rect, // clip rect
//										null, // null for the ImageObserver
//										true, // fill background with white
//										true // block until drawing is done
//										);
//								BufferedImage awtBufImage = new BufferedImage(
//										rect.width,
//										rect.height,
//										BufferedImage.TYPE_INT_RGB);
//								java.awt.Graphics g = awtBufImage.getGraphics();
//
//								g.drawImage(img, 0, 0, null);
//
//								ImageData swtImageData = convertToSWT(awtBufImage);
//								image = new org.eclipse.swt.graphics.Image(getShell().getDisplay(), swtImageData);
//
//							} catch (FileNotFoundException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//							return image;
//						}
//
//						// source: snippet156,
//						// http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java?view=co
//						private ImageData convertToSWT(BufferedImage bufferedImage) {
//							if (bufferedImage.getColorModel() instanceof DirectColorModel) {
//								DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
//								PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel
//										.getGreenMask(), colorModel.getBlueMask());
//								ImageData data = new ImageData(
//										bufferedImage.getWidth(),
//										bufferedImage.getHeight(),
//										colorModel.getPixelSize(),
//										palette);
//								WritableRaster raster = bufferedImage.getRaster();
//								int[] pixelArray = new int[3];
//								for (int y = 0; y < data.height; y++) {
//									for (int x = 0; x < data.width; x++) {
//										raster.getPixel(x, y, pixelArray);
//										int pixel = palette.getPixel(new RGB(
//												pixelArray[0],
//												pixelArray[1],
//												pixelArray[2]));
//										data.setPixel(x, y, pixel);
//									}
//								}
//								return data;
//							} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
//								IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
//								int size = colorModel.getMapSize();
//								byte[] reds = new byte[size];
//								byte[] greens = new byte[size];
//								byte[] blues = new byte[size];
//								colorModel.getReds(reds);
//								colorModel.getGreens(greens);
//								colorModel.getBlues(blues);
//								RGB[] rgbs = new RGB[size];
//								for (int i = 0; i < rgbs.length; i++) {
//									rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
//								}
//								PaletteData palette = new PaletteData(rgbs);
//								ImageData data = new ImageData(
//										bufferedImage.getWidth(),
//										bufferedImage.getHeight(),
//										colorModel.getPixelSize(),
//										palette);
//								data.transparentPixel = colorModel.getTransparentPixel();
//								WritableRaster raster = bufferedImage.getRaster();
//								int[] pixelArray = new int[1];
//								for (int y = 0; y < data.height; y++) {
//									for (int x = 0; x < data.width; x++) {
//										raster.getPixel(x, y, pixelArray);
//										data.setPixel(x, y, pixelArray[0]);
//									}
//								}
//								return data;
//							}
//							return null;
//						}
//
//						public void run() {
//							GalleryItem item = (GalleryItem) eventCopy.item;
//							int index;
//							if (item.getParentItem() != null) {
//								index = item.getParentItem().indexOf(item);
//								item.setItemCount(0);
//							} else {
//								index = gallery.indexOf(item);
//								item.setItemCount(100);
//							}
//
//							// Your image here
//							ImageLoader loader = new ImageLoader();
//							final ImageData[] imageData = loader.load("libs/gnu_herd_banner_01.png"); //$NON-NLS-1$
//							final Image image = new Image(getShell().getDisplay(), imageData[0]);
//
//							item.setImage(loadImage());
//							item.setText("Item " + index);
//						}
//					});
//				}
//
//			});
//
//			gallery.setItemCount(100);
//
//		}
//
//		setupGallery();
//	}

	void createUI(final Composite parent) {

		_uiParent = parent;
		_display = parent.getDisplay();

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText("images");

			_gallery = new Gallery(container, SWT.VIRTUAL | SWT.V_SCROLL | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_gallery);

			_gallery.addListener(SWT.SetData, new Listener() {
				public void handleEvent(final Event event) {

					final GalleryItem gi = (GalleryItem) event.item;

//					final Photo photo = (Photo) gi.getData();
//
//					final int index = _gallery.indexOf(gi);

					System.out.println("SWT.SetData: " + gi.getText());
				}
			});
		}

		setupGallery();
	}

	void dispose() {

		workerStop();

		_imageCache.dispose();
	}

	private void setupGallery() {

		_groupRenderer = new NoGroupRenderer();
//		_itemRenderer = new ListItemRenderer();

//		_groupRenderer = new DefaultGalleryGroupRenderer();
		_itemRenderer = new DefaultGalleryItemRenderer();
		_itemRenderer.setShowLabels(true);

		_gallery.setGroupRenderer(_groupRenderer);
		_gallery.setItemRenderer(_itemRenderer);
	}

	void showImages(final File dir) {
		workerUpdate(dir);
	}

	/**
	 * Updates the gallery contents
	 */
	private void workerExecute() {

		final File[] dirList = PhotoDirectoryView.getDirectoryList(_workerStateDir);

		_display.syncExec(new Runnable() {
			public void run() {

				// guard against the ui being closed before this runs
				if (_uiParent.isDisposed()) {
					return;
				}

				// clear existing gallerie items
				_gallery.removeAll();
				_imageCache.dispose();

				rootItem = new GalleryItem(_gallery, SWT.None);
				rootItem.setText("root");

			}
		});

		for (final File file : dirList) {

			if (_workerCancelled) {
				break;
			}

			// check if the file is an image
			if (Sanselan.hasImageFileExtension(file) == false) {
				continue;
			}

			workerExecute_10_loadImage(file, rootItem);
		}
	}

	private void workerExecute_10_loadImage(final File imageFile, final GalleryItem rootItem) {

		try {

			final String absolutePath = imageFile.getAbsolutePath();

			final Photo photo = new Photo(imageFile);

			photo.loadMetaData();

			final Image photoImage = new Image(_display, absolutePath);

			if (photo.getWidth() == Integer.MIN_VALUE) {

				// images size is not yet set

				final Rectangle imageSize = photoImage.getBounds();

				photo.setSize(imageSize.width, imageSize.height);
			}
			_imageCache.add(absolutePath, photoImage);

			_display.syncExec(new Runnable() {
				public void run() {

					// guard against the ui being closed before this runs
					if (_uiParent.isDisposed()) {
						return;
					}

					final GalleryItem galItem = new GalleryItem(rootItem, SWT.None);

					galItem.setText(photo.getFileName());
					galItem.setImage(photoImage);

					galItem.setData(photo);
				}
			});

		} catch (final Exception e) {
			// TODO: handle exception
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
	 * @param dir
	 *            the new base directory for the table, null is ignored
	 */
	private void workerUpdate(final File dir) {

		if (dir == null) {
			return;
		}

		if ((_workerNextDir != null) && (_workerNextDir.equals(dir))) {
			return;
		}

		synchronized (_workerLock) {

			_workerNextDir = dir;

			_workerStopped = false;
			_workerCancelled = true;

			_workerLock.notifyAll();
		}

		if (_workerThread == null) {
			_workerThread = new Thread(_workerRunnable, "Image file reader");
			_workerThread.start();
		}
	}

}
