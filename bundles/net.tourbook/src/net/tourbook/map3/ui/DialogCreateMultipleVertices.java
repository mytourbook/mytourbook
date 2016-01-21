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
package net.tourbook.map3.ui;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class DialogCreateMultipleVertices extends Dialog {

	private static final int		DEFAULT_START_VALUE			= 0;
	private static final int		DEFAULT_END_VALUE			= 2000;
	private static final int		DEFAULT_DIFFERENCE_VALUE	= 200;
	private static final int		MAX_OF_CREATED_VALUES		= 20;

	private static final String		ERROR_TEXT					= "%d > %d"; //$NON-NLS-1$

	private static final String		STATE_START_VALUE			= "STATE_START_VALUE";								//$NON-NLS-1$
	private static final String		STATE_END_VALUE				= "STATE_END_VALUE";								//$NON-NLS-1$
	private static final String		STATE_DIFFERENCE_VALUE		= "STATE_DIFFERENCE_VALUE";						//$NON-NLS-1$

	private final IDialogSettings	_state						= TourbookPlugin.getState(getClass().getName());

	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;

	private int						_startValue;
	private int						_endValue;
	private int						_valueDiff;

	/*
	 * UI controls
	 */
	private Label					_lblNumberOfCreatedValues;

	private Spinner					_spinEndValue;
	private Spinner					_spinStartValue;
	private Spinner					_spinValueDiff;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				validateFields();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				validateFields();
			}
		};
	}

	public DialogCreateMultipleVertices(final Shell shell) {
		super(shell);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Map3Vertices_Dialog_Title);
	}

	@Override
	public void create() {

		super.create();

		restoreState();
		validateFields();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(10, 10, 10, 10)
				.spacing(20, 5)
				.numColumns(2)
				.applyTo(container);
		{
			{
				/*
				 * Start value
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Vertices_Dialog_Label_StartValue);

				_spinStartValue = new Spinner(container, SWT.BORDER);
				_spinStartValue.setMinimum(Integer.MIN_VALUE);
				_spinStartValue.setMaximum(Integer.MAX_VALUE);
				_spinStartValue.setPageIncrement(10);
				_spinStartValue.addSelectionListener(_defaultSelectionAdapter);
				_spinStartValue.addMouseWheelListener(_defaultMouseWheelListener);
			}

			{
				/*
				 * End value
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Vertices_Dialog_Label_EndValue);

				_spinEndValue = new Spinner(container, SWT.BORDER);
				_spinEndValue.setMinimum(Integer.MIN_VALUE);
				_spinEndValue.setMaximum(Integer.MAX_VALUE);
				_spinEndValue.setPageIncrement(10);
				_spinEndValue.addSelectionListener(_defaultSelectionAdapter);
				_spinEndValue.addMouseWheelListener(_defaultMouseWheelListener);
			}

			{
				/*
				 * Value difference
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Vertices_Dialog_Label_ValueDifference);

				_spinValueDiff = new Spinner(container, SWT.BORDER);
				_spinValueDiff.setMinimum(Integer.MIN_VALUE);
				_spinValueDiff.setMaximum(Integer.MAX_VALUE);
				_spinValueDiff.setPageIncrement(10);
				_spinValueDiff.addSelectionListener(_defaultSelectionAdapter);
				_spinValueDiff.addMouseWheelListener(_defaultMouseWheelListener);
			}

			{
				/*
				 * Number of created values
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Map3Vertices_Dialog_Label_NumberOfCreatedValues);

				_lblNumberOfCreatedValues = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumberOfCreatedValues);
			}
		}

		return container;
	}

	int getEndValue() {
		return _endValue;
	}

	int getStartValue() {
		return _startValue;
	}

	int getValueDifference() {
		return _valueDiff;
	}

	@Override
	protected void okPressed() {

		saveState();

		super.okPressed();
	}

	private void restoreState() {

		_spinStartValue.setSelection(Util.getStateInt(_state, STATE_START_VALUE, DEFAULT_START_VALUE));
		_spinEndValue.setSelection(Util.getStateInt(_state, STATE_END_VALUE, DEFAULT_END_VALUE));
		_spinValueDiff.setSelection(Util.getStateInt(_state, STATE_DIFFERENCE_VALUE, DEFAULT_DIFFERENCE_VALUE));
	}

	private void saveState() {

		_state.put(STATE_START_VALUE, _startValue);
		_state.put(STATE_END_VALUE, _endValue);
		_state.put(STATE_DIFFERENCE_VALUE, _valueDiff);
	}

	private void validateFields() {

		boolean isValid = true;

		try {

			_startValue = _spinStartValue.getSelection();
			_endValue = _spinEndValue.getSelection();
			_valueDiff = _spinValueDiff.getSelection();

			if (_startValue >= _endValue) {

				// start must be smaller than end
				isValid = false;

			} else if (_valueDiff <= 0) {

				// ele diff must be bigger than 0
				isValid = false;

			} else if (_endValue < _startValue + _valueDiff) {

				// end ele must be at least one diff
				isValid = false;
			}

		} catch (final NumberFormatException e) {
			isValid = false;
		}

		if (isValid) {

			final int numberOfCreatedValues = (_endValue - _startValue) / _valueDiff;

			if (numberOfCreatedValues > MAX_OF_CREATED_VALUES) {
				isValid = false;
			}

			_lblNumberOfCreatedValues.setText(isValid //
					? Integer.toString(numberOfCreatedValues)
					: String.format(ERROR_TEXT, numberOfCreatedValues, MAX_OF_CREATED_VALUES));
		} else {

			_lblNumberOfCreatedValues.setText(UI.EMPTY_STRING);
		}

		getButton(IDialogConstants.OK_ID).setEnabled(isValid);
	}

}
