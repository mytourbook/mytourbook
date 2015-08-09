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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
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
public class SlideoutTourChartSegmenterConfig extends AnimatedToolTipShell implements IFontDialogListener {

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

	private TourSegmenterView				_tourSegmenterView;

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;
	private Composite						_ttContainer;

	private Button							_chkShowSegmentMarker;
	private Button							_chkShowSegmentValue;

	private FontFieldEditorExtended			_valueFontEditor;

	private Label							_lblVisibleStackedValues;
	private Label							_lblFontSize;

	private Spinner							_spinFontSize;
	private Spinner							_spinVisibleValuesStacked;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartSegmenterConfig(final Control ownerControl,
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

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().applyTo(container);
			{
				createUI_10_Controls(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Controls(final Composite parent) {

		final int firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

		_ttContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_ttContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_ttContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Label: Title
				 */
				final Label label = new Label(_ttContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(label);
				label.setText(Messages.Slideout_SegmenterChartOptions_Label_Title);

			}
			{
				/*
				 * Checkbox: Show segment marker
				 */
				_chkShowSegmentMarker = new Button(_ttContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowSegmentMarker);
				_chkShowSegmentMarker.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentMarker);
				_chkShowSegmentMarker.addSelectionListener(_defaultSelectionAdapter);
			}
			{
				/*
				 * Checkbox: Show segment value
				 */
				_chkShowSegmentValue = new Button(_ttContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowSegmentValue);
				_chkShowSegmentValue.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentValue);
				_chkShowSegmentValue.addSelectionListener(_defaultSelectionAdapter);
			}
			{
				/*
				 * Number of stacked values
				 */
				// Label
				_lblVisibleStackedValues = new Label(_ttContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(firstColumnIndent, 0)
						.applyTo(_lblVisibleStackedValues);
				_lblVisibleStackedValues.setText(Messages.Slideout_SegmenterChartOptions_Label_StackedValues);
				_lblVisibleStackedValues.setToolTipText(//
						Messages.Slideout_SegmenterChartOptions_Label_StackedValues_Tooltip);

				// Spinner:
				_spinVisibleValuesStacked = new Spinner(_ttContainer, SWT.BORDER);
				_spinVisibleValuesStacked.setMinimum(0);
				_spinVisibleValuesStacked.setMaximum(20);
				_spinVisibleValuesStacked.setPageIncrement(2);
				_spinVisibleValuesStacked.addSelectionListener(_defaultSelectionAdapter);
				_spinVisibleValuesStacked.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Font size
				 */
				// Label
				_lblFontSize = new Label(_ttContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(firstColumnIndent, 0)
						.applyTo(_lblFontSize);
				_lblFontSize.setText(Messages.Slideout_SegmenterChartOptions_Label_FontSize);

				// Spinner
				_spinFontSize = new Spinner(_ttContainer, SWT.BORDER);
				_spinFontSize.setMinimum(2);
				_spinFontSize.setMaximum(100);
				_spinFontSize.setPageIncrement(5);
				_spinFontSize.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeFontSize();
					}
				});
				_spinFontSize.addMouseWheelListener(new MouseWheelListener() {

					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeFontSize();
					}
				});
			}
			{
				/*
				 * Font editor
				 */
				_valueFontEditor = new FontFieldEditorExtended(
						ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT,
						Messages.Slideout_SegmenterChartOptions_Label_ValueFont,
						Messages.Slideout_SegmenterChartOptions_Label_ValueFont_Example,
						_ttContainer);

				_valueFontEditor.setPropertyChangeListener(_defaultChangePropertyListener);
				_valueFontEditor.addOpenListener(this);
				_valueFontEditor.setFirstColumnIndent(firstColumnIndent, 0);
			}
		}
	}

	private void enableControls() {

		final boolean isShowValues = _chkShowSegmentValue.getSelection();

		_lblFontSize.setEnabled(isShowValues);
		_spinFontSize.setEnabled(isShowValues);

		_lblVisibleStackedValues.setEnabled(isShowValues);
		_spinVisibleValuesStacked.setEnabled(isShowValues);

		_valueFontEditor.setEnabled(isShowValues, _ttContainer);
	}

	@Override
	public void fontDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
		final int tipHeight = tipSize.y;

//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
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

		updateUI_FontSize();

		/*
		 * Update UI
		 */
		_tourSegmenterView.fireSegmentLayerChanged();
	}

	private void onChangeFontSize() {

		final FontData[] selectedFont = PreferenceConverter.getFontDataArray(
				_prefStore,
				ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT);

		final FontData font = selectedFont[0];
		font.setHeight(_spinFontSize.getSelection());

		final FontData[] validFont = JFaceResources.getFontRegistry().filterData(
				selectedFont,
				_shellContainer.getDisplay());

		if (validFont == null) {

			// selected font size is not valid

		} else {

			PreferenceConverter.setValue(_prefStore, //
					ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT,
					selectedFont);

			saveState();

			// update font editor
			_valueFontEditor.load();

			// Update UI
			_tourSegmenterView.fireSegmentLayerChanged();
		}
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

	private void restoreState() {

		_chkShowSegmentMarker.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER_DEFAULT));

		_chkShowSegmentValue.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE_DEFAULT));

		_spinVisibleValuesStacked.setSelection(Util.getStateInt(
				_segmenterState,
				TourSegmenterView.STATE_SEGMENTER_STACKED_VISIBLE_VALUES,
				TourSegmenterView.STATE_SEGMENTER_STACKED_VISIBLE_VALUES_DEFAULT));

		restoreState_Font();
	}

	private void restoreState_Font() {

		updateUI_FontSize();

		_valueFontEditor.setPreferenceStore(_prefStore);
		_valueFontEditor.load();
	}

	private void saveState() {

		// !!! font editor saves it's values automatically !!!

		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_MARKER, _chkShowSegmentMarker.getSelection());
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUE, _chkShowSegmentValue.getSelection());

		_segmenterState.put(TourSegmenterView.STATE_SEGMENTER_STACKED_VISIBLE_VALUES,//
				_spinVisibleValuesStacked.getSelection());
	}

	private void updateUI_FontSize() {

		// update font size widget

		final FontData[] selectedFont = PreferenceConverter.getFontDataArray(
				_prefStore,
				ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT);

		final FontData font = selectedFont[0];
		final int fontHeight = font.getHeight();

		_spinFontSize.setSelection(fontHeight);
	}

}
