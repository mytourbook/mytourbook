/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog.geo;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutGeoCompareOptions extends AdvancedSlideout {

	private static IDialogSettings	_state;

	private PixelConverter			_pc;
	private ToolItem				_actionToolItem;

	private SelectionAdapter		_compareSelectionListener;
	private MouseWheelListener		_compareMouseWheelListener;

	private Action					_actionRestoreDefaults;

	private GeoCompareView			_geoCompareView;

	private int						_geoAccuracy;

	/*
	 * UI controls
	 */
	private Label					_lblGeoAccuracy;

	private Spinner					_spinnerGeoAccuracy;
	private Spinner					_spinnerDistanceInterval;

	/**
	 * @param actionToolItem
	 * @param state
	 * @param geoCompareView
	 */
	public SlideoutGeoCompareOptions(	final ToolItem actionToolItem,
										final IDialogSettings state,
										final GeoCompareView geoCompareView) {

		super(
				actionToolItem.getParent(),
				state,
				new int[] { 700, 400, 700, 400 });

		_actionToolItem = actionToolItem;
		_state = state;
		_geoCompareView = geoCompareView;

		setShellFadeOutDelaySteps(10);
		setTitleText("Geo Compare Options");
	}

	private void createActions() {

		/*
		 * Action: Restore default
		 */
		_actionRestoreDefaults = new Action() {
			@Override
			public void run() {
				resetToDefaults();
			}
		};

		_actionRestoreDefaults.setImageDescriptor(//
				TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
	}

	@Override
	protected void createSlideoutContent(final Composite parent) {

		initUI(parent);

		createActions();

		createUI(parent);

		restoreState();
		updateUI_GeoAccuracy();

		enableControls();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_20_Controls(container);
			createUI_90_Actions(container);
		}
	}

	private void createUI_20_Controls(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
//			createUI_22_Left(container);
			createUI_24_Right(container);
		}
	}

	private void createUI_24_Right(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			{
				/*
				 * Normalized geo data factor
				 */
				{
					// Label
					final Label label = new Label(container, SWT.NONE);
					label.setText("Geo &accuracy");
				}
				{
					// Spinner
					_spinnerGeoAccuracy = new Spinner(container, SWT.BORDER);
					_spinnerGeoAccuracy.setMinimum(100);
					_spinnerGeoAccuracy.setMaximum(100_000);
					_spinnerGeoAccuracy.setPageIncrement(100);
					_spinnerGeoAccuracy.addSelectionListener(_compareSelectionListener);
					_spinnerGeoAccuracy.addMouseWheelListener(_compareMouseWheelListener);
				}
				{
					// geo distance
					_lblGeoAccuracy = new Label(container, SWT.NONE);
					GridDataFactory
							.fillDefaults()
							.grab(true, false)
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblGeoAccuracy);
				}
			}
		}
		{
			/*
			 * Distance interval
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				label.setText("&Distance interval");
			}
			{
				// Spinner
				_spinnerDistanceInterval = new Spinner(container, SWT.BORDER);
				_spinnerDistanceInterval.setMinimum(10);
				_spinnerDistanceInterval.setMaximum(1_000);
				_spinnerDistanceInterval.setPageIncrement(10);
				_spinnerDistanceInterval.addSelectionListener(_compareSelectionListener);
				_spinnerDistanceInterval.addMouseWheelListener(_compareMouseWheelListener);
				GridDataFactory
						.fillDefaults()
						.align(SWT.END, SWT.FILL)
						.applyTo(_spinnerDistanceInterval);
			}
			{
				// Label: Distance unit
				final Label labelDistanceUnit = new Label(container, SWT.NONE);
				labelDistanceUnit.setText("m");
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(labelDistanceUnit);
			}
		}
	}

	private void createUI_90_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void enableControls() {
		// TODO Auto-generated method stub

	}

	@Override
	protected Rectangle getParentBounds() {

		final Rectangle itemBounds = _actionToolItem.getBounds();
		final Point itemDisplayPosition = _actionToolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

		itemBounds.x = itemDisplayPosition.x;
		itemBounds.y = itemDisplayPosition.y;

		return itemBounds;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDisposeSlideout();
			}
		});

		_compareSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChange_CompareParameter();
			}
		};

		_compareMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChange_CompareParameter();
			}
		};
	}

	private void onChange_CompareParameter() {

		saveStateSlideout();

		updateUI_GeoAccuracy();

		_geoCompareView.onChange_CompareParameter();
	}

//	private void createUI_22_Left(final Composite parent) {
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		{
//			{
//				/*
//				 * Number of tours
//				 */
//
//				final Label label = new Label(container, SWT.NONE);
//				label.setText("Tours"); //$NON-NLS-1$
//
//				_lblNumTours = new Label(container, SWT.NONE);
//				_lblNumTours.setText(UI.EMPTY_STRING);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
//			}
//			{
//				/*
//				 * Number of time slices
//				 */
//
//				final Label label = new Label(container, SWT.NONE);
//				label.setText("Time slices"); //$NON-NLS-1$
//
//				_lblNumSlices = new Label(container, SWT.NONE);
//				_lblNumSlices.setText(UI.EMPTY_STRING);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
//			}
//			{
//				/*
//				 * Number of geo parts
//				 */
//
//				final Label label = new Label(container, SWT.NONE);
//				label.setText("Geo grids"); //$NON-NLS-1$
//
//				_lblNumGeoGrid = new Label(container, SWT.NONE);
//				_lblNumGeoGrid.setText(UI.EMPTY_STRING);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoGrid);
//			}
//		}
//	}

	private void onDisposeSlideout() {

		saveStateSlideout();
	}

	@Override
	protected void onFocus() {

		_spinnerGeoAccuracy.setFocus();
	}

	private void resetToDefaults() {

		_spinnerDistanceInterval.setSelection(GeoCompareView.DEFAULT_DISTANCE_INTERVAL);
		_spinnerGeoAccuracy.setSelection(GeoCompareView.DEFAULT_GEO_ACCURACY);

		onChange_CompareParameter();
	}

	private void restoreState() {

		_geoAccuracy = Util.getStateInt(
				_state,
				GeoCompareView.STATE_GEO_ACCURACY,
				GeoCompareView.DEFAULT_GEO_ACCURACY);

		_spinnerGeoAccuracy.setSelection(_geoAccuracy);

		_spinnerDistanceInterval.setSelection(
				Util.getStateInt(
						_state,
						GeoCompareView.STATE_DISTANCE_INTERVAL,
						GeoCompareView.DEFAULT_DISTANCE_INTERVAL));
	}

	private void saveStateSlideout() {

		_geoAccuracy = _spinnerGeoAccuracy.getSelection();

		_state.put(GeoCompareView.STATE_GEO_ACCURACY, _geoAccuracy);
		_state.put(GeoCompareView.STATE_DISTANCE_INTERVAL, _spinnerDistanceInterval.getSelection());

	}

	private void updateUI_GeoAccuracy() {

		final double latStart = 0;
		final double latEnd = 1.0 / _geoAccuracy;

		final double lonStart = 0;
		final double lonEnd = 1.0 / _geoAccuracy;

		final double distDiff = MtMath.distanceVincenty(latStart, lonStart, latEnd, lonEnd);

		final double distValue = distDiff / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE_SMALL;

		final String valueFormatting = distValue > 100
				? "%1.0f %s"
				: distValue > 10
						? "%1.1f %s"//
						: "%1.2f %s";

		final String geoDistance = String.format(valueFormatting, distValue, UI.UNIT_LABEL_DISTANCE_SMALL);

		_lblGeoAccuracy.setText(geoDistance);
	}
}
