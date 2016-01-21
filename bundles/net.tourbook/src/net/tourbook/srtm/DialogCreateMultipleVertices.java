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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */
package net.tourbook.srtm;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogCreateMultipleVertices extends Dialog {

	private static final String		DEFAULT_ELEVATION_START			= "0";										//$NON-NLS-1$
	private static final String		DEFAULT_ELEVATION_END			= "2000";									//$NON-NLS-1$
	private static final String		DEFAULT_ELEVATION_DIFFERENCE	= "200";									//$NON-NLS-1$

	private static final String		STATE_START_ELEVATION			= "start-elevation";						//$NON-NLS-1$
	private static final String		STATE_END_ELELEVATION			= "end-elevation";							//$NON-NLS-1$
	private static final String		STATE_ELELEVATION_DIFF			= "elevation-difference";					//$NON-NLS-1$

	private final IDialogSettings	fState							= TourbookPlugin.getDefault() //
																			.getDialogSettingsSection(
																					"CreateMultipleVertexes");	//$NON-NLS-1$

	private Text					fTxtStartEleValue;
	private Text					fTxtEndEleValue;
	private Text					fTxtEleDiff;
	private int						fStartEle;
	private int						fEndEle;
	private int						fEleDiff;

	public DialogCreateMultipleVertices(final Shell shell) {
		super(shell);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_multipleVertexes_title);
	}

	@Override
	public void create() {

		super.create();

		restoreState();
		validateFields();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		final FocusListener focusListener = new FocusListener() {
			public void focusGained(final FocusEvent e) {
				((Text) e.widget).selectAll();
			}

			public void focusLost(final FocusEvent e) {}
		};

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().extendedMargins(10, 10, 5, 10).numColumns(2).applyTo(container);
		{
			/*
			 * label: start elevation
			 */
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.dialog_multipleVertexes_label_startElevation);

			/*
			 * text: start ele input
			 */
			fTxtStartEleValue = new Text(container, SWT.BORDER | SWT.TRAIL);
			fTxtStartEleValue.addVerifyListener(UI.verifyListenerInteger(false));
			fTxtStartEleValue.addModifyListener(modifyListener);
			fTxtStartEleValue.addFocusListener(focusListener);

			/*
			 * label: end elevation
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.dialog_multipleVertexes_label_endElevation);

			/*
			 * text: start ele input
			 */
			fTxtEndEleValue = new Text(container, SWT.BORDER | SWT.TRAIL);
			fTxtEndEleValue.addVerifyListener(UI.verifyListenerInteger(false));
			fTxtEndEleValue.addModifyListener(modifyListener);
			fTxtEndEleValue.addFocusListener(focusListener);

			/*
			 * label: ele diff
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.dialog_multipleVertexes_label_eleDiff);

			/*
			 * text: ele diff input
			 */
			fTxtEleDiff = new Text(container, SWT.BORDER | SWT.TRAIL);
			fTxtEleDiff.addVerifyListener(UI.verifyListenerInteger(false));
			fTxtEleDiff.addModifyListener(modifyListener);
			fTxtEleDiff.addFocusListener(focusListener);
		}

		return container;
	}

	int getElevationDifference() {
		return fEleDiff;
	}

	int getEndElevation() {
		return fEndEle;
	}

	int getStartElevation() {
		return fStartEle;
	}

	@Override
	protected void okPressed() {

		saveState();

		super.okPressed();
	}

	private void restoreState() {

		String startEle = fState.get(STATE_START_ELEVATION);
		if (startEle == null) {
			startEle = DEFAULT_ELEVATION_START;
		}
		fTxtStartEleValue.setText(startEle);

		String endEle = fState.get(STATE_END_ELELEVATION);
		if (endEle == null) {
			endEle = DEFAULT_ELEVATION_END;
		}
		fTxtEndEleValue.setText(endEle);

		String eleDiff = fState.get(STATE_ELELEVATION_DIFF);
		if (eleDiff == null) {
			eleDiff = DEFAULT_ELEVATION_DIFFERENCE;
		}
		fTxtEleDiff.setText(eleDiff);
	}

	private void saveState() {

		fState.put(STATE_START_ELEVATION, fTxtStartEleValue.getText());
		fState.put(STATE_END_ELELEVATION, fTxtEndEleValue.getText());
		fState.put(STATE_ELELEVATION_DIFF, fTxtEleDiff.getText());
	}

	private void validateFields() {

		boolean isValid = true;

		try {

			fStartEle = Integer.parseInt(fTxtStartEleValue.getText());
			fEndEle = Integer.parseInt(fTxtEndEleValue.getText());
			fEleDiff = Integer.parseInt(fTxtEleDiff.getText());

			if (fStartEle >= fEndEle) {

				// start must be smaller than end
				isValid = false;

			} else if (fEleDiff <= 0) {

				// ele diff must be bigger than 0
				isValid = false;

			} else if (fEndEle < fStartEle + fEleDiff) {

				// end ele must be at least one diff
				isValid = false;
			}

		} catch (final NumberFormatException e) {
			isValid = false;
		}

		getButton(IDialogConstants.OK_ID).setEnabled(isValid);
	}

}
