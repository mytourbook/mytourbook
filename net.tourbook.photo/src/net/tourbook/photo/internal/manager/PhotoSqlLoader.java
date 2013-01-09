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
package net.tourbook.photo.internal.manager;

import net.tourbook.photo.ILoadCallBack;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoSqlLoadingState;
import net.tourbook.photo.internal.LoadCallbackImage;

public class PhotoSqlLoader {

	private Photo			photo;
	private ILoadCallBack	imageLoadCallback;

	public PhotoSqlLoader(final Photo photo, final ILoadCallBack imageLoadCallback) {

		this.photo = photo;
		this.imageLoadCallback = imageLoadCallback;
	}

	public void loadSql() {

//		Connection conn = null;
//
//		try {
//
//			conn = TourDatabase.getInstance().getConnection();
//
////			final PreparedStatement sqlUpdate = conn.prepareStatement(//
////					//
////					"SELECT " //
////								//
////							+ TourDatabase.TABLE_TOUR_PHOTO //
////							+ " SET" //								//$NON-NLS-1$
////							+ " ratingStars=? " //					//$NON-NLS-1$
////							+ " WHERE photoId=?"); //				//$NON-NLS-1$
////
////			sqlUpdate.setInt(1, ratingStars);
////			sqlUpdate.setLong(2, photoRef.photoId);
////			sqlUpdate.executeUpdate();
//
//		} catch (final SQLException e) {
////			UI.showSQLException(e);
//		} finally {
//
//			if (conn != null) {
//				try {
//					conn.close();
//				} catch (final SQLException e) {
////					UI.showSQLException(e);
//				}
//			}
//		}

//		galleryPhoto.adjustedTime = tourPhoto.getAdjustedTime();
//		galleryPhoto.imageExifTime = tourPhoto.getImageExifTime();
//
//		tourLatitude = tourPhoto.getLatitude();
//
//		galleryPhoto.isGeoFromExif = tourPhoto.isGeoFromPhoto();
//		galleryPhoto.isPhotoWithGps = tourLatitude != 0;
//
//		galleryPhoto.ratingStars = tourPhoto.getRatingStars();
//
//	/*
//	 * when a photo is in the photo cache it is possible that the tour is from the file
//	 * system, update tour relevant fields
//	 */
//	galleryPhoto.isPhotoFromTour = true;
//
//	// ensure this tour is set in the photo
//	galleryPhoto.addTour(tourPhoto.getTourId(), tourPhoto.getPhotoId());
//
//	if (galleryPhoto.getTourLatitude() == 0 && tourLatitude != 0) {
//		galleryPhoto.setTourGeoPosition(tourLatitude, tourPhoto.getLongitude());
//	}

		photo.getSqlLoadingState().set(PhotoSqlLoadingState.IS_LOADED);

		// update UI in the original callback
		if (imageLoadCallback instanceof LoadCallbackImage) {
			((LoadCallbackImage) imageLoadCallback).updateUI();
		}
	}

}
