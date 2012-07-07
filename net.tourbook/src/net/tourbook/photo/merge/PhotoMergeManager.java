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
package net.tourbook.photo.merge;

import java.util.ArrayList;
import java.util.Collection;

import net.tourbook.application.PerspectiveFactoryPhoto;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class PhotoMergeManager {

	public static void openPhotoMergePerspective(	final Collection<GalleryMT20Item> selectedImages,
													final ArrayList<Long> selectedTours) {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {

				// show photo merge perspective
				workbench.showPerspective(PerspectiveFactoryPhoto.PERSPECTIVE_ID, window);

				final PhotoMergeView view = (PhotoMergeView) Util.showView(PhotoMergeView.ID);

				if (view != null) {
//					view.reloadViewer();
				}

			} catch (final PartInitException e) {
				StatusUtil.showStatus(e);
			} catch (final WorkbenchException e) {
				StatusUtil.showStatus(e);
			}
		}
	}
}
