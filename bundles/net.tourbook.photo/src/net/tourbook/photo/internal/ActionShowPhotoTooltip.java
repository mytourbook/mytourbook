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

import net.tourbook.photo.PhotoGallery;

import org.eclipse.jface.action.Action;

public class ActionShowPhotoTooltip extends Action {

	private PhotoGallery	_photoGallery;

	public ActionShowPhotoTooltip(final PhotoGallery photoGallery) {

		super(Messages.Pic_Dir_Action_ShowPhotoTooltip, AS_CHECK_BOX);

		_photoGallery = photoGallery;

		setToolTipText(Messages.Pic_Dir_Action_ShowPhotoTooltip_Tooltip);

		setImageDescriptor(Activator.getImageDescriptor(Messages.Image__PhotoTooltip));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.Image__PhotoTooltip_Disabled));
	}

	@Override
	public void run() {
		_photoGallery.actionShowPhotoInfo(this);
	}
}
