/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourSegmenter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.font.FontFieldEditorExtended;
import net.tourbook.common.font.IFontDialogListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartSegmenterProperties extends AnimatedToolTipShell implements IFontDialogListener {

	private static final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();
	private static final IDialogSettings	_segmenterState		= TourSegmenterView.getState();

	// initialize with default values which are (should) never be used
	private Rectangle						_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer					_waitTimer			= new WaitTimer();

	private boolean							_isWaitTimerStarted;
	private boolean							_canOpenToolTip;

	private IPropertyChangeListener			_defaultChangePropertyListener;
	private SelectionAdapter				_defaultSelectionAdapter;
	private MouseWheelListener				_defaultMouseWheelListener;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_defaultChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeFontInEditor();
			}
		};
	}

	private boolean							_isAnotherDialogOpened;
	private PixelConverter					_pc;
	private int								_firstColumnIndent;

	private TourSegmenterView				_tourSegmenterView;

	private Action							_actionRestoreDefaults;

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;
	private Composite						_fontContainer;

	private Button							_chkHideSmallValues;
	private Button							_chkShowDecimalPlaces;
	private Button							_chkShowSegmentMarker;
	private Button							_chkShowSegmentValue;

	private FontFieldEditorExtended			_valueFontEditor;

	private Label							_lblGraphOpacity;
	private Label							_lblHideSmallValuesUnit;
	private Label							_lblVisibleStackedValues;

	private Spinner							_spinGraphOpacity;
	private Spinner							_spinHideSmallValues;
	private Spinner							_spinVisibleValuesStacked;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartSegmenterProperties(final Control ownerControl,
											final ToolBar toolBar,
											final TourSegmenterView tourSegmenterView) {

		super(ownerControl);

		_tourSegmenterView = tourSegmenterView;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);

		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		final boolean isCanClose = _isAnotherDialogOpened == false;

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
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
	protected Composite createToolTipContentArea(final Composite parent) {

		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(4)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);
				createUI_20_Options(container);
				createUI_30_SegmentValues(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {
		{
			/*
			 * Label: Slideout title
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.applyTo(label);
			label.setText(Messages.Slideout_SegmenterChartOptions_Label_Title);
			label.setFont(JFaceResources.getBannerFont());
		}
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void createUI_20_Options(final Composite parent) {
		{
			/*
			 * Checkbox: Show segment marker
			 */
			_chkShowSegmentMarker = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.applyTo(_chkShowSegmentMarker);
			_chkShowSegmentMarker.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentMarker);
			_chkShowSegmentMarker.addSelectionListener(_defaultSelectionAdapter);
		}
		{
			/*
			 * Segment/graph opacity
			 */
			// Label
			_lblGraphOpacity = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblGraphOpacity);
			_lblGraphOpacity.setText(Messages.Slideout_SegmenterChartOptions_Label_GraphOpacity);
			_lblGraphOpacity.setToolTipText(//
					Messages.Slideout_SegmenterChartOptions_Label_GraphOpacity_Tooltip);

			// Spinner:
			_spinGraphOpacity = new Spinner(parent, SWT.BORDER);
			_spinGraphOpacity.setMinimum(0);
			_spinGraphOpacity.setMaximum(100);
			_spinGraphOpacity.setPageIncrement(10);
			_spinGraphOpacity.addSelectionListener(_defaultSelectionAdapter);
			_spinGraphOpacity.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private void createUI_30_SegmentValues(final Composite parent) {

		{
			/*
			 * Checkbox: Show segment value
			 */
			_chkShowSegmentValue = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(4, 1)
					.applyTo(_chkShowSegmentValue);
			_chkShowSegmentValue.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentValue);
			_chkShowSegmentValue.addSelectionListener(_defaultSelectionAdapter);
		}
		{
			/*
			 * Checkbox: Show decimal places
			 */
			_chkShowDecimalPlaces = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(_firstColumnIndent, 0)
					.applyTo(_chkShowDecimalPlaces);
			_chkShowDecimalPlaces.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowDecimalPlaces);
			_chkShowDecimalPlaces.addSelectionListener(_defaultSelectionAdapter);
		}
		{
			/*
			 * Number of stacked values
			 */
			// Label
			_lblVisibleStackedValues = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblVisibleStackedValues);
			_lblVisibleStackedValues.setText(Messages.Slideout_SegmenterChartOptions_Label_StackedValues);
			_lblVisibleStackedValues.setToolTipText(//
					Messages.Slideout_SegmenterChartOptions_Label_StackedValues_Tooltip);

			// Spinner:
			_spinVisibleValuesStacked = new Spinner(parent, SWT.BORDER);
			_spinVisibleValuesStacked.setMinimum(0);
			_spinVisibleValuesStacked.setMaximum(20);
			_spinVisibleValuesStacked.setPageIncrement(5);
			_spinVisibleValuesStacked.addSelectionListener(_defaultSelectionAdapter);
			_spinVisibleValuesStacked.addMouseWheelListener(_defaultMouseWheelListener);
		}
		{
			/*
			 * Hide small values
			 */

			final Composite containerSmall = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerSmall);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerSmall);
//			containerSmall.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				// Checkbox
				_chkHideSmallValues = new Button(containerSmall, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_firstColumnIndent, 0)
						.applyTo(_chkHideSmallValues);
				_chkHideSmallValues.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_HideSmallValues);
				_chkHideSmallValues
						.setToolTipText(Messages.Slideout_SegmenterChartOptions_Label_HideSmallValues_Tooltip);
				_chkHideSmallValues.addSelectionListener(_defaultSelectionAdapter);

				// Spinner
				_spinHideSmallValues = new Spinner(containerSmall, SWT.BORDER);
				_spinHideSmallValues.setMinimum(0);
				_spinHideSmallValues.setMaximum(500);
				_spinHideSmallValues.setDigits(1);
				_spinHideSmallValues.setPageIncrement(10);
				_spinHideSmallValues.addSelectionListener(_defaultSelectionAdapter);
				_spinHideSmallValues.addMouseWheelListener(_defaultMouseWheelListener);

				// Label %
				_lblHideSmallValuesUnit = new Label(containerSmall, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblHideSmallValuesUnit);
				_lblHideSmallValuesUnit.setText(UI.SYMBOL_PERCENTAGE);
			}
		}
		{
			/*
			 * Font editor
			 */
			_fontContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(4, 1)
					.applyTo(_fontContainer);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_fontContainer);
			{
				_valueFontEditor = new FontFieldEditorExtended(
						ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT,
						Messages.Slideout_SegmenterChartOptions_Label_ValueFont,
						Messages.Slideout_SegmenterChartOptions_Label_ValueFont_Example,
						_fontContainer);

				_valueFontEditor.setPropertyChangeListener(_defaultChangePropertyListener);
				_valueFontEditor.addOpenListener(this);
				_valueFontEditor.setFirstColumnIndent(_firstColumnIndent, 0);
			}
		}
	}

	private void enableControls() {

		final boolean isShowValues = _chkShowSegmentValue.getSelection();
		final boolean isHideSmallValues = _chkHideSmallValues.getSelection();

		_chkShowDecimalPlaces.setEnabled(isShowValues);

		_chkHideSmallValues.setEnabled(isShowValues);
		_lblHideSmallValuesUnit.setEnabled(isShowValues && isHideSmallValues);
		_spinHideSmallValues.setEnabled(isShowValues && isHideSmallValues);

		_lblVisibleStackedValues.setEnabled(isShowValues);
		_spinVisibleValuesStacked.setEnabled(isShowValues);

		_valueFontEditor.setEnabled(isShowValues, _fontContainer);
	}

	@Override
	public void fontDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x + itemWidth / 2 - tipWidth / 2;
		int devY = _toolTipItemBounds.y + itemHeight + 0;

		final Rectangle displayBounds = _shellContainer.getShell().getDisplay().getBounds();

		if (devY + tipHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = _toolTipItemBounds.y - tipHeight;
		}

		return new Point(devX, devY);

	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeFontInEditor() {

		enableControls();

		final Shell shell = _shellContainer.getShell();
		shell.pack(true);

		/*
		 * Update state
		 */
		_valueFontEditor.store();

		/*
		 * Update UI
		 */
		_tourSegmenterView.fireSegmentLayerChanged();
	}

	private void onChangeUI() {

		enableControls();

		saveState();

		/*
		 * Update UI
		 */
		_tourSegmenterView.fireSegmentLayerChanged();
	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {
			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	private void resetToDefaults() {

		// set font editor default values
		_valueFontEditor.loadDefault();
		_valueFontEditor.store();

		// hide small values
		_segmenterState.put(
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES_DEFAULT);
		_segmenterState.put(//
				TourSegmenterView.STATE_SMALL_VALUE_SIZE,
				TourSegmenterView.STATE_SMALL_VALUE_SIZE_DEFAULT);

		_segmenterState.put(
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT);
		_segmenterState.put(
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT);
		_segmenterState.put(
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT);

		_segmenterState.put(//
				TourSegmenterView.STATE_GRAPH_ALPHA,
				TourSegmenterView.STATE_GRAPH_ALPHA_DEFAULT);
		_segmenterState.put(
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES_DEFAULT);

		// update UI with the default values from the state/pref store
		restoreState();

		enableControls();

		/*
		 * Update UI
		 */
		_tourSegmenterView.fireSegmentLayerChanged();
	}

	private void restoreState() {

		// hide small values
		_chkHideSmallValues.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES,
				TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES_DEFAULT));
		_spinHideSmallValues.setSelection(Util.getStateInt(
				_segmenterState,
				TourSegmenterView.STATE_SMALL_VALUE_SIZE,
				TourSegmenterView.STATE_SMALL_VALUE_SIZE_DEFAULT));

		_chkShowDecimalPlaces.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES_DEFAULT));

		_chkShowSegmentMarker.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT));

		_chkShowSegmentValue.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT));

		_spinGraphOpacity.setSelection(Util.getStateInt(
				_segmenterState,
				TourSegmenterView.STATE_GRAPH_ALPHA,
				TourSegmenterView.STATE_GRAPH_ALPHA_DEFAULT));

		_spinVisibleValuesStacked.setSelection(Util.getStateInt(
				_segmenterState,
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES,
				TourSegmenterView.STATE_STACKED_VISIBLE_VALUES_DEFAULT));

		restoreState_Font();
	}

	private void restoreState_Font() {

		_valueFontEditor.setPreferenceStore(_prefStore);
		_valueFontEditor.load();
	}

	private void saveState() {

		// !!! font editor saves it's values automatically !!!

		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_DECIMAL_PLACES,//
				_chkShowDecimalPlaces.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER, //
				_chkShowSegmentMarker.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE, //
				_chkShowSegmentValue.getSelection());

		_segmenterState.put(TourSegmenterView.STATE_IS_HIDE_SMALL_VALUES, //
				_chkHideSmallValues.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_SMALL_VALUE_SIZE, //
				_spinHideSmallValues.getSelection());

		_segmenterState.put(TourSegmenterView.STATE_GRAPH_ALPHA, //
				_spinGraphOpacity.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_STACKED_VISIBLE_VALUES, //
				_spinVisibleValuesStacked.getSelection());
	}

}
