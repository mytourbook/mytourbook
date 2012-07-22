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
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourPhotosView extends ViewPart implements IPhotoGalleryProvider {

	public static final String		ID			= "net.tourbook.photo.merge.TourPhotosView.ID";			//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private final IDialogSettings	_state		= TourbookPlugin.getDefault().getDialogSettingsSection(ID);

	private PostSelectionProvider	_postSelectionProvider;

	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourPropertyListener;
	private IPartListener2			_partListener;

	private boolean					_isPartVisible;

	/**
	 * contains selection which was set when the part is hidden
	 */
	private ISelection				_selectionWhenHidden;

	private ISelection				_currentPhotoSelection;

	private PhotoGallery			_photoGallery;

	/*
	 * UI controls
	 */

	public TourPhotosView() {
		super();
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

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		restoreState();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_photoGallery = new PhotoGallery(container, SWT.H_SCROLL | SWT.MULTI, this);

			_photoGallery.setDefaultStatusMessage(Messages.Tour_Photos_Label_StatusMessage_NoTourWithPhotos);
		}
	}

	@Override
	public void dispose() {

		_photoGallery.dispose();

		final IWorkbenchPage page = getViewSite().getPage();

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		page.removePostSelectionListener(_postSelectionListener);
		page.removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return getViewSite().getActionBars().getStatusLineManager();
	}

	@Override
	public IToolBarManager getToolBarManager() {
		return getViewSite().getActionBars().getToolBarManager();
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

	@Override
	public void registerContextMenu(final String menuId, final MenuManager menuManager) {
//		getSite().registerContextMenu(menuId, menuManager, _postSelectionProvider);
	}

	private void restoreState() {

		updateColors(true);

		_photoGallery.restoreState(_state);
	}

	private void saveState() {

		_photoGallery.saveState(_state);
	}

	@Override
	public void setFocus() {

	}

	@Override
	public void setSelection(final PhotoSelection photoSelection) {
		_postSelectionProvider.setSelection(photoSelection);
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

		final MergeTour mergeTour = tourPhotoSelection.mergeTour;
		final ArrayList<PhotoWrapper> photoWrapperList = mergeTour.tourPhotos;

		final long tourId = mergeTour.tourId;

		final String galleryPositionKey = tourId == Long.MIN_VALUE //
				? Long.toString(mergeTour.tourStartTime)
				: Long.toString(tourId);

		_photoGallery.showImages(photoWrapperList, galleryPositionKey);
	}
}
