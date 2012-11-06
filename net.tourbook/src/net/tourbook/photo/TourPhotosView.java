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
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourPhotosView extends ViewPart {

	public static final String				ID								= "net.tourbook.photo.TourPhotosView.ID";	//$NON-NLS-1$

	private static final String				STATE_PHOTO_GALLERY_IS_VERTICAL	= "STATE_PHOTO_GALLERY_IS_VERTICAL";		//$NON-NLS-1$

	private final IPreferenceStore			_prefStore						= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();
	private static final IDialogSettings	_state							= TourbookPlugin.getDefault()//
																					.getDialogSettingsSection(ID);

	private final DateTimeFormatter			_dtFormatter					= DateTimeFormat.forStyle("SL");			//$NON-NLS-1$

	private PostSelectionProvider			_postSelectionProvider;

	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourPropertyListener;
	private IPartListener2					_partListener;

	private boolean							_isPartVisible;

	private ActionToggleGalleryOrientation	_actionToggleGalleryOrientation;

	/**
	 * contains selection which was set when the part is hidden
	 */
	private ISelection						_selectionWhenHidden;

	private ISelection						_currentPhotoSelection;

	private PhotoGallery					_photoGallery;

	private boolean							_isVerticalGallery;
	public IToolBarManager					_galleryToolbarManager;

	/*
	 * UI controls
	 */
	private ToolBar							_toolbarLeft;
	private Label							_labelTitle;

	private class ActionToggleGalleryOrientation extends Action {

		public ActionToggleGalleryOrientation() {

			super(null, Action.AS_PUSH_BUTTON);

			/**
			 * VERY IMPORTANT
			 * <p>
			 * an image must be set in the constructor, otherwise the button is small when only ONE
			 * action is in the toolbar
			 */
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__PhotoGalleryHorizontal));
		}

		@Override
		public void run() {
			actionToggleVH();
		}
	}

	private final class PhotoGalleryProvider implements IPhotoGalleryProvider {

		@Override
		public IStatusLineManager getStatusLineManager() {
			return getViewSite().getActionBars().getStatusLineManager();
		}

		@Override
		public IToolBarManager getToolBarManager() {
			return getViewSite().getActionBars().getToolBarManager();
		}

		@Override
		public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

		@Override
		public void setSelection(final PhotoSelection photoSelection) {
			_postSelectionProvider.setSelection(photoSelection);
		}
	}

	public TourPhotosView() {
		super();
	}

	private void actionToggleVH() {

		// keep state for current orientation
		_photoGallery.saveState(_state);

		// toggle gallery
		_isVerticalGallery = !_isVerticalGallery;

		updateUI_ToogleAction();

		_photoGallery.setVertical(_isVerticalGallery);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotosView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotosView.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourPhotosView.this) {

					_isPartVisible = true;

					if (_selectionWhenHidden != null) {

						onSelectionChanged(_selectionWhenHidden);

						_selectionWhenHidden = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)) {

					updateColors(false);
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourPhotosView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == TourPhotosView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {

					// check if a tour must be updated

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void clearView() {

	}

	private void createActions() {

		_actionToggleGalleryOrientation = new ActionToggleGalleryOrientation();
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		createActions();
		fillActionBar();

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		restoreState();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());
	}

	private void createUI(final Composite parent) {

		createUI_10_Gallery(parent);
		createUI_20_ActionBar(_photoGallery.getCustomActionBarContainer());

		// must be called after the custom action bar is created
		_photoGallery.createActionBar();
	}

	private void createUI_10_Gallery(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_photoGallery = new PhotoGallery();

			_photoGallery.setShowCustomActionBar();
			_photoGallery.setShowThumbnailSize();

			_photoGallery.createPhotoGallery(
					container,
					SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI,
					new PhotoGalleryProvider());

			_photoGallery.setDefaultStatusMessage(Messages.Tour_Photos_Label_StatusMessage_NoTourWithPhotos);
		}
	}

	private void createUI_20_ActionBar(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * label: title
			 */
			_labelTitle = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.CENTER)
//					.hint(20, SWT.DEFAULT)
					.applyTo(_labelTitle);
//			_labelTitle.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

			/*
			 * create toolbar for the buttons on the left side
			 */
			_toolbarLeft = new ToolBar(container, SWT.FLAT);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
//					.grab(false, true)
					.applyTo(_toolbarLeft);
//			_toolbarLeft.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		}
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getViewSite().getPage();

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBar() {

		/*
		 * fill gallery toolbar
		 */
		_galleryToolbarManager = new ToolBarManager(_toolbarLeft);

		_galleryToolbarManager.add(_actionToggleGalleryOrientation);

		_galleryToolbarManager.update(true);
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_isPartVisible == false) {

			if (selection instanceof TourPhotoSelection) {
				_selectionWhenHidden = selection;
			}
			return;
		}

		if (selection instanceof TourPhotoSelection) {

			if (_currentPhotoSelection == selection) {
				// prevent setting the same selection again
				return;
			}

			_currentPhotoSelection = selection;

			final TourPhotoSelection tourPhotoSelection = (TourPhotoSelection) selection;

			updateUI(tourPhotoSelection);
		}
	}

	private void restoreState() {

		updateColors(true);

		_photoGallery.restoreState(_state);

		// set gallery orientation
		_isVerticalGallery = Util.getStateBoolean(_state, STATE_PHOTO_GALLERY_IS_VERTICAL, true);
		_photoGallery.setVertical(_isVerticalGallery);

		updateUI_ToogleAction();
	}

	private void saveState() {

		_state.put(STATE_PHOTO_GALLERY_IS_VERTICAL, _isVerticalGallery);

		_photoGallery.saveState(_state);
	}

	@Override
	public void setFocus() {

	}

	private void updateColors(final boolean isRestore) {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
		final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

		final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

//		tree.setForeground(fgColor);
//		tree.setBackground(bgColor);

		_photoGallery.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, isRestore);
	}

	private void updateUI(final TourPhotoSelection tourPhotoSelection) {

		/*
		 * update photo gallery
		 */
		final MergeTour mergeTour = tourPhotoSelection.mergedTour;
		final ArrayList<PhotoWrapper> photoWrapperList = mergeTour.tourPhotos;

		final String galleryPositionKey = Long.toString(mergeTour.mergeId);

		_photoGallery.showImages(photoWrapperList, galleryPositionKey + " TourPhotosView", true);//$NON-NLS-1$

		/*
		 * set title
		 */
		final int size = photoWrapperList.size();
		String labelText;

		if (size == 1) {
			labelText = _dtFormatter.print(mergeTour.tourStartTime);
		} else if (size > 1) {
			labelText = _dtFormatter.print(mergeTour.tourStartTime)
					+ UI.DASH_WITH_DOUBLE_SPACE
					+ _dtFormatter.print(mergeTour.tourEndTime);
		} else {
			labelText = UI.EMPTY_STRING;
		}

		_labelTitle.setText(labelText);
		_labelTitle.setToolTipText(labelText);
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
}
