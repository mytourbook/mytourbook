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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.MtMath;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Slideout for the tour tag filter
 */
public class SlideoutGeoCompareOptions extends AdvancedSlideout implements IColorSelectorListener {

	final static IPreferenceStore		_prefStore						= TourbookPlugin.getPrefStore();
	private static IDialogSettings		_state;

	private PixelConverter				_pc;
	private ToolItem					_actionToolItem;

	private SelectionAdapter			_compareSelectionListener;
	private MouseWheelListener			_compareMouseWheelListener;
	private MouseWheelListener			_mapOptions_MouseWheelListener;
	private IPropertyChangeListener		_mapOptions_PropertyListener;
	private SelectionAdapter			_mapOptions_SelectionListener;

	private Action						_actionRestoreDefaults;

	private GeoCompareView				_geoCompareView;

	private int							_geoAccuracy;

	/**
	 * contains the controls which are displayed in the first column, these controls are used to get
	 * the maximum width and set the first column within the different section to the same width
	 */
	private final ArrayList<Control>	_firstColumnContainerControls	= new ArrayList<Control>();
	private final ArrayList<Control>	_firstColumnControls			= new ArrayList<Control>();
	private final ArrayList<Control>	_secondColumnControls			= new ArrayList<Control>();

	/*
	 * UI controls
	 */
	private Composite					_parent;

	private ColorSelectorExtended		_colorMapOption_RefTour;
	private ColorSelectorExtended		_colorMapOption_ComparedTourPart;

	private Label						_lblGeoAccuracy;
	private Label						_lblDistanceIntervalUnit;
	private Label						_lblNormalizedDistance;
	private Label						_lblNormalizedDistanceUnit;
	private Label						_lblNumGeoGrids;
	private Label						_lblNumSlices;
	private Label						_lblNumTours;

	private Spinner						_spinnerGeoAccuracy;
	private Spinner						_spinnerDistanceInterval;
	private Spinner						_spinnerMapOption_LineWidth;
	private Spinner						_spinnerMapOption_Opacity;

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

	@Override
	public void colorDialogOpened(final boolean isAnotherDialogOpened) {

		setIsAnotherDialogOpened(isAnotherDialogOpened);
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

		_parent = parent;

		initUI(parent);

		createActions();

		createUI(parent);

		restoreState();
		updateUI_GeoAccuracy();
		updateUI_StateValues(_geoCompareView.getSlideoutState());

		enableControls();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_20_CompareOptions(container);
			createUI_90_Actions(container);
		}

		// compute width for all controls and equalize column width for the different sections
		container.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnControls, 5);
		UI.setEqualizeColumWidths(_secondColumnControls);

		container.layout(true, true);
		UI.setEqualizeColumWidths(_firstColumnContainerControls);
	}

	private void createUI_20_CompareOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUI_30_Info(container);
			createUI_40_Options(container);
			createUI_50_MapOptions(container);
		}
	}

	private void createUI_30_Info(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		_firstColumnContainerControls.add(container);

		{
			{
				/*
				 * Number of tours
				 */
				{
					final Label label = new Label(container, SWT.NONE);
					label.setText("Possible tours");

					_firstColumnControls.add(label);
				}
				{
					_lblNumTours = new Label(container, SWT.NONE);
					_lblNumTours.setText(UI.EMPTY_STRING);
					GridDataFactory
							.fillDefaults()
							.grab(true, false)
							//							.align(SWT.END, SWT.FILL)
							.applyTo(_lblNumTours);
				}
			}
			{
				/*
				 * Number of geo parts
				 */
				{
					final Label label = new Label(container, SWT.NONE);
					label.setText("Geo grids");
					label.setToolTipText("Number of geo squares (1.57 km / 1 mi)");

					_firstColumnControls.add(label);
				}
				{
					_lblNumGeoGrids = new Label(container, SWT.NONE);
					_lblNumGeoGrids.setText(UI.EMPTY_STRING);
					GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoGrids);
				}
			}
			{
				/*
				 * Number of time slices
				 */
				{
					final Label label = new Label(container, SWT.NONE);
					label.setText("Time slices");

					_firstColumnControls.add(label);
				}
				{
					_lblNumSlices = new Label(container, SWT.NONE);
					_lblNumSlices.setText(UI.EMPTY_STRING);
					GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
				}
			}
		}
	}

	private void createUI_40_Options(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()
				//				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));

		_firstColumnContainerControls.add(container);

		{
			{
				/*
				 * Distance
				 */
				{
					final Label label = new Label(container, SWT.NONE);
					label.setText("Distance");

					_firstColumnControls.add(label);
				}
				{
					_lblNormalizedDistance = new Label(container, SWT.TRAIL);
					_lblNormalizedDistance.setText(UI.EMPTY_STRING);
					GridDataFactory
							.fillDefaults()
							//							.grab(true, false)
							//							.hint(_pc.convertWidthInCharsToPixels(2), SWT.DEFAULT)
							.align(SWT.END, SWT.FILL)
							.applyTo(_lblNormalizedDistance);

					_secondColumnControls.add(_lblNormalizedDistance);
				}
				{
					// Label: Distance unit
					_lblNormalizedDistanceUnit = new Label(container, SWT.NONE);
					_lblNormalizedDistanceUnit.setText(UI.EMPTY_STRING);
					GridDataFactory
							.fillDefaults()
							.grab(true, false)
							.applyTo(_lblNormalizedDistanceUnit);
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

					_firstColumnControls.add(label);
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

					_secondColumnControls.add(_spinnerDistanceInterval);
				}
				{
					// Label: Distance unit
					_lblDistanceIntervalUnit = new Label(container, SWT.NONE);
					_lblDistanceIntervalUnit.setText("m");
					GridDataFactory
							.fillDefaults()
							.grab(true, false)
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(_lblDistanceIntervalUnit);
				}
			}
			{
				/*
				 * Normalized geo data factor
				 */
				{
					// Label
					final Label label = new Label(container, SWT.NONE);
					label.setText("Geo &accuracy");

					_firstColumnControls.add(label);
				}
				{
					// Spinner
					_spinnerGeoAccuracy = new Spinner(container, SWT.BORDER);
					_spinnerGeoAccuracy.setMinimum(100);
					_spinnerGeoAccuracy.setMaximum(100_000);
					_spinnerGeoAccuracy.setPageIncrement(100);
					_spinnerGeoAccuracy.addSelectionListener(_compareSelectionListener);
					_spinnerGeoAccuracy.addMouseWheelListener(_compareMouseWheelListener);
//					GridDataFactory
//							.fillDefaults()
//							.align(SWT.END, SWT.FILL)
//							.applyTo(_spinnerGeoAccuracy);

					_secondColumnControls.add(_spinnerGeoAccuracy);
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
	}

	private void createUI_50_MapOptions(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText("Map Options for the Reference Tour");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

		_firstColumnContainerControls.add(group);

		{
			{
				/*
				 * Ref tour line width
				 */
				{
					// label
					final Label label = new Label(group, SWT.NONE);
					label.setText("Line width");

					_firstColumnControls.add(label);
				}
				{
					// spinner
					_spinnerMapOption_LineWidth = new Spinner(group, SWT.BORDER);
					_spinnerMapOption_LineWidth.setMinimum(1);
					_spinnerMapOption_LineWidth.setMaximum(100);
					_spinnerMapOption_LineWidth.setPageIncrement(5);
					_spinnerMapOption_LineWidth.addSelectionListener(_mapOptions_SelectionListener);
					_spinnerMapOption_LineWidth.addMouseWheelListener(_mapOptions_MouseWheelListener);

					_secondColumnControls.add(_spinnerMapOption_LineWidth);
				}
			}
			{
				/*
				 * Ref tour in map
				 */
				{
					// label
					final Label label = new Label(group, SWT.NONE);
					label.setText("Reference tour");

					_firstColumnControls.add(label);
				}
				{
					// color
					_colorMapOption_RefTour = new ColorSelectorExtended(group);
					_colorMapOption_RefTour.addListener(_mapOptions_PropertyListener);
					_colorMapOption_RefTour.addOpenListener(this);
				}
			}
			{
				/*
				 * Compared tour part in map
				 */
				{
					// label
					final Label label = new Label(group, SWT.NONE);
					label.setText("Compared tour part");

					_firstColumnControls.add(label);
				}
				{
					// color
					_colorMapOption_ComparedTourPart = new ColorSelectorExtended(group);
					_colorMapOption_ComparedTourPart.addListener(_mapOptions_PropertyListener);
					_colorMapOption_ComparedTourPart.addOpenListener(this);
				}
			}
			{
				/*
				 * Opacity
				 */
				{
					// label
					final Label label = new Label(group, SWT.NONE);
					label.setText("Opacity");
					label.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOpacity_Tooltip);

					_firstColumnControls.add(label);
				}
				{
					// spinner
					_spinnerMapOption_Opacity = new Spinner(group, SWT.BORDER);
					_spinnerMapOption_Opacity.setMinimum(PrefPageMap2Appearance.MAP_OPACITY_MINIMUM);
					_spinnerMapOption_Opacity.setMaximum(100);
					_spinnerMapOption_Opacity.setIncrement(1);
					_spinnerMapOption_Opacity.setPageIncrement(10);
					_spinnerMapOption_Opacity.addSelectionListener(_mapOptions_SelectionListener);
					_spinnerMapOption_Opacity.addMouseWheelListener(_mapOptions_MouseWheelListener);
				}
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

		_mapOptions_SelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChange_MapOptions();
			}
		};

		_mapOptions_MouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChange_MapOptions();
			}
		};

		_mapOptions_PropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChange_MapOptions();
			}
		};
	}

	private void onChange_CompareParameter() {

		saveStateSlideout();

		updateUI_GeoAccuracy();

		_geoCompareView.onChange_CompareParameter();
	}

	private void onChange_MapOptions() {

		_prefStore.setValue(
				ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH, //
				_spinnerMapOption_LineWidth.getSelection());

		_prefStore.setValue(
				ITourbookPreferences.MAP2_LAYOUT_OPACITY, //
				_spinnerMapOption_Opacity.getSelection());

		PreferenceConverter.setValue(
				_prefStore,
				ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB, //
				_colorMapOption_RefTour.getColorValue());

		PreferenceConverter.setValue(
				_prefStore,
				ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB, //
				_colorMapOption_ComparedTourPart.getColorValue());

		// force repainting
		TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	private void onDisposeSlideout() {

		saveStateSlideout();

		_firstColumnControls.clear();
		_secondColumnControls.clear();
	}

	@Override
	protected void onFocus() {

		_spinnerGeoAccuracy.setFocus();
	}

	private void resetToDefaults() {

		_spinnerDistanceInterval.setSelection(GeoCompareView.DEFAULT_DISTANCE_INTERVAL);
		_spinnerGeoAccuracy.setSelection(GeoCompareView.DEFAULT_GEO_ACCURACY);

		/*
		 * Map options
		 */
		_spinnerMapOption_Opacity.setSelection(
				_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_OPACITY));

		_spinnerMapOption_LineWidth.setSelection(
				_prefStore.getDefaultInt(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH));

		_colorMapOption_RefTour.setColorValue(
				PreferenceConverter.getDefaultColor(
						_prefStore,
						ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB));

		_colorMapOption_ComparedTourPart.setColorValue(
				PreferenceConverter.getDefaultColor(
						_prefStore,
						ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB));

		onChange_CompareParameter();
		onChange_MapOptions();
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

		/*
		 * Map options
		 */
		_spinnerMapOption_LineWidth.setSelection(
				_prefStore.getInt(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH));

		_colorMapOption_RefTour.setColorValue(
				PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB));

		_colorMapOption_ComparedTourPart.setColorValue(
				PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB));

		_spinnerMapOption_Opacity.setSelection(_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_OPACITY));
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

	void updateUI_StateValues(final GeoCompareState slideoutState) {

		if (_parent == null || _parent.isDisposed() || slideoutState == null) {
			return;
		}

		if (slideoutState.isReset) {

			_lblNormalizedDistance.setText(UI.EMPTY_STRING);
			_lblNumGeoGrids.setText(UI.EMPTY_STRING);
			_lblNumSlices.setText(UI.EMPTY_STRING);
			_lblNumTours.setText(UI.EMPTY_STRING);

		} else {

			final float distance = slideoutState.normalizedDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			_lblNormalizedDistance.setText(FormatManager.formatDistance(distance / 1000.0));
			_lblNormalizedDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

			_lblNumGeoGrids.setText(Integer.toString(slideoutState.numGeoGrids));
			_lblNumSlices.setText(Integer.toString(slideoutState.numSlices));
			_lblNumTours.setText(Integer.toString(slideoutState.numTours));
		}

	}

}
