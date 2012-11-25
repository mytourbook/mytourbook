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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

public class ActionHandlerSyncPhotoWithTour extends AbstractHandler {

	static final String		COMMAND_ID				= "command.net.tourbook.PicDir.SyncPhotoWithTour"; //$NON-NLS-1$

	private boolean			_isInitializedState;
	private boolean			_isInitializedView;

	private boolean			_isSyncPhotoWithTour;

	private SyncSelection	_syncSelectionProvider	= new SyncSelection();

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		_isSyncPhotoWithTour = !HandlerUtil.toggleCommandState(event.getCommand());

//		System.out.println(UI.timeStampNano() + " _isSyncPhotoWithTour " + _isSyncPhotoWithTour);
//		// TODO remove SYSTEM.OUT.PRINTLN
//
		final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
		if (activePart instanceof PicDirView) {

			final PicDirView picDirView = (PicDirView) activePart;

			picDirView.setSelectionConverter(_isSyncPhotoWithTour ? _syncSelectionProvider : null);

			if (_isSyncPhotoWithTour) {

				// sync photo with tour

				final PhotosWithExifSelection selectedPhotosWithExif = picDirView.getSelectedPhotosWithExif(false);

				if (selectedPhotosWithExif != null) {
					PhotoManager.getInstance().linkPhotosWithTours(selectedPhotosWithExif);
				}
			} else {

				// don't create tour photo links when sync is disabled

//				picDirView.fireCurrentSelection();
			}
		}

		return null;
	}

	/*
	 * this method is used to initialize the pic dir view with the current state
	 */
	@Override
	public void setEnabled(final Object evaluationContext) {

		super.setEnabled(evaluationContext);

		if (_isInitializedState && _isInitializedView) {
			return;
		}

		final IWorkbench wb = PlatformUI.getWorkbench();

		if (_isInitializedState == false) {

			/**
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 * <p>
			 * getting the state works only the first time, otherwise it returns the OLD state
			 * <p>
			 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			 */

			final Command command = ((ICommandService) wb.getService(ICommandService.class)).getCommand(COMMAND_ID);

			final State state = command.getState(RegistryToggleState.STATE_ID);

			_isSyncPhotoWithTour = (Boolean) state.getValue();

			_isInitializedState = true;
		}

		/*
		 * get PicDirView
		 */
		PicDirView picDirView = null;

		for (final IWorkbenchWindow wbWindow : wb.getWorkbenchWindows()) {

			final IWorkbenchPage wbPage = wbWindow.getActivePage();

			if (wbPage != null) {

				for (final IViewReference viewRef : wbPage.getViewReferences()) {
					if (viewRef.getId().equals(PicDirView.ID)) {
						final IViewPart viewPart = viewRef.getView(false);
						if (viewPart instanceof PicDirView) {
							picDirView = (PicDirView) viewPart;
							break;
						}
					}
				}
			}
		}

		if (picDirView != null) {

			_isInitializedView = true;

			picDirView.setSelectionConverter(_isSyncPhotoWithTour ? _syncSelectionProvider : null);
		}

//		System.out.println(UI.timeStampNano() + " _isSyncPhotoWithTour " + _isSyncPhotoWithTour);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}
}
