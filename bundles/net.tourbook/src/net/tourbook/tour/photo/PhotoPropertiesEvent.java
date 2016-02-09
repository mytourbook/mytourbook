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
package net.tourbook.tour.photo;

public class PhotoPropertiesEvent {

	/**
	 * Number of selected rating stars.
	 */
	public int	filterRatingStars;

	/**
	 * Can be one of these operators:
	 * <p>
	 * {@link DialogPhotoProperties#OPERATOR_IS_EQUAL}, {@link DialogPhotoProperties#OPERATOR_IS_LESS_OR_EQUAL} or
	 * {@link DialogPhotoProperties#OPERATOR_IS_MORE_OR_EQUAL}
	 */
	public int	fiterRatingStarOperator;

	@Override
	public String toString() {
		return "PhotoPropertiesEvent [ratingStars=" //$NON-NLS-1$
				+ filterRatingStars
				+ "{)}, ratingStarOperator=" //$NON-NLS-1$
				+ fiterRatingStarOperator
				+ "]"; //$NON-NLS-1$
	}

}
