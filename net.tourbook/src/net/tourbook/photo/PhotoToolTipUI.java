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
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.ui.tourChart.ChartPhoto;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Photo tooltip UI for a tour chart or map or something unknown
 * 
 * @author Wolfgang Schramm, created 3.8.2012
 */

public abstract class PhotoToolTipUI extends PhotoToolTipShell {

	private static final String				STATE_PHOTO_GALLERY_IS_VERTICAL	= "STATE_PHOTO_GALLERY_IS_VERTICAL";	//$NON-NLS-1$
	private static final String				STATE_TOOL_TIP_LOCATION			= "STATE_TOOL_TIP_LOCATION";			//$NON-NLS-1$

	private IDialogSettings					_state;

	private int								_displayedPhotosHash;

	private final ArrayList<PhotoWrapper>	_photoWrapperList				= new ArrayList<PhotoWrapper>();

	private ActionCloseToolTip				_actionCloseToolTip;
	private ActionPinToolTip				_actionPinToolTip;
	private ActionToggleGalleryOrientation	_actionToggleGalleryOrientation;
	private ActionToolTipLocationUpDown		_actionToolTipLocation;

	private ToolBarManager					_galleryToolbarManager;
	private boolean							_isVerticalGallery;
	private boolean							_isShellDragged;

	/**
	 * <pre>
	 * 1 ... above tour chart
	 * 0 ... below tour chart
	 * </pre>
	 */
	private int								_toolTipLocationUpDown;

	private int								_devXMousedown;
	private int								_devYMousedown;

	/*
	 * UI controls
	 */
	private Composite						_galleryContainer;

	private PhotoGallery					_photoGallery;
	private ToolBar							_ttToolbarControlExit;

	private ToolBar							_galleryToolbarControl;
	private Label							_labelDragToolTip;
	private Cursor							_cursorResize;

	private Cursor							_cursorHand;

	private class ActionCloseToolTip extends Action {

		public ActionCloseToolTip() {

			super(null, Action.AS_PUSH_BUTTON);

			setToolTipText(Messages.App_Action_Close_ToolTip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Close));
		}

		@Override
		public void run() {
			actionCloseToolTip();
		}
	}

	private class ActionPinToolTip extends Action {

		public ActionPinToolTip() {

			super(null, Action.AS_CHECK_BOX);

			setToolTipText(Messages.Photo_Tooltip_Action_PinToolTip_ToolTip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Pin_Blue));
		}

		@Override
		public void run() {
			actionPinToolTip(_actionPinToolTip.isChecked());
		}
	}

	private class ActionToggleGalleryOrientation extends Action {

		public ActionToggleGalleryOrientation() {
			super(null, Action.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			actionToggleVH();
		}
	}

	private class ActionToolTipLocationUpDown extends Action {

		public ActionToolTipLocationUpDown() {

			super(null, Action.AS_PUSH_BUTTON);

			setToolTipText(Messages.App_Action_ToolTipLocation_AboveTourChart_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));
		}

		@Override
		public void run() {
			actionToolTipLocation();
		}
	}

	private final class PhotoGalleryProvider implements IPhotoGalleryProvider {

		@Override
		public IStatusLineManager getStatusLineManager() {
			return null;
		}

		@Override
		public IToolBarManager getToolBarManager() {
			return _galleryToolbarManager;
		}

		@Override
		public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

		@Override
		public void setSelection(final PhotoSelection photoSelection) {
			onSelectPhoto(photoSelection);
		}
	}

	public PhotoToolTipUI(final Control ownerControl) {

		super(ownerControl);

		_cursorResize = new Cursor(ownerControl.getDisplay(), SWT.CURSOR_SIZEALL);
		_cursorHand = new Cursor(ownerControl.getDisplay(), SWT.CURSOR_HAND);

		ownerControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		createActions();
	}

	private void actionCloseToolTip() {
		ttHide();
	}

	private void actionToggleVH() {

		// keep state for current orientation
		_photoGallery.saveState(_state);

		// toggle gallery
		_isVerticalGallery = !_isVerticalGallery;

		updateUI_ToogleAction();

		/*
		 * set tooltip shell to the correct size, each orientation has it's own size
		 */
		final Point contentSize = getContentSize();

		// set size in layout data
		final GridData gd = (GridData) _galleryContainer.getLayoutData();
		gd.widthHint = contentSize.x;
		gd.heightHint = contentSize.y;

		// relayout shell
		getToolTipShell().pack(true);

		// show shell at default location
		showAtDefaultLocation();

		setIsShellToggle();

		_photoGallery.setVertical(_isVerticalGallery);
	}

	private void actionToolTipLocation() {

		_toolTipLocationUpDown = _toolTipLocationUpDown == 1 ? 0 : 1;

		updateUI_ToolTipLocation();

		super.doNotStopAnimation();

		showShell();
	}

	@Override
	protected void afterCreateShell(final Shell shell) {

		if (_state == null) {
			// this happened when testing
			return;
		}

		/*
		 * restore MUST be done after the shell is created, otherwise clientArea() can return 0
		 * values
		 */
		_photoGallery.restoreState(_state);

		// set gallery orientation
		_photoGallery.setVertical(_isVerticalGallery);

		/**
		 * Prevent to open pref dialog, when it's opened it would close this tooltip and the pref
		 * dialog is hidden -->> APP IS FREEZING !!!
		 */
		_photoGallery.setShowOtherShellActions(false);

		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {

				if (_photoGallery.isDisposed()) {
					// this happens by multiple shells
					return;
				}
				_photoGallery.saveState(_state);
			}
		});
	}

	@Override
	void beforeHideToolTip() {

		// force to show the same images

		_displayedPhotosHash = Integer.MIN_VALUE;
	}

	private void createActions() {

		_actionPinToolTip = new ActionPinToolTip();
		_actionToggleGalleryOrientation = new ActionToggleGalleryOrientation();
		_actionCloseToolTip = new ActionCloseToolTip();
		_actionToolTipLocation = new ActionToolTipLocationUpDown();
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite container = createUI(parent);

		updateUI_Colors(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(2, 2).applyTo(shellContainer);
		shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_Gallery(shellContainer);
		}

		createUI_20_ActionBar(_photoGallery.getCustomActionBarContainer());

		fillActionBar();

		// must be called after the custom action bar is created
		_photoGallery.createActionBar();
		_galleryToolbarManager.update(false);

		return shellContainer;
	}

	private void createUI_10_Gallery(final Composite parent) {

		_galleryContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_galleryContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.extendedMargins(0, 0, 0, 0)
				.applyTo(_galleryContainer);
		_galleryContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_photoGallery = new PhotoGallery();

			_photoGallery.setShowCustomActionBar();
			_photoGallery.setShowThumbnailSize();

			_photoGallery.createPhotoGallery(
					_galleryContainer,
					SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI,
					new PhotoGalleryProvider());
		}
	}

	private void createUI_20_ActionBar(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * create toolbar for the exit button
			 */
			_ttToolbarControlExit = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.applyTo(_ttToolbarControlExit);
//			_ttToolbarControlExit.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

			/*
			 * spacer
			 */
			_labelDragToolTip = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.hint(50, SWT.DEFAULT)
					.applyTo(_labelDragToolTip);
			_labelDragToolTip.setText(UI.EMPTY_STRING);
			_labelDragToolTip.setToolTipText(Messages.Photo_Tooltip_Action_MoveToolTip_ToolTip);
//			_labelDragToolTip.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			_labelDragToolTip.addMouseTrackListener(new MouseTrackListener() {

				@Override
				public void mouseEnter(final MouseEvent e) {
					_labelDragToolTip.setCursor(_cursorHand);
				}

				@Override
				public void mouseExit(final MouseEvent e) {}

				@Override
				public void mouseHover(final MouseEvent e) {}
			});

			_labelDragToolTip.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(final MouseEvent e) {
					onMouseDown(e);
				}

				@Override
				public void mouseUp(final MouseEvent e) {
					onMouseUp(e);
				}
			});
			_labelDragToolTip.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(final MouseEvent e) {
					onMouseMove(e);
				}
			});

			/*
			 * create gallery toolbar
			 */
			_galleryToolbarControl = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.END, SWT.FILL)
					.applyTo(_galleryToolbarControl);
		}
	}

	@Override
	protected void enableControls() {

		_actionToolTipLocation.setEnabled(isToolTipPinned() == false);
	}

	private void fillActionBar() {

		/*
		 * fill exit toolbar
		 */
		final ToolBarManager exitToolbarManager = new ToolBarManager(_ttToolbarControlExit);

		exitToolbarManager.add(_actionCloseToolTip);
		exitToolbarManager.add(_actionPinToolTip);
		exitToolbarManager.add(_actionToolTipLocation);

		exitToolbarManager.update(true);

		/*
		 * fill gallery toolbar
		 */
		_galleryToolbarManager = new ToolBarManager(_galleryToolbarControl);

		_galleryToolbarManager.add(_actionToggleGalleryOrientation);
		_galleryToolbarManager.add(new Separator());
	}

	/**
	 * @return Returns current tooltip location
	 *         <p>
	 *         1 ... above tour chart<br>
	 *         0 ... below tour chart
	 */
	protected int getTooltipLocation() {
		return _toolTipLocationUpDown;
	}

	@Override
	protected boolean isToolTipDragged() {
		return _isShellDragged;
	}

	@Override
	boolean isVerticalGallery() {
		return _isVerticalGallery;
	}

	private void onDispose() {

		_cursorResize.dispose();
		_cursorHand.dispose();
	}

	private void onMouseDown(final MouseEvent e) {

		_isShellDragged = true;

		_devXMousedown = e.x;
		_devYMousedown = e.y;

		_labelDragToolTip.setCursor(_cursorResize);
	}

	private void onMouseMove(final MouseEvent e) {

		if (_isShellDragged) {

			// shell is dragged

			final int diffX = _devXMousedown - e.x;
			final int diffY = _devYMousedown - e.y;

			setShellLocation(diffX, diffY);
		}
	}

	private void onMouseUp(final MouseEvent e) {

		if (_isShellDragged) {

			// shell is dragged with the mouse, stop dragging

			_isShellDragged = false;

			_labelDragToolTip.setCursor(null);

			setToolTipPinnedLocation();
		}
	}

	@Override
	protected void onReparentShell(final Shell reparentedShell) {
		_photoGallery.onReparentShell(reparentedShell);
	}

	protected abstract void onSelectPhoto(final PhotoSelection photoSelection);

	@Override
	protected void restoreState(final IDialogSettings state) {

		_state = state;

		_isVerticalGallery = Util.getStateBoolean(state, STATE_PHOTO_GALLERY_IS_VERTICAL, true);
		_toolTipLocationUpDown = Util.getStateInt(state, STATE_TOOL_TIP_LOCATION, 1);

		updateUI_ToogleAction();
		updateUI_ToolTipLocation();

		super.restoreState(state);

		enableControls();
	}

	@Override
	protected void saveState(final IDialogSettings state) {

		state.put(STATE_PHOTO_GALLERY_IS_VERTICAL, _isVerticalGallery);
		state.put(STATE_TOOL_TIP_LOCATION, _toolTipLocationUpDown);

		super.saveState(state);
	}

	@Override
	void setToolTipPinned(final boolean isToolTipPinned) {
		_actionPinToolTip.setChecked(isToolTipPinned);
	}

	protected void showPhotoToolTip(final ArrayList<ChartPhoto> hoveredPhotos) {

		final boolean isPhotoHovered = hoveredPhotos != null && hoveredPhotos.size() > 0;

		final int hoveredPhotosHash = isPhotoHovered ? hoveredPhotos.hashCode() : 0;

		/**
		 * check if new images should be displayed, this check is VERY IMPORTANT otherwise this can
		 * be a performance hog
		 */
		if (_displayedPhotosHash == hoveredPhotosHash) {
			return;
		}

		_displayedPhotosHash = hoveredPhotosHash;

		// display shell
		if (showShell() == false) {
			return;
		}

		/*
		 * display photo images
		 */
		// create list containing all images
		_photoWrapperList.clear();
		for (final ChartPhoto chartPhoto : hoveredPhotos) {
			_photoWrapperList.add(chartPhoto.photoWrapper);
		}

		final String galleryPositionKey = hoveredPhotosHash + "_PhotoToolTipUI";//$NON-NLS-1$

		_photoGallery.showImages(_photoWrapperList, galleryPositionKey);
	}

	private void updateUI_Colors(final Composite parent) {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

//		final Color fgColor = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
//		final Color bgColor = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);

		final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);
		final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

		_galleryContainer.setForeground(fgColor);
		_galleryContainer.setBackground(bgColor);

		_photoGallery.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, true);
	}

	private void updateUI_ToogleAction() {

		if (_isVerticalGallery) {

			_actionToggleGalleryOrientation.setToolTipText(//
					Messages.Photo_Gallery_Action_ToggleGalleryHorizontal_ToolTip);

			_actionToggleGalleryOrientation.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__PhotoGalleryHorizontal));

		} else {

			_actionToggleGalleryOrientation.setToolTipText(//
					Messages.Photo_Gallery_Action_ToggleGalleryVertical_ToolTip);

			_actionToggleGalleryOrientation.setImageDescriptor(//
					TourbookPlugin.getImageDescriptor(Messages.Image__PhotoGalleryVertical));
		}
	}

	private void updateUI_ToolTipLocation() {

		if (_toolTipLocationUpDown == 1) {

			// above tour chart

			_actionToolTipLocation.setToolTipText(Messages.App_Action_ToolTipLocation_BelowTourChart_Tooltip);
			_actionToolTipLocation.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp));

		} else {

			// below tour chart

			_actionToolTipLocation.setToolTipText(Messages.App_Action_ToolTipLocation_AboveTourChart_Tooltip);
			_actionToolTipLocation.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));
		}
	}
}
