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
package net.tourbook.data;

import java.io.File;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoWrapper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Contains a photo for a tour.
 * <p>
 * <b>This object is currently not used (version 12.12), it will be used in the next version.</b>
 * 
 * @since 12.12
 */
@Entity
public class TourPhoto {

	public static final int	DB_LENGTH_FILE_PATH	= 260;

	/**
	 * Unique id for the {@link TourPhoto} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long			photoId				= TourDatabase.ENTITY_IS_NOT_SAVED;

	/**
	 * Image filename with extension.
	 */
	private String			imageFileName;

	/**
	 * Image file extension.
	 */
	private String			imageFileExt;

	/**
	 * Image file path without filename.
	 */
	private String			imageFilePath;

	private String			imageFilePathName;

	/**
	 * Exif time in milliseconds, when not available, the last modified time of the image file is
	 * used.
	 */
	private long			imageExifTime;

	/**
	 * Last modified in GMT
	 */
	private long			imageFileLastModified;

	/**
	 *
	 */
	private long			adjustedTime;

	/**
	 * <code>0</code> geo position is from a tour<br>
	 * <code>1</code> geo position is from a photo<br>
	 */
	private int				isGeoFromPhoto;

	/**
	 * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
	 * value is <b>NOT</b> set !!!
	 */
	private double			latitude			= 0;
	private double			longitude			= 0;

	@ManyToOne(optional = false)
	private TourData		tourData;

	/**
	 * unique id for manually created markers because the {@link #photoId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long			_createId			= 0;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int		_createCounter		= 0;

	// constructor is required for hibernate
	public TourPhoto() {}

	public TourPhoto(final TourData tourData, final PhotoWrapper photoWrapper) {

		_createId = ++_createCounter;

		this.tourData = tourData;

		final File imageFile = photoWrapper.imageFile;

		final String filePathName = imageFile.getAbsolutePath();
		final IPath filePath = new Path(filePathName);

		final String fileExtension = filePath.getFileExtension();

		imageFileName = filePath.lastSegment();
		imageFileExt = fileExtension == null ? UI.EMPTY_STRING : fileExtension;
		imageFilePath = filePath.removeLastSegments(1).toOSString();
		imageFilePathName = filePathName;

		imageFileLastModified = imageFile.lastModified();
		imageExifTime = photoWrapper.imageExifTime;

		final long photoAdjustedTime = photoWrapper.adjustedTime;
		adjustedTime = photoAdjustedTime == Long.MIN_VALUE ? imageExifTime : photoAdjustedTime;

		if (photoWrapper.isGeoFromExif) {
			setIsGeoFromPhoto();
		} else {
			setIsGeoFromTour();
		}

		final Photo photo = photoWrapper.photo;

		latitude = photo.getLatitude();
		longitude = photo.getLongitude();
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourPhoto)) {
			return false;
		}

		final TourPhoto other = (TourPhoto) obj;

		if (_createId == 0) {

			// photo is from the database
			if (photoId != other.photoId) {
				return false;
			}
		} else {

			// photo was create
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public long getAdjustedTime() {
		return adjustedTime;
	}

	public long getImageExifTime() {
		return imageExifTime;
	}

	public String getImageFileExt() {
		return imageFileExt;
	}

	public long getImageFileLastModified() {
		return imageFileLastModified;
	}

	public String getImageFileName() {
		return imageFileName;
	}

	public String getImageFilePath() {
		return imageFilePath;
	}

	/**
	 * @return Returns the full filepathname
	 */
	public String getImageFilePathName() {
		return imageFilePathName;
	}

	/**
	 * @return Returns 0 when latitude is <b>NOT</b> set.
	 */
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (photoId ^ (photoId >>> 32));
		return result;
	}

	public boolean isGeoFromPhoto() {
		return isGeoFromPhoto == 1;
	}

	public boolean isGeoFromTour() {
		return isGeoFromPhoto == 0;
	}

	public void setAdjustedTime(final long adjustedTime) {
		this.adjustedTime = adjustedTime;
	}

	public void setGeoLocation(final double latitude, final double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void setIsGeoFromPhoto() {
		isGeoFromPhoto = 0;
	}

	public void setIsGeoFromTour() {
		isGeoFromPhoto = 1;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append(TourPhoto.class.getSimpleName())
				.append(" id:") //$NON-NLS-1$
				.append(photoId)
				.append(" createId:") //$NON-NLS-1$
				.append(_createId)
				.toString();
	}

}
