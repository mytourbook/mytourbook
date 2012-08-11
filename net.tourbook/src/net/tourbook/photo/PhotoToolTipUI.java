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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.ui.tourChart.ChartPhoto;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Photo tooltip UI for a tour chart or map or something unknown
 * 
 * @author Wolfgang Schramm, created 3.8.2012
 */

public abstract class PhotoToolTipUI extends PhotoToolTipShell {

	private static final String				STATE_PHOTO_GALLERY_IS_VERTICAL	= "STATE_PHOTO_GALLERY_IS_VERTICAL";	//$NON-NLS-1$

	private IDialogSettings					_state;
	private ArrayList<ChartPhoto>			_hoveredPhotos;

	private int								_hoveredPhotosHash;
	private int								_displayedPhotosHash;

	private final ArrayList<PhotoWrapper>	_photoWrapperList				= new ArrayList<PhotoWrapper>();

	private ToggleGalleryOrientation		_actionToggleGalleryOrientation;

	private boolean							_isVerticalGallery;
	private Point							_devPositionHoveredValue;

	private Color							_fgColor;
	private Color							_bgColor;

	/*
	 * UI controls
	 */
	private Composite						_galleryContainer;
	private ImageGallery					_imageGallery;
	private ToolBar							_toolbarControl;

	private final class PhotoGalleryProvider implements IPhotoGalleryProvider {
		@Override
		public IStatusLineManager getStatusLineManager() {
			return null;
		}

		@Override
		public IToolBarManager getToolBarManager() {
			return null;
		}

		@Override
		public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

		@Override
		public void setSelection(final PhotoSelection photoSelection) {}
	}

	private class ToggleGalleryOrientation extends Action {

		public ToggleGalleryOrientation() {
			super(null, Action.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {

			_imageGallery.saveState(_state);

			// toggle gallery
			_isVerticalGallery = !_isVerticalGallery;

			updateUI_ToogleAction();

			reopenTooltip();
		}
	}

	public PhotoToolTipUI(final Control ownerControl) {

		super(ownerControl);

		initUI(ownerControl);

		createActions();
	}

	private void createActions() {

		_actionToggleGalleryOrientation = new ToggleGalleryOrientation();
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite shell) {

		final Composite container = createUI(shell);

		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {

				// keep gallery state, the gallery can be reopened very often

				_imageGallery.saveState(_state);
			}
		});

		updateUI_Colors(shell);

		super.setImageGallery(_imageGallery);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(2, 2).applyTo(shellContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_Gallery(shellContainer);
		}

		return shellContainer;
	}

	private void createUI_10_Gallery(final Composite parent) {

		final Point shellSize = super.getShellSize();

		_galleryContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.hint(shellSize.x, shellSize.y)
				.grab(true, true)
				.applyTo(_galleryContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
//				.margins(10, 10)
				.extendedMargins(0, 0, 0, 20)
				.applyTo(_galleryContainer);
		_galleryContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_imageGallery = new ImageGallery();

			_imageGallery.setShowActionBar();

			if (_isVerticalGallery) {
				_imageGallery.setShowThumbnailSize();
			}

			final int galleryStyle = _isVerticalGallery ? //
					SWT.V_SCROLL // | SWT.MULTI
					: SWT.H_SCROLL // | SWT.MULTI
			;

			_imageGallery.createImageGallery(_galleryContainer, galleryStyle, new PhotoGalleryProvider());

			_imageGallery.showInfo(false, null, true, true);

			createUI_20_ActionBar(_imageGallery.getCustomActionBarContainer());
		}
	}

	private void createUI_20_ActionBar(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		/*
		 * create toolbar
		 */
		_toolbarControl = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(_toolbarControl);
		_toolbarControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		/*
		 * fill toolbar
		 */
		final ToolBarManager tbm = new ToolBarManager(_toolbarControl);

		tbm.add(_actionToggleGalleryOrientation);

		tbm.update(true);
	}

	public abstract int getHideCounter();

	public abstract ArrayList<ChartPhoto> getHoveredPhotos();

	@Override
	protected Object getToolTipArea(final Event event) {

		final int hideCounter = getHideCounter();

		return hideCounter;
	}

	public abstract void incrementHideCounter();

	private void initUI(final Control control) {

//		_pc = new PixelConverter(control);
	}

	@Override
	boolean isVerticalGallery() {
		return _isVerticalGallery;
	}

	private void reopenTooltip() {

		// hide and recreate tooltip

		hide();

		showPhotoToolTip(_devPositionHoveredValue);
	}

	@Override
	protected void restoreState(final IDialogSettings state) {

		_state = state;

		_isVerticalGallery = Util.getStateBoolean(state, STATE_PHOTO_GALLERY_IS_VERTICAL, true);

		updateUI_ToogleAction();

		super.restoreState(state);
	}

	@Override
	protected void saveState(final IDialogSettings state) {

		state.put(STATE_PHOTO_GALLERY_IS_VERTICAL, _isVerticalGallery);

		super.saveState(state);
	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		_hoveredPhotos = getHoveredPhotos();

		if (super.shouldCreateToolTip(event) == false) {
			return false;
		}

		final boolean isPhotoHovered = _hoveredPhotos != null && _hoveredPhotos.size() > 0;

		return isPhotoHovered;
	}

	protected void showPhotoToolTip(final Point devPositionHoveredValue) {

		// keep position for reopening the tooltip
		_devPositionHoveredValue = devPositionHoveredValue;

		final boolean isPhotoHovered = _hoveredPhotos != null && _hoveredPhotos.size() > 0;
		_hoveredPhotosHash = isPhotoHovered ? _hoveredPhotos.hashCode() : 0;

		/*
		 * show/locate shell
		 */
		boolean isNewImages;
		final Shell ttShell = getToolTipShell();
		if (ttShell == null || ttShell.isDisposed()) {

			// create tooltip shell

			if (show(devPositionHoveredValue) == false) {
				return;
			}

			/*
			 * restore MUST be done after the shell is created, otherwise clientArea() can return 0
			 * values
			 */
			_imageGallery.restoreState(_state);

			_imageGallery.showInfo(false, null, true, true);

			isNewImages = true;

		} else {

			isNewImages = false;

			// move tooltip
			setTTShellLocation();

			// check if new images should be displayed
			if (_displayedPhotosHash == _hoveredPhotosHash) {
				return;
			}
		}

		updateUI(isNewImages);

		_displayedPhotosHash = _hoveredPhotosHash;
	}

	/**
	 * update UI
	 * 
	 * @param isNewImages
	 */
	private void updateUI(final boolean isNewImages) {

		/*
		 * display photo images
		 */
		// create list containing all images
		_photoWrapperList.clear();
		for (final ChartPhoto chartPhoto : _hoveredPhotos) {
			_photoWrapperList.add(chartPhoto.photoWrapper);
		}

		final String galleryPositionKey = _hoveredPhotosHash + "_PhotoToolTipUI";//$NON-NLS-1$

		if (isNewImages) {
			_imageGallery.showImages(_photoWrapperList, galleryPositionKey, false);
		} else {
			// do not hide already displayed images
			_imageGallery.showImages(_photoWrapperList, galleryPositionKey);
		}
	}

	private void updateUI_Colors(final Composite parent) {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		_fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		_bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
//		_fgColor = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
//		_bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
		final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

		final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

		_galleryContainer.setForeground(_fgColor);
		_galleryContainer.setBackground(_bgColor);

		_imageGallery.updateColors(_fgColor, _bgColor, selectionFgColor, noFocusSelectionFgColor, true);

//		updateUI_Colors_ChildColors(parent);
	}

	/**
	 * !!! This is recursive !!!
	 * 
	 * @param child
	 */
	private void updateUI_Colors_ChildColors(final Control child) {

		child.setBackground(_bgColor);
		child.setForeground(_fgColor);

		if (child instanceof Composite) {

			final Control[] children = ((Composite) child).getChildren();

			for (final Control element : children) {

				if (element != null && element.isDisposed() == false) {
					updateUI_Colors_ChildColors(element);
				}
			}
		}
	}

	private void updateUI_ToogleAction() {

		if (_isVerticalGallery) {

			_actionToggleGalleryOrientation.setToolTipText(//
					Messages.Photo_Tooltip_Action_ToggleGalleryHorizontal_ToolTip);

			_actionToggleGalleryOrientation.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__PhotoGalleryHorizontal));

		} else {

			_actionToggleGalleryOrientation.setToolTipText(//
					Messages.Photo_Tooltip_Action_ToggleGalleryVertical_ToolTip);

			_actionToggleGalleryOrientation.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__PhotoGalleryVertical));
		}
	}
}
