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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
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
public class SlideoutTourChartInfo extends AnimatedToolTipShell implements IColorSelectorListener {

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private IPropertyChangeListener	_defaultChangePropertyListener;
	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;

	{
		_defaultSelectionListener = new SelectionAdapter() {
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
				onChangeUI();
			}
		};
	}

	private PixelConverter			_pc;

	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;
	private TourChart				_tourChart;

	private Button					_chkShowInfoTitle;
	private Button					_chkShowInfoTooltip;
	private Button					_chkShowInfoTourSeparator;
	private Button					_chkSegmentAlternateColor;

	private Label					_lblTooltipDelay;

	private Spinner					_spinnerTooltipDelay;

	private ColorSelectorExtended	_colorSegmentAlternateColor;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartInfo(	final Control ownerControl,
									final ToolBar toolBar,
									final IDialogSettings state,
									final TourChart tourChart) {

		super(ownerControl);

		_tourChart = tourChart;

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
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
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

		_actionPrefDialog = new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID);

		/*
		 * Set shell to the parent otherwise the pref dialog is closed when the slideout is closed.
		 */
		_actionPrefDialog.setShell(_tourChart.getShell());
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

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);
				createUI_20_Controls(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourInfoOptions_Label_Title);
		label.setFont(JFaceResources.getBannerFont());
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);
		tbm.add(_actionPrefDialog);

		tbm.update(true);
	}

	private void createUI_20_Controls(final Composite parent) {

		final Composite ttContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(ttContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ttContainer);
		{
			{
				/*
				 * Show tour title
				 */
				_chkShowInfoTitle = new Button(ttContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowInfoTitle);
				_chkShowInfoTitle.setText(//
						Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourTitle);
				_chkShowInfoTitle.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Show tour separator
				 */
				_chkShowInfoTourSeparator = new Button(ttContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowInfoTourSeparator);
				_chkShowInfoTourSeparator.setText(//
						Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourSeparator);
				_chkShowInfoTourSeparator.setToolTipText(//
						Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourSeparator_Tooltip);
				_chkShowInfoTourSeparator.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Checkbox: Segments with alternate colors
				 */
				_chkSegmentAlternateColor = new Button(ttContainer, SWT.CHECK);
				_chkSegmentAlternateColor.setText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor);
				_chkSegmentAlternateColor.setToolTipText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor_Tooltip);
				_chkSegmentAlternateColor.addSelectionListener(_defaultSelectionListener);

				// Color: Segment alternate color
				_colorSegmentAlternateColor = new ColorSelectorExtended(ttContainer);
				_colorSegmentAlternateColor.addListener(_defaultChangePropertyListener);
				_colorSegmentAlternateColor.addOpenListener(this);
			}
			{
				/*
				 * Show info tooltip
				 */
				_chkShowInfoTooltip = new Button(ttContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowInfoTooltip);
				_chkShowInfoTooltip.setText(//
						Messages.Slideout_TourInfoOptions_Checkbox_IsShowInfoTooltip);
				_chkShowInfoTooltip.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Tooltip delay
				 */
				// Label
				_lblTooltipDelay = new Label(ttContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(_pc.convertWidthInCharsToPixels(3), 0)
						.applyTo(_lblTooltipDelay);
				_lblTooltipDelay.setText(Messages.Slideout_TourInfoOptions_Label_TooltipDelay);
				_lblTooltipDelay.setToolTipText(Messages.Slideout_TourInfoOptions_Label_TooltipDelay_Tooltip);

				// Spinner
				_spinnerTooltipDelay = new Spinner(ttContainer, SWT.BORDER);
				_spinnerTooltipDelay.setMinimum(0);
				_spinnerTooltipDelay.setMaximum(1000);
				_spinnerTooltipDelay.setPageIncrement(50);
				_spinnerTooltipDelay.addSelectionListener(_defaultSelectionListener);
				_spinnerTooltipDelay.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void enableControls() {

		final boolean isShowInfoTooltip = _chkShowInfoTooltip.getSelection();

		_lblTooltipDelay.setEnabled(isShowInfoTooltip);
		_spinnerTooltipDelay.setEnabled(isShowInfoTooltip);
	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
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

		final Rectangle displayBounds = this.getShell().getDisplay().getBounds();

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

	private void onChangeUI() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		final boolean isShowInfoTitle = _chkShowInfoTitle.getSelection();
		final boolean isShowInfoTooltip = _chkShowInfoTooltip.getSelection();
		final boolean isShowInfoTourSeparator = _chkShowInfoTourSeparator.getSelection();
		final int tooltipDelay = _spinnerTooltipDelay.getSelection();

		/*
		 * Update pref store
		 */
		_prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TITLE_VISIBLE, isShowInfoTitle);
		_prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE, isShowInfoTooltip);
		_prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE, isShowInfoTourSeparator);
		_prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_TOOLTIP_DELAY, tooltipDelay);

		// segment alternate color
		_prefStore.setValue(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR,//
				_chkSegmentAlternateColor.getSelection());
		PreferenceConverter.setValue(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR,//
				_colorSegmentAlternateColor.getColorValue());

		/*
		 * Update chart config
		 */
		tcc.isShowInfoTitle = isShowInfoTitle;
		tcc.isShowInfoTooltip = isShowInfoTooltip;
		tcc.isShowInfoTourSeparator = isShowInfoTourSeparator;
		tcc.tourInfoTooltipDelay = tooltipDelay;

		// update chart with new settings
		_tourChart.updateUI_TourTitleInfo();

		enableControls();
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

		/*
		 * Update UI with defaults from pref store
		 */
		_chkShowInfoTitle.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TITLE_VISIBLE));
		_chkShowInfoTooltip.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE));
		_chkShowInfoTourSeparator.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE));

		_spinnerTooltipDelay.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TOUR_INFO_TOOLTIP_DELAY));

		// segment alternate color
		_chkSegmentAlternateColor.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
		_colorSegmentAlternateColor.setColorValue(//
				PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));

		onChangeUI();
	}

	private void restoreState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		_chkShowInfoTitle.setSelection(tcc.isShowInfoTitle);
		_chkShowInfoTooltip.setSelection(tcc.isShowInfoTooltip);
		_chkShowInfoTourSeparator.setSelection(tcc.isShowInfoTourSeparator);

		_spinnerTooltipDelay.setSelection(tcc.tourInfoTooltipDelay);

		// segment alternate color
		_chkSegmentAlternateColor.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
		_colorSegmentAlternateColor.setColorValue(//
				PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));
	}

}
