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
package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor settings.
 */
public class SlideoutViewSettings extends ToolbarSlideout implements IColorSelectorListener {

	private final IDialogSettings	_state	= TourbookPlugin.getState(TourDataEditorView.ID);

	private TourDataEditorView		_tourEditorView;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Spinner					_spinnerLatLonDigits;

	private Label					_lblLatLonDigits;

	public SlideoutViewSettings(final Control ownerControl,
								final ToolBar toolBar,
								final IDialogSettings state,
								final TourDataEditorView tourMarkerAllView) {

		super(ownerControl, toolBar);

		_tourEditorView = tourMarkerAllView;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				createUI_10_LatLonDigits(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_LatLonDigits(final Composite parent) {

		final SelectionListener _selectionAdapterLatLonDigits = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelect_LatLonDigits();
			}
		};

		/*
		 * Lat/lon digits
		 */
		{
			// label: lat/lon digits
			_lblLatLonDigits = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblLatLonDigits);
			_lblLatLonDigits.setText(Messages.Slideout_TourEditor_Label_LatLonDigits);
			_lblLatLonDigits.setToolTipText(Messages.Slideout_TourEditor_Label_LatLonDigits_Tooltip);

			// spinner
			_spinnerLatLonDigits = new Spinner(parent, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.END, SWT.CENTER)
					.applyTo(_spinnerLatLonDigits);
			_spinnerLatLonDigits.setMinimum(0);
			_spinnerLatLonDigits.setMaximum(20);
			_spinnerLatLonDigits.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onSelect_LatLonDigits();
				}
			});
			_spinnerLatLonDigits.addSelectionListener(_selectionAdapterLatLonDigits);
		}
	}

	private void onSelect_LatLonDigits() {

		final int latLonDigits = _spinnerLatLonDigits.getSelection();

		_state.put(TourDataEditorView.STATE_LAT_LON_DIGITS, latLonDigits);

		_tourEditorView.updateUI_LatLonDigits(latLonDigits);
	}

	private void restoreState() {

		/*
		 * Lat/lon digits
		 */
		_spinnerLatLonDigits.setSelection(Util.getStateInt(
				_state,
				TourDataEditorView.STATE_LAT_LON_DIGITS,
				TourDataEditorView.DEFAULT_LAT_LON_DIGITS));
	}

}
