/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class DialogAdjustTemperature_OLD extends TitleAreaDialog {

	private static final int		VERTICAL_SECTION_MARGIN	= 10;

	private final IDialogSettings	_state					= TourbookPlugin
																	.getState("net.tourbook.tour.DialogAdjustTemperature"); //$NON-NLS-1$

	private ArrayList<TourData>		_selectedTours;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Composite				_dlgContainer;
	private Composite				_inputContainer;

	private DateTime				_dtTemperatureAdjustmentDuration;

	private Label					_lblAvgTemperature;
	private Label					_lblTemperatureAdjustmentInfo;
	private Label					_lblTemperatureAdjustmentDuration;
	private Label					_lblTemperatureUnit;

	private ProgressMonitorPart		_progressMonitor;

	private Spinner					_spinnerAvgTemperature;

	public DialogAdjustTemperature_OLD(final Shell parentShell, final ArrayList<TourData> selectedTours) {

		super(parentShell);

		_selectedTours = selectedTours;
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_AdjustTemperature_Dialog_Title);
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_AdjustTemperature_Dialog_Title);
		setMessage(Messages.Dialog_AdjustTemperature_Dialog_Message);

		restoreState();

		/*
		 * Set shell size that it DO NOT display a free area
		 */
		final Shell shell = getShell();

		shell.pack();

		// set width after the first pack, see SWT Snippet335
		final GridData gd = (GridData) _lblTemperatureAdjustmentInfo.getLayoutData();
		gd.widthHint = _pc.convertWidthInCharsToPixels(60);

		shell.pack();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
//		getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_AdjustTemperature_Button_AdjustTemperature);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		initUI(parent);

		_dlgContainer = (Composite) super.createDialogArea(parent);
//		_dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		createUI(_dlgContainer);

//		_lblProgressWorked.setText(NLS.bind(//
//				Messages.Dialog_AdjustTemperature_Label_Progress,
//				0,
//				_selectedTours.size()));

		return _dlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(5, 0).numColumns(1).applyTo(container);
		{
			createUI_10_Controls(container);
			createUI_90_Progress(container);
		}
	}

	private void createUI_10_Controls(final Composite parent) {

		_inputContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(_inputContainer);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_inputContainer);
		{
			{
				/*
				 * Label: Temperature adjustment info
				 */
				_lblTemperatureAdjustmentInfo = new Label(_inputContainer, SWT.WRAP | SWT.READ_ONLY);
				_lblTemperatureAdjustmentInfo.setText(Messages.Dialog_AdjustTemperature_Info_TemperatureAdjustment);
				GridDataFactory.fillDefaults().applyTo(_lblTemperatureAdjustmentInfo);
			}

			final Composite container = new Composite(_inputContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				{
					/*
					 * Label: Adjustment duration
					 */
					_lblTemperatureAdjustmentDuration = new Label(container, SWT.NONE);
					_lblTemperatureAdjustmentDuration
							.setText(Messages.Dialog_AdjustTemperature_Label_TemperatureAdjustmentDuration);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(0, _pc.convertVerticalDLUsToPixels(4))
							.applyTo(_lblTemperatureAdjustmentDuration);

					/*
					 * DateTime: Duration
					 */
					_dtTemperatureAdjustmentDuration = new DateTime(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
					GridDataFactory.fillDefaults()//
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(_pc.convertWidthInCharsToPixels(2), _pc.convertVerticalDLUsToPixels(4))
							.applyTo(_dtTemperatureAdjustmentDuration);

					// spacer
					new Label(container, SWT.NONE);
				}

				{
					/*
					 * Avg temperature
					 */
					// label
					_lblAvgTemperature = new Label(container, SWT.NONE);
					_lblAvgTemperature.setText(Messages.Dialog_AdjustTemperature_Label_AvgTemperature);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblAvgTemperature);

					// spinner
					_spinnerAvgTemperature = new Spinner(container, SWT.BORDER);
					_spinnerAvgTemperature.setPageIncrement(5);
					_spinnerAvgTemperature.setMinimum(0);
					_spinnerAvgTemperature.setMaximum(30);
					_spinnerAvgTemperature.addMouseWheelListener(new MouseWheelListener() {
						@Override
						public void mouseScrolled(final MouseEvent event) {
							Util.adjustSpinnerValueOnMouseScroll(event);
						}
					});
					GridDataFactory.fillDefaults() //
							.align(SWT.END, SWT.FILL)
							.applyTo(_spinnerAvgTemperature);

					// label: °C
					_lblTemperatureUnit = new Label(container, SWT.NONE);
					_lblTemperatureUnit.setText(UI.SYMBOL_TEMPERATURE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblTemperatureUnit);
				}
			}
		}
	}

	private void createUI_90_Progress(final Composite parent) {

//		final int selectedTours = _selectedTours.size();
//
//		// hide progress bar when only one tour is adjusted
//		if (selectedTours < 2) {
//			return;
//		}

		final GridLayout gl = new GridLayout();

		_progressMonitor = new ProgressMonitorPart(parent, gl, true);

		_progressMonitor.setVisible(false);

		// grab width
		final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		_progressMonitor.setLayoutData(gridData);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep ONLY window position
		return _state;
	}

	@Override
	protected int getDialogBoundsStrategy() {

//
// ORIGINAL
//
//		return DIALOG_PERSISTLOCATION | DIALOG_PERSISTSIZE;

		/*
		 * Do NOT persist the SIZE otherwise when the content is changed it will FOREVER have the
		 * wrong size because the dialog cannot be resized.
		 */

		return DIALOG_PERSISTLOCATION;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	private void restoreState() {}

	private void saveState() {}

}
