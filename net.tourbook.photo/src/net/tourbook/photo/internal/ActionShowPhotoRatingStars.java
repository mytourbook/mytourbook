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
package net.tourbook.photo.internal;

import net.tourbook.common.UI;
import net.tourbook.photo.PhotoGallery;

import org.eclipse.jface.action.Action;

public class ActionShowPhotoRatingStars extends Action {

	private PhotoGallery	_photoGallery;

	public ActionShowPhotoRatingStars(final PhotoGallery photoGallery) {

		super(UI.EMPTY_STRING, AS_CHECK_BOX);

		_photoGallery = photoGallery;

		setToolTipText(Messages.Photo_Gallery_Action_ShowPhotoRatingStars_Tooltip);
		setImageDescriptor(Activator.getImageDescriptor(Messages.Image__PhotoRatingStar));
	}

	@Override
	public void run() {
		_photoGallery.actionShowPhotoRatingStars();
	}
}
