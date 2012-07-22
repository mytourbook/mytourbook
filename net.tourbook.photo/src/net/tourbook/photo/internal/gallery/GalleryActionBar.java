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
package net.tourbook.photo.internal.gallery;

import net.tourbook.common.Activator;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageGallery;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.internal.ImageSizeIndicator;
import net.tourbook.photo.internal.Messages;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class GalleryActionBar {

	private static final int				IMAGE_INDICATOR_SIZE	= 16;

	private static final IPreferenceStore	_prefStore				= Activator.getDefault().getPreferenceStore();

	private ImageGallery					_imageGallery;

	/*
	 * UI controls
	 */
	private Composite						_containerActionBar;
	private Composite						_customActionBarContainer;
	private ToolBar							_toolbar;
	private Spinner							_spinnerThumbSize;
	private ImageSizeIndicator				_canvasImageSizeIndicator;

	public GalleryActionBar(final Composite parent, final ImageGallery imageGallery) {

		_imageGallery = imageGallery;

		createUI_10_ActionBar(parent);
	}

	private void createUI_10_ActionBar(final Composite parent) {

		_containerActionBar = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerActionBar);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.extendedMargins(0, 2, 2, 2)
				.spacing(3, 0)
				.applyTo(_containerActionBar);
		{
			/*
			 * toolbar actions
			 */
			_toolbar = new ToolBar(_containerActionBar, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_toolbar);

			createUI_16_CustomActionBar(_containerActionBar);
			createUI_17_ImageSize(_containerActionBar);
			createUI_18_ImageSizeIndicator(_containerActionBar);
		}
	}

	private void createUI_16_CustomActionBar(final Composite parent) {

		_customActionBarContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_customActionBarContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_customActionBarContainer);
	}

	/**
	 * spinner: thumb size
	 */
	private void createUI_17_ImageSize(final Composite parent) {

		_spinnerThumbSize = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerThumbSize);
		_spinnerThumbSize.setMinimum(ImageGallery.MIN_GALLERY_ITEM_WIDTH);
		_spinnerThumbSize.setMaximum(ImageGallery.MAX_GALLERY_ITEM_WIDTH);
		_spinnerThumbSize.setIncrement(1);
		_spinnerThumbSize.setPageIncrement(50);
		_spinnerThumbSize.setToolTipText(UI.IS_OSX
				? Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip_OSX
				: Messages.Pic_Dir_Spinner_ThumbnailSize_Tooltip);
		_spinnerThumbSize.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_imageGallery.setThumbnailSize(_spinnerThumbSize.getSelection());
			}
		});
		_spinnerThumbSize.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				_imageGallery.setThumbnailSize(_spinnerThumbSize.getSelection());
			}
		});
	}

	/**
	 * canvas: image size indicator
	 */
	private void createUI_18_ImageSizeIndicator(final Composite parent) {

		_canvasImageSizeIndicator = new ImageSizeIndicator(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.hint(IMAGE_INDICATOR_SIZE, IMAGE_INDICATOR_SIZE)
				.align(SWT.CENTER, SWT.CENTER)
				.applyTo(_canvasImageSizeIndicator);
	}

	public Composite getCustomContainer() {
		return _customActionBarContainer;
	}

	public int getThumbnailSize() {
		return _spinnerThumbSize.getSelection();
	}

	public ToolBar getToolbar() {
		return _toolbar;
	}

	public void restoreState(final IDialogSettings state, final int stateThumbSize) {

		updateUI_ImageIndicatorTooltip();

		_spinnerThumbSize.setSelection(stateThumbSize);
	}

	public void updateColors(final Color fgColor, final Color bgColor) {

		_containerActionBar.setForeground(fgColor);
		_containerActionBar.setBackground(bgColor);

		_toolbar.setForeground(fgColor);
		_toolbar.setBackground(bgColor);

		_spinnerThumbSize.setForeground(fgColor);
		_spinnerThumbSize.setBackground(bgColor);

		_canvasImageSizeIndicator.setForeground(fgColor);
		_canvasImageSizeIndicator.setBackground(bgColor);
	}

	public void updateUI_AfterZoomInOut(final int imageSize) {

		_spinnerThumbSize.setSelection(imageSize);

		final boolean isHqImage = imageSize > PhotoLoadManager.IMAGE_SIZE_THUMBNAIL;

		_canvasImageSizeIndicator.setIndicator(isHqImage);
	}

	public void updateUI_ImageIndicatorTooltip() {

		_canvasImageSizeIndicator.setToolTipText(NLS.bind(
				Messages.Pic_Dir_ImageSizeIndicator_Tooltip,
				_prefStore.getString(IPhotoPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK)));
	}
}
