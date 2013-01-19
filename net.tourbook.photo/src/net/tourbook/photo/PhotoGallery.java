/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.ActionPhotoFilterGPS;
import net.tourbook.photo.internal.ActionPhotoFilterNoGPS;
import net.tourbook.photo.internal.ActionPhotoFilterNoTour;
import net.tourbook.photo.internal.ActionPhotoFilterTour;
import net.tourbook.photo.internal.ActionPhotoGalleryType;
import net.tourbook.photo.internal.ActionShowAnnotations;
import net.tourbook.photo.internal.ActionShowPhotoDate;
import net.tourbook.photo.internal.ActionShowPhotoName;
import net.tourbook.photo.internal.ActionShowPhotoRatingStars;
import net.tourbook.photo.internal.ActionShowPhotoTooltip;
import net.tourbook.photo.internal.ActionSortByFileDate;
import net.tourbook.photo.internal.ActionSortByFileName;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.GalleryType;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.PhotoDateInfo;
import net.tourbook.photo.internal.PhotoFilterGPS;
import net.tourbook.photo.internal.PhotoFilterTour;
import net.tourbook.photo.internal.RatingStarBehaviour;
import net.tourbook.photo.internal.manager.GallerySorting;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;

public class PhotoGallery extends ImageGallery {

	private static final String			STATE_GALLERY_SORTING				= "STATE_GALLERY_SORTING";					//$NON-NLS-1$
	private static final String			STATE_GALLERY_TYPE					= "STATE_GALLERY_TYPE";					//$NON-NLS-1$
	private static final String			STATE_PHOTO_FILTER_GPS				= "STATE_PHOTO_FILTER_GPS";				//$NON-NLS-1$
	private static final String			STATE_PHOTO_FILTER_TOUR				= "STATE_PHOTO_FILTER_TOUR";				//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_GPS_ANNOTATION	= "STATE_IS_SHOW_PHOTO_GPS_ANNOTATION";	//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY	= "STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY";	//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_TOOLTIP			= "STATE_IS_SHOW_PHOTO_TOOLTIP";			//$NON-NLS-1$
	private static final String			STATE_PHOTO_INFO_DATE				= "STATE_PHOTO_INFO_DATE";					//$NON-NLS-1$
	private static final String			STATE_RATING_STAR_BEHAVIOUR			= "STATE_RATING_STAR_BEHAVIOUR";			//$NON-NLS-1$

	private IDialogSettings				_state;
	private PhotoDateInfo				_photoDateInfo;

	private boolean						_isShowActionFiltering				= true;
	private boolean						_isShowActionSorting				= true;

	private ActionPhotoFilterGPS		_actionPhotoFilterGPS;
	private ActionPhotoFilterNoGPS		_actionPhotoFilterNoGPS;
	private ActionPhotoFilterTour		_actionPhotoFilterTour;
	private ActionPhotoFilterNoTour		_actionPhotoFilterNoTour;
	private ActionPhotoGalleryType		_actionPhotoGalleryType;
	private ActionShowPhotoName			_actionShowPhotoName;
	private ActionShowPhotoDate			_actionShowPhotoDate;
	private ActionShowPhotoRatingStars	_actionShowPhotoRatingStars;
	private ActionShowPhotoTooltip		_actionShowPhotoTooltip;
	private ActionShowAnnotations		_actionShowGPSAnnotation;
	private ActionSortByFileDate		_actionSortFileByDate;
	private ActionSortByFileName		_actionSortByFileName;

	private PhotoFilterGPS				_photoFilterGPS						= PhotoFilterGPS.NO_FILTER;
	private PhotoFilterTour				_photoFilterTour					= PhotoFilterTour.NO_FILTER;

	private GallerySorting				_gallerySorting;
	private GalleryType					_galleryType;

	private RatingStarBehaviour			_ratingStarBehaviour;

	public PhotoGallery(final IDialogSettings state) {

		super(state);

		_state = state;
	}

	public void actionImageFilterGPS(final Action filterAction) {

		/*
		 * get selected filter, uncheck other
		 */
		if (filterAction == _actionPhotoFilterGPS) {

			_photoFilterGPS = filterAction.isChecked() ? PhotoFilterGPS.WITH_GPS : PhotoFilterGPS.NO_FILTER;

			_actionPhotoFilterNoGPS.setChecked(false);

		} else if (filterAction == _actionPhotoFilterNoGPS) {

			_photoFilterGPS = filterAction.isChecked() ? PhotoFilterGPS.NO_GPS : PhotoFilterGPS.NO_FILTER;

			_actionPhotoFilterGPS.setChecked(false);
		}

		// update gallery

		filterGallery(_photoFilterGPS, _photoFilterTour);
	}

	public void actionImageFilterTour(final Action filterAction) {

		/*
		 * get selected filter, uncheck other
		 */
		if (filterAction == _actionPhotoFilterTour) {

			_photoFilterTour = filterAction.isChecked() ? PhotoFilterTour.WITH_TOURS : PhotoFilterTour.NO_FILTER;

			_actionPhotoFilterNoTour.setChecked(false);

		} else if (filterAction == _actionPhotoFilterNoTour) {

			_photoFilterTour = filterAction.isChecked() ? PhotoFilterTour.NO_TOURS : PhotoFilterTour.NO_FILTER;

			_actionPhotoFilterTour.setChecked(false);
		}

		// update gallery

		filterGallery(_photoFilterGPS, _photoFilterTour);
	}

	public void actionPhotoGalleryType() {

		/*
		 * toggle gallery type
		 */
		if (_galleryType == GalleryType.THUMBNAIL) {
			_galleryType = GalleryType.DETAILS;
		} else {
			_galleryType = GalleryType.THUMBNAIL;
		}
		updateUI_GalleryType(_galleryType);

		selectGalleryType(_galleryType);
	}

	public void actionShowPhotoInfo(final Action action) {

		if (action == _actionShowPhotoDate) {

			// toggle date info

			if (_photoDateInfo == PhotoDateInfo.NoDateTime) {

				// nothing -> date

				_photoDateInfo = PhotoDateInfo.Date;

			} else if (_photoDateInfo == PhotoDateInfo.Date) {

				// date -> time

				_photoDateInfo = PhotoDateInfo.Time;

			} else if (_photoDateInfo == PhotoDateInfo.Time) {

				// time -> date/time

				_photoDateInfo = PhotoDateInfo.DateTime;

			} else {

				// time -> nothing

				_photoDateInfo = PhotoDateInfo.NoDateTime;
			}

			_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		}

		showInfo(//
				_actionShowPhotoName.isChecked(),
				_photoDateInfo,
				_actionShowGPSAnnotation.isChecked(),
				_actionShowPhotoTooltip.isChecked());
	}

	public void actionShowPhotoRatingStars() {

		/*
		 * toggle rating stars
		 */
		if (_ratingStarBehaviour == RatingStarBehaviour.NO_STARS) {

			_ratingStarBehaviour = RatingStarBehaviour.HOVERED_STARS;

		} else if (_ratingStarBehaviour == RatingStarBehaviour.HOVERED_STARS) {

			_ratingStarBehaviour = RatingStarBehaviour.NO_HOVERED_STARS;

		} else {

			_ratingStarBehaviour = RatingStarBehaviour.NO_STARS;
		}

		updateUI_RatingStarBehaviour(_ratingStarBehaviour);

		setShowPhotoRatingStars(_ratingStarBehaviour);
	}

	public void actionSortByDate() {

		final boolean isChecked = _actionSortFileByDate.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortByFileName.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortByFileName.setChecked(true);
		}

		sortGallery(_gallerySorting);
	}

	public void actionSortByName() {

		final boolean isChecked = _actionSortByFileName.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortFileByDate.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortFileByDate.setChecked(true);
		}

		sortGallery(_gallerySorting);
	}

	/**
	 * Creates action bar with default actions.
	 */
	public void createActionBar() {
		createActions();
		fillActionBars();
	}

	private void createActions() {

		_actionPhotoGalleryType = new ActionPhotoGalleryType(this);

		_actionPhotoFilterGPS = new ActionPhotoFilterGPS(this);
		_actionPhotoFilterNoGPS = new ActionPhotoFilterNoGPS(this);
		_actionPhotoFilterTour = new ActionPhotoFilterTour(this);
		_actionPhotoFilterNoTour = new ActionPhotoFilterNoTour(this);

		_actionShowGPSAnnotation = new ActionShowAnnotations(this);

		_actionShowPhotoName = new ActionShowPhotoName(this);
		_actionShowPhotoDate = new ActionShowPhotoDate(this);
		_actionShowPhotoRatingStars = new ActionShowPhotoRatingStars(this);
		_actionShowPhotoTooltip = new ActionShowPhotoTooltip(this);

		_actionSortByFileName = new ActionSortByFileName(this);
		_actionSortFileByDate = new ActionSortByFileDate(this);
	}

	public void createPhotoGallery(	final Composite parent,
									final int style,
									final IPhotoGalleryProvider photoGalleryProvider) {

		super.createImageGallery(parent, style, photoGalleryProvider);
	}

	@Override
	protected void enableActions(final boolean isItemAvailable) {

		_actionPhotoGalleryType.setEnabled(isItemAvailable);

		_actionPhotoFilterGPS.setEnabled(isItemAvailable);
		_actionPhotoFilterNoGPS.setEnabled(isItemAvailable);
		_actionPhotoFilterTour.setEnabled(isItemAvailable);
		_actionPhotoFilterNoTour.setEnabled(isItemAvailable);

		_actionShowGPSAnnotation.setEnabled(isItemAvailable);

		_actionShowPhotoTooltip.setEnabled(isItemAvailable);

		_actionSortByFileName.setEnabled(isItemAvailable);
		_actionSortFileByDate.setEnabled(isItemAvailable);
	}

	@Override
	protected void enableAttributeActions(final boolean isAttributesPainted) {

		_actionShowPhotoRatingStars.setEnabled(isAttributesPainted);
		_actionShowPhotoDate.setEnabled(isAttributesPainted);
		_actionShowPhotoName.setEnabled(isAttributesPainted);
	}

	/**
	 * fill view toolbar
	 */
	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = _photoGalleryProvider.getToolBarManager();

		if (tbm == null) {
			return;
		}

// disabled 2.1.2013: is not yet fully implemented, delayed for a later version
//		tbm.add(_actionPhotoGalleryType);

		tbm.add(new Separator());
		tbm.add(_actionShowPhotoTooltip);

		tbm.add(new Separator());
		tbm.add(_actionShowPhotoRatingStars);
		tbm.add(_actionShowGPSAnnotation);
		tbm.add(_actionShowPhotoDate);
		tbm.add(_actionShowPhotoName);

		if (_isShowActionFiltering) {
			tbm.add(new Separator());
			tbm.add(_actionPhotoFilterTour);
			tbm.add(_actionPhotoFilterNoTour);
			tbm.add(_actionPhotoFilterGPS);
			tbm.add(_actionPhotoFilterNoGPS);
		}

		if (_isShowActionSorting) {
			tbm.add(new Separator());
			tbm.add(_actionSortFileByDate);
			tbm.add(_actionSortByFileName);
		}
	}

	public void hideActionFiltering() {
		_isShowActionFiltering = false;
	}

	public void hideActionSorting() {
		_isShowActionSorting = false;
	}

	@Override
	public void restoreState() {

		/*
		 * photo filter: gps
		 */
		final String prefPhotoFilterGPS = Util.getStateString(
				_state,
				STATE_PHOTO_FILTER_GPS,
				PhotoFilterGPS.NO_FILTER.name());
		try {
			_photoFilterGPS = PhotoFilterGPS.valueOf(prefPhotoFilterGPS);
		} catch (final Exception e) {
			_photoFilterGPS = PhotoFilterGPS.NO_FILTER;
		}
		_actionPhotoFilterGPS.setChecked(_photoFilterGPS == PhotoFilterGPS.WITH_GPS);
		_actionPhotoFilterNoGPS.setChecked(_photoFilterGPS == PhotoFilterGPS.NO_GPS);

		/*
		 * photo filter: tour
		 */
		final String prefPhotoFilterTour = Util.getStateString(
				_state,
				STATE_PHOTO_FILTER_TOUR,
				PhotoFilterTour.NO_FILTER.name());
		try {
			_photoFilterTour = PhotoFilterTour.valueOf(prefPhotoFilterTour);
		} catch (final Exception e) {
			_photoFilterTour = PhotoFilterTour.NO_FILTER;
		}
		_actionPhotoFilterTour.setChecked(_photoFilterTour == PhotoFilterTour.WITH_TOURS);
		_actionPhotoFilterNoTour.setChecked(_photoFilterTour == PhotoFilterTour.NO_TOURS);

		/*
		 * photo date / time / name / tooltip / annotation
		 */
		final PhotoDateInfo photoDateDefault = PhotoDateInfo.NoDateTime;
		final String prefDateInfo = Util.getStateString(_state, STATE_PHOTO_INFO_DATE, photoDateDefault.name());
		try {
			_photoDateInfo = PhotoDateInfo.valueOf(prefDateInfo);
		} catch (final Exception e) {
			_photoDateInfo = photoDateDefault;
		}

		final boolean isShowPhotoName = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, false);
		final boolean isShowTooltip = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_TOOLTIP, true);
		final boolean isShowPhotoAnnotations = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_PHOTO_GPS_ANNOTATION,
				true);

		_actionShowGPSAnnotation.setChecked(isShowPhotoAnnotations);
		_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		_actionShowPhotoName.setChecked(isShowPhotoName);
		_actionShowPhotoTooltip.setChecked(isShowTooltip);

		setPhotoInfo(isShowPhotoName, _photoDateInfo, isShowPhotoAnnotations, isShowTooltip);

		/*
		 * gallery sorting
		 */
		final String prefSorting = Util.getStateString(_state, STATE_GALLERY_SORTING, GallerySorting.FILE_DATE.name());
		try {
			_gallerySorting = GallerySorting.valueOf(prefSorting);
		} catch (final Exception e) {
			_gallerySorting = GallerySorting.FILE_DATE;
		}
		_actionSortFileByDate.setChecked(_gallerySorting == GallerySorting.FILE_DATE);
		_actionSortByFileName.setChecked(_gallerySorting == GallerySorting.FILE_NAME);

		/*
		 * gallery type
		 */
		final String prefGalleryType = Util.getStateString(_state, STATE_GALLERY_TYPE, GalleryType.THUMBNAIL.name());
		try {
			_galleryType = GalleryType.valueOf(prefGalleryType);
		} catch (final Exception e) {
			// set default
			_galleryType = GalleryType.THUMBNAIL;
		}
		updateUI_GalleryType(_galleryType);
		selectGalleryType(_galleryType);

		/*
		 * rating star behaviour
		 */
		final String stateValue = Util.getStateString(
				_state,
				STATE_RATING_STAR_BEHAVIOUR,
				RatingStarBehaviour.HOVERED_STARS.name());
		try {
			_ratingStarBehaviour = RatingStarBehaviour.valueOf(stateValue);
		} catch (final Exception e) {
			// set default
			_ratingStarBehaviour = RatingStarBehaviour.HOVERED_STARS;
		}
		updateUI_RatingStarBehaviour(_ratingStarBehaviour);
		setShowPhotoRatingStars(_ratingStarBehaviour);

		super.restoreState();

		// !!! overwrite super settings !!!
		setSorting(_gallerySorting);
		setFilter(_photoFilterGPS, _photoFilterTour);
	}

	@Override
	public void saveState() {

		_state.put(STATE_GALLERY_SORTING, _actionSortFileByDate.isChecked()
				? GallerySorting.FILE_DATE.name()
				: GallerySorting.FILE_NAME.name());

		_state.put(STATE_GALLERY_TYPE, _galleryType == GalleryType.DETAILS
				? GalleryType.DETAILS.name()
				: GalleryType.THUMBNAIL.name());

		_state.put(STATE_IS_SHOW_PHOTO_GPS_ANNOTATION, _actionShowGPSAnnotation.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, _actionShowPhotoName.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_TOOLTIP, _actionShowPhotoTooltip.isChecked());

		_state.put(STATE_PHOTO_FILTER_GPS, _photoFilterGPS.name());
		_state.put(STATE_PHOTO_FILTER_TOUR, _photoFilterTour.name());

		_state.put(STATE_PHOTO_INFO_DATE, _photoDateInfo.name());
		_state.put(STATE_RATING_STAR_BEHAVIOUR, _ratingStarBehaviour.name());

		super.saveState();
	}

	private void updateUI_GalleryType(final GalleryType galleryType) {

		String text;
		String toolTipText;
		ImageDescriptor imageDescriptor;

		if (galleryType == GalleryType.DETAILS) {

			text = Messages.Photo_Gallery_Action_PhotoGalleryThumbnail;
			toolTipText = Messages.Photo_Gallery_Action_PhotoGalleryThumbnail_Tooltip;
			imageDescriptor = Activator.getImageDescriptor(Messages.Image__PotoGalleryThumbnail);

		} else {

			// thumbnail view

			text = Messages.Photo_Gallery_Action_ShowPhotoGalleryDetails;
			toolTipText = Messages.Photo_Gallery_Action_ShowPhotoGalleryDetails_Tooltip;
			imageDescriptor = Activator.getImageDescriptor(Messages.Image__PotoGalleryDetails);
		}

		_actionPhotoGalleryType.setText(text);
		_actionPhotoGalleryType.setToolTipText(toolTipText);
		_actionPhotoGalleryType.setImageDescriptor(imageDescriptor);
	}

	private void updateUI_RatingStarBehaviour(final RatingStarBehaviour ratingStarBehaviour) {

		String toolTipText;
		ImageDescriptor imageDescriptor;

		if (ratingStarBehaviour == RatingStarBehaviour.NO_STARS) {

			toolTipText = Messages.Photo_Gallery_Action_ShowPhotoRatingStars_NoStars_Tooltip;
			imageDescriptor = Activator.getImageDescriptor(Messages.Image__PhotoRatingStarAndHovered);

		} else if (ratingStarBehaviour == RatingStarBehaviour.NO_HOVERED_STARS) {

			toolTipText = Messages.Photo_Gallery_Action_ShowPhotoRatingStars_NoHoveredStars_Tooltip;
			imageDescriptor = Activator.getImageDescriptor(Messages.Image__PhotoRatingStars);

		} else {

			// set default: RatingStarBehaviour.HOVERED_STARS

			toolTipText = Messages.Photo_Gallery_Action_ShowPhotoRatingStars_Tooltip;
			imageDescriptor = Activator.getImageDescriptor(Messages.Image__PhotoRatingStarsHovered);
		}

		final boolean isShowRatingStars = ratingStarBehaviour != RatingStarBehaviour.NO_STARS;

		_actionShowPhotoRatingStars.setChecked(isShowRatingStars);
		_actionShowPhotoRatingStars.setToolTipText(toolTipText);
		_actionShowPhotoRatingStars.setImageDescriptor(imageDescriptor);
	}

}
