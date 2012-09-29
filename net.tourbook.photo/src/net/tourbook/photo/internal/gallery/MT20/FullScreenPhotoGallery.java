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
package net.tourbook.photo.internal.gallery.MT20;

import net.tourbook.common.UI;
import net.tourbook.photo.IPhotoGalleryProvider;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.PhotoSelection;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FullScreenPhotoGallery implements IPhotoGalleryProvider {

	private static final int	GALLERY_ACTIVATION_POSITION	= 100;

	private boolean				_isVisibleWhenLostFocus;
	private IDialogSettings		_state;

	private Shell				_fullScreenShell;
	private Shell				_galleryShell;

	private GalleryMT20			_sourceGallery;
	private PhotoGallery		_photoGallery;

	public FullScreenPhotoGallery(final Shell fullScreenshell, final GalleryMT20 galleryMT20) {

		_fullScreenShell = fullScreenshell;
		_sourceGallery = galleryMT20;

	}

	private void addFullScreenListener() {

		_fullScreenShell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	private void addShellListener() {

		_galleryShell.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				onFocusGained();
			}

			@Override
			public void focusLost(final FocusEvent e) {
				onFocusLost(e);
			}
		});
	}

	private void createUI() {

		_galleryShell = new Shell(SWT.NO_TRIM | SWT.ON_TOP);

		addShellListener();

		final Rectangle fsShellSize = _fullScreenShell.getBounds();

		_galleryShell.setBounds(fsShellSize.x, fsShellSize.y, fsShellSize.width, GALLERY_ACTIVATION_POSITION);

		_galleryShell.setLayout(new FillLayout());

		createUI_10_Gallery(_galleryShell);

		addFullScreenListener();

		restoreState();
	}

	private void createUI_10_Gallery(final Shell parent) {

		_photoGallery = new PhotoGallery();

		_photoGallery.createPhotoGallery(parent, SWT.H_SCROLL, this);
		_photoGallery.createActionBar();
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	@Override
	public IToolBarManager getToolBarManager() {
		return null;
	}

	private void hide() {

//		System.out.println(UI.timeStampNano() + " hide  gallery\t");
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_galleryShell == null || _galleryShell.isVisible() == false) {
			return;
		}

		_galleryShell.setVisible(false);
	}

	private void onDispose() {

		if (_galleryShell != null) {
			_galleryShell.close();
		}
	}

	void onFocusGained() {

		System.out.println(UI.timeStampNano() + " focusGained\tGallery");
		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isVisibleWhenLostFocus) {
			show();
		}

		_isVisibleWhenLostFocus = false;
	}

	private void onFocusLost(final FocusEvent e) {
		// TODO Auto-generated method stub

		System.out.println(UI.timeStampNano() + " focusLost\tGallery");
		// TODO remove SYSTEM.OUT.PRINTLN

		final boolean isVisible = _galleryShell.isVisible();

		if (isVisible) {

			_isVisibleWhenLostFocus = isVisible;

			hide();
		}
	}

	@Override
	public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

	private void restoreState() {

		updateColors(true);

		_photoGallery.restoreState(_state);
	}

	void restoreState(final IDialogSettings state) {

		_state = state;
	}

	void saveState(final IDialogSettings state) {

		_photoGallery.saveState(state);
	}

	@Override
	public void setSelection(final PhotoSelection photoSelection) {}

	private void show() {

		if (_galleryShell == null) {
			createUI();
			_galleryShell.open();
		}

		if (_galleryShell.isVisible() == false) {

			_galleryShell.setVisible(true);
			_galleryShell.setActive();
		}

//		_photoGallery.showImages(_sourceGallery.photoWrapperList, UI.EMPTY_STRING);
	}

	boolean showImages(final MouseEvent mouseEvent) {

		if (mouseEvent.y < GALLERY_ACTIVATION_POSITION) {

			// show gallery

			show();

			return true;

		} else {

			// hide gallery

			if (_galleryShell != null && _galleryShell.isVisible()) {
				_galleryShell.setVisible(false);
			}

			return false;
		}
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

}
