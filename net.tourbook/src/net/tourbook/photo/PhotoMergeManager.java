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

import net.tourbook.application.PerspectiveFactoryPhoto;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class PhotoMergeManager {

	public static void openPhotoMergePerspective(final MergePhotoTourSelection photoMergeSelection) {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {

				// show photo merge perspective
				workbench.showPerspective(PerspectiveFactoryPhoto.PERSPECTIVE_ID, window);

				// show photo merge tour view
				final PhotosAndToursView view = (PhotosAndToursView) Util.showView(PhotosAndToursView.ID);

				if (view != null) {
					view.updatePhotosAndTours(photoMergeSelection);
				}

			} catch (final PartInitException e) {
				StatusUtil.showStatus(e);
			} catch (final WorkbenchException e) {
				StatusUtil.showStatus(e);
			}
		}
	}
}
