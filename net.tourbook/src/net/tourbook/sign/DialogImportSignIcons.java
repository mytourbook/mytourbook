/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.sign;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.PhotosWithExifSelection;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to import tour sign icons.
 */
public class DialogImportSignIcons extends TitleAreaDialog {

	private final IDialogSettings	_state	= TourbookPlugin.getState("DialogImportTourSignIcons"); //$NON-NLS-1$

	/*
	 * none UI
	 */
	private PixelConverter			_pc;

	/*
	 * UI controls
	 */

	/**
	 * @param parentShell
	 * @param selectedPhotosWithExif
	 * @param tourData
	 * @param initialTourMarker
	 *            TourMarker which is selected when the dialog is opened
	 */
	public DialogImportSignIcons(final Shell parentShell, final PhotosWithExifSelection selectedPhotosWithExif) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window
//		tour-sign-import.png
		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__TourSignImport).createImage());

	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_ImportSigns_Dialog_Title);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgContainer);

		restoreState();

		setTitle(Messages.Dialog_ImportSigns_Dialog_Title);
		setMessage(Messages.Dialog_ImportSigns_Dialog_Title_Message);

		enableControls();

		return dlgContainer;
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite marginContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(marginContainer);
		GridLayoutFactory.swtDefaults().applyTo(marginContainer);
		{
			final Composite dlgContainer = new Composite(marginContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
			GridLayoutFactory.swtDefaults().applyTo(dlgContainer);
			{
				final Label label = new Label(dlgContainer, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("test"); //$NON-NLS-1$

			}
		}
	}

	private void enableControls() {

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		return _state;
//		return null;
	}

	@Override
	protected void okPressed() {

		super.okPressed();
	}

	private void restoreState() {

	}

	private void saveState() {

	}

}
