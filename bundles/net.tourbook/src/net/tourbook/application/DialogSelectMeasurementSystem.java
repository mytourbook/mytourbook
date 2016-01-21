/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import net.tourbook.Messages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

class DialogSelectMeasurementSystem extends Dialog {

	private Button	_rdoMetric;
	private Button	_rdoImperial;

	protected DialogSelectMeasurementSystem(final Shell parentShell) {
		super(parentShell);
	}

	@Override
	public boolean close() {

		final int systemIndex = _rdoMetric.getSelection() ? 0 : 1;

		MeasurementSystemContributionItem.selectSystemInPrefStore(systemIndex);

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.App_Dialog_FirstStartupSystem_Title);
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().margins(10, 10).numColumns(1).applyTo(container);
		{
			// label: measurement system
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.App_Dialog_FirstStartupSystem_Label_System);

			/*
			 * radio: metric
			 */
			_rdoMetric = new Button(container, SWT.RADIO);
			GridDataFactory.fillDefaults().indent(0, 10).applyTo(_rdoMetric);
			_rdoMetric.setText(Messages.App_Dialog_FirstStartupSystem_Radio_Metric);

			/*
			 * radio: imperial
			 */
			_rdoImperial = new Button(container, SWT.RADIO);
			_rdoImperial.setText(Messages.App_Dialog_FirstStartupSystem_Radio_Imperial);

			// label: info
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().indent(0, 15).applyTo(label);
			label.setText(Messages.App_Dialog_FirstStartupSystem_Label_Info);
		}

		// metric is the default
		_rdoMetric.setSelection(true);
		_rdoImperial.setSelection(false);

		applyDialogFont(container);

		return container;
	}

}
