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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.animation.movement.ElasticOut;
import org.eclipse.nebula.animation.movement.ExpoOut;
import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The original source code is in org.eclipse.swt.examples.fileviewer
 */
public class PicDirImages_with_parts_from_sharemedia {

	private static final int			ICON		= 0;
	private static final int			LIST		= 1;

	private int							displayMode	= ICON;
	private int							itemWidth	= 64, itemHeight = 64;

	private Gallery						gallery;
	private AbstractGridGroupRenderer	groupRenderer;
	private AbstractGalleryItemRenderer	itemRenderer;

	private static final ImageCache		_imageCache	= new ImageCache();

	void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUIGallery(container);
		}
	}

	private void createUIGallery(final Composite parent) {

		// Create gallery
		gallery = new Gallery(parent, SWT.V_SCROLL | SWT.VIRTUAL | SWT.MULTI);
		gallery.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		gallery.setLowQualityOnUserAction(true);
		gallery.setHigherQualityDelay(500);
		gallery.setAntialias(SWT.OFF);
		gallery.setInterpolation(SWT.LOW);
		gallery.setVirtualGroups(true);
		gallery.setVirtualGroupDefaultItemCount(100);
		gallery.setVirtualGroupsCompatibilityMode(true);

//		final ScrollingSmoother ss = new ScrollingSmoother(gallery, new ExpoOut());
//		ss.smoothControl(true);

		final Listener itemListener = new Listener() {

			public void handleEvent(final Event event) {
				switch (event.type) {

				case SWT.PaintItem: {

					final GalleryItem galleryItem = (GalleryItem) event.item;

					if (galleryItem != null && galleryItem.getParentItem() != null) {

//						final IMedia m = (IMedia) galleryItem.getData(DATA_MEDIA);
//						final int definition = itemHeight > 140 ? IConstants.IMAGE_LOW : IConstants.IMAGE_THUMB;
//
//						Image img = getImageCache().getImage(m, definition);
//
//						if (img == null) {
//							img = getImageCache().getImage(
//									m,
//									itemHeight > 140 ? IConstants.IMAGE_THUMB : IConstants.IMAGE_LOW);
//
//							final LoadItemCallback callback = new LoadItemCallback(gallery, galleryItem);
//
//							if (img == null && definition == IConstants.IMAGE_LOW) {
//								MediaDownload.getInstance().load(m, IConstants.IMAGE_THUMB, callback);
//							}
//							MediaDownload.getInstance().load(m, definition, callback);
//						}
					}

					break;
				}

				}

			}

		};

		gallery.addListener(SWT.PaintItem, itemListener);

		gallery.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(final SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(final SelectionEvent e) {
//				currentSelection.clear();
//				final GalleryItem[] items = gallery.getSelection();
//				for (final GalleryItem item : items) {
//					currentSelection.add((IMedia) item.getData(DATA_MEDIA));
//				}
			}

		});

		gallery.addListener(SWT.SetData, new Listener() {

			public void handleEvent(final Event event) {

				final GalleryItem item = (GalleryItem) event.item;
				final int index = gallery.indexOf(item);
				item.setText("Item:" + index);
				System.out.println(item.getText());

//				final GalleryItem item = (GalleryItem) event.item;
//
//				if (item.getParentItem() == null) {
//
//					/*
//					 * It's a group
//					 */
//
////					final int index = gallery.indexOf(item);
////					final String label = groupLabels.get(index);
////					item.setText(label);
////					if (logger.isDebugEnabled()) {
////						logger.debug(label);
////					}
////
////					final IQuery groupQuery = groupQueries.get(index);
////					item.setData(DATA_QUERY, groupQuery);
////
////					item.setItemCount(library.countMedia(groupQuery, null));
////					item.setExpanded(true);
//
//				} else {
//
//					/*
//					 * It's an item
//					 */
//
//					final GalleryItem parentItem = item.getParentItem();
//					final int index = parentItem.indexOf(item);
//
//					final IQuery groupQuery = (IQuery) parentItem.getData(DATA_QUERY);
//					List<String> ml = (List<String>) parentItem.getData(DATA_QUERY_RESULT);
//					if (ml == null) {
//						ml = library.selectMedia(groupQuery, null, sort, true);
//						parentItem.setData(DATA_QUERY_RESULT, ml);
//					}
//
//					// TODO : fix exception
//					// java.lang.IndexOutOfBoundsException: Index: 6, Size: 3
//					if (index >= 0 && index < ml.size()) {
//						final IMedia m = library.getMedia(ml.get(index));
//
//						item.setData(DATA_MEDIA, m);
//
//						try {
//							if (m.getMetaAsInteger(IConstants.META_TYPE) == IConstants.TYPE_VIDEO) {
//								item.setData(AbstractGalleryItemRenderer.OVERLAY_BOTTOM_RIGHT, videoOverlay);
//							}
//						} catch (final Exception e) {
//							logger.error(e, e);
//						}
//
//						final String txt = m.getMetaAsString(IConstants.META_NAME);
//						if (txt != null) {
//							item.setText(txt);
//						}
//
//						if (!StringUtils.isEmpty(m.getMetaAsString(IConstants.META_DESCRIPTION))) {
//							item.setDescription(m.getMetaAsString(IConstants.META_DESCRIPTION));
//						}
//					}
//				}
			}
		});

		setDisplayMode(displayMode);

		// gallery.setMenu(this.createMenu(gallery));
		// gallery.addSelectionListener(new SelectionListener() {
		//
		// public void widgetDefaultSelected(SelectionEvent e) {
		// }
		//
		// public void widgetSelected(SelectionEvent e) {
		// GalleryItem sGalleryItem = (GalleryItem) e.item;
		// int itemIndex = gallery.indexOf(sGalleryItem);
		// IMedia m = library.getMedia(visibleMedia.get(itemIndex));
		// m.refreshMetadata();
		// }
		//
		// });

		gallery.addKeyListener(new KeyListener() {

			public void keyPressed(final KeyEvent e) {
//				if (e.keyCode == SWT.F1) {
//					setDisplayMode(ICON);
//				} else if (e.keyCode == SWT.F2) {
//					setDisplayMode(LIST);
//				}

			}

			public void keyReleased(final KeyEvent e) {}

		});

//		final DropTarget target = this.createDragTarget(gallery);
//		final DragSource source = this.createDragSource(gallery);
//		source.setDragSourceEffect(new GalleryDragSourceEffect(gallery, this));
	}

	void dispose() {
		_imageCache.dispose();
	}

	public void setDisplayMode(final int mode) {

		// Free previous renderer
		if (itemRenderer != null) {
			itemRenderer.dispose();
			// itemRenderer = null;
		}

		groupRenderer = new DefaultGalleryGroupRenderer();
		final DefaultGalleryGroupRenderer defaultGalleryGroupRenderer = (DefaultGalleryGroupRenderer) groupRenderer;
		defaultGalleryGroupRenderer.setAnimation(true);
		defaultGalleryGroupRenderer.setAnimationLength(1000);
		defaultGalleryGroupRenderer.setFillIfSingleColumn(true);

		switch (mode) {
		case ICON:
			itemRenderer = new ShareMediaIconRenderer2();
			final ShareMediaIconRenderer2 shareMediaIconRenderer2 = (ShareMediaIconRenderer2) itemRenderer;
			shareMediaIconRenderer2.setShowLabels(false);
			shareMediaIconRenderer2.setDropShadows(true);
			shareMediaIconRenderer2.setDropShadowsSize(5);

			defaultGalleryGroupRenderer.setAnimationCloseMovement(new ExpoOut());
			defaultGalleryGroupRenderer.setAnimationOpenMovement(new ElasticOut());
			groupRenderer.setItemSize((int) (itemWidth * (float) 15 / 11), itemHeight);
			break;

		case LIST:
			itemRenderer = new ShareMediaListRenderer();
			final ShareMediaListRenderer shareMediaListRenderer = (ShareMediaListRenderer) itemRenderer;
			shareMediaListRenderer.setShowLabels(true);
			shareMediaListRenderer.setDropShadowsSize(0);
			shareMediaListRenderer.setDropShadows(false);

			defaultGalleryGroupRenderer.setAnimationCloseMovement(new ExpoOut());
			defaultGalleryGroupRenderer.setAnimationOpenMovement(new ElasticOut());
			groupRenderer.setItemSize((itemWidth * 2), (int) (itemHeight * 0.7));
			break;
		}

		gallery.setItemRenderer(itemRenderer);
		groupRenderer.setAutoMargin(true);
		groupRenderer.setMinMargin(2);

		defaultGalleryGroupRenderer.setTitleForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		// Now using default system gradient
		// ((DefaultGalleryGroupRenderer) groupRenderer).setTitleBackground(this
		// .galleryTitleColor);
//		defaultGalleryGroupRenderer.setFont(this.groupTitle);

		gallery.setGroupRenderer(groupRenderer);

		displayMode = mode;
	}

	void showImages(final File dir) {

	}

}
