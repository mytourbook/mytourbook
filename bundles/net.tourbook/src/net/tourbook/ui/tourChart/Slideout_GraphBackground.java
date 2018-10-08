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
package net.tourbook.ui.tourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;

/**
 * Tour chart properties slideout.
 */
public class Slideout_GraphBackground extends ToolbarSlideout {

	private static final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();
	private IDialogSettings						_state;

	private Action									_actionRestoreDefaults;
	private ActionOpenPrefDialog				_actionPrefDialog;

	private SelectionAdapter					_defaultSelectionListener;

	private SelectionAdapter					_defaultSelectionAdapter;
	private MouseWheelListener					_defaultMouseWheelListener;
	private IPropertyChangeListener			_defaultPropertyChangeListener;
	private FocusListener						_keepOpenListener;

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

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI();
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};
	}

	/*
	 * UI controls
	 */
	private TourChart	_tourChart;

	private Combo		_comboGraphBgStyle;
	private Combo		_comboGraphBgSource;

	public Slideout_GraphBackground(	final Control ownerControl,
												final ToolBar toolBar,
												final TourChart tourChart,
												final IDialogSettings state) {

		super(ownerControl, toolBar);

		_tourChart = tourChart;
		_state = state;
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

		_actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

		_actionPrefDialog = new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID);

		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_tourChart.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		fillUI();
		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);
			}
			createUI_20_Graphs(shellContainer);
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourChartGraphBackground_Label_Title);
		MTFont.setBannerFont(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);
		tbm.add(_actionPrefDialog);

		tbm.update(true);
	}

	private void createUI_20_Graphs(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Combo: Graph source
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_TourChartGraphBackground_Label_BackgroundSource);
				GridDataFactory.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				_comboGraphBgSource = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboGraphBgSource.setVisibleItemCount(20);
				_comboGraphBgSource.setToolTipText(Messages.Slideout_TourChartGraphBackground_Combo_BackgroundSource_Tooltip);
				_comboGraphBgSource.addSelectionListener(_defaultSelectionAdapter);
				_comboGraphBgSource.addFocusListener(_keepOpenListener);
//				GridDataFactory.fillDefaults()
//						.grab(true, false)
//						.align(SWT.END, SWT.FILL)
//						.applyTo(_comboBackgroundSource);
			}
			{
				/*
				 * Combo: Background style
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_TourChartGraphBackground_Label_BackgroundStyle);
				GridDataFactory.fillDefaults()
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);

				_comboGraphBgStyle = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboGraphBgStyle.setVisibleItemCount(20);
				_comboGraphBgStyle.addSelectionListener(_defaultSelectionAdapter);
				_comboGraphBgStyle.addFocusListener(_keepOpenListener);
//				GridDataFactory.fillDefaults()
//						.grab(true, false)
//						.align(SWT.END, SWT.FILL)
//						.applyTo(_comboBackgroundStyle);
			}
		}
	}

	private void fillUI() {

		for (final GraphBgSourceType bgSource : TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_TYPE) {
			_comboGraphBgSource.add(bgSource.label);
		}

		for (final GraphBgStyleType bgStyle : TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE) {
			_comboGraphBgStyle.add(bgStyle.label);
		}

	}

	private void initUI(final Composite parent) {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		saveState();

		updateGraphUI();
	}

	@Override
	protected void onDispose() {

	}

	private void resetToDefaults() {

		select_GraphBgSource(TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_DEFAULT);
		select_GraphBgStyle(TourChartConfiguration.GRAPH_BACKGROUND_STYLE_DEFAULT);

		saveState();

		updateGraphUI();
	}

	private void restoreState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		// graph background
		select_GraphBgSource(tcc.graphBackground_Source);
		select_GraphBgStyle(tcc.graphBackground_Style);
	}

	private void saveState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

// SET_FORMATTING_OFF

		final GraphBgSourceType graphBgSourceType 	= TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_TYPE[_comboGraphBgSource.getSelectionIndex()];
		final GraphBgStyleType graphBgStyleType 		= TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE[_comboGraphBgStyle.getSelectionIndex()];

		final GraphBgSource graphBgSource 				= graphBgSourceType.graphBgSource;
		final GraphBackgroundStyle graphBgStyle 		= graphBgStyleType.graphBgStyle;

		/*
		 * Update pref store
		 */
		_prefStore.setValue(ITourbookPreferences.GRAPH_BACKGROUND_SOURCE, graphBgSource.name());
		_prefStore.setValue(ITourbookPreferences.GRAPH_BACKGROUND_STYLE, graphBgStyle.name());

		/*
		 * Update chart config
		 */
		tcc.graphBackground_Source 	= graphBgSource;
		tcc.graphBackground_Style 		= graphBgStyle;

// SET_FORMATTING_ON
	}

	private void select_GraphBgSource(final GraphBgSource graphBgSource) {

		final GraphBgSourceType[] graphBgSourceType = TourChartConfiguration.GRAPH_BACKGROUND_SOURCE_TYPE;
		for (int itemIndex = 0; itemIndex < graphBgSourceType.length; itemIndex++) {

			final GraphBgSourceType bgSource = graphBgSourceType[itemIndex];

			if (bgSource.graphBgSource.equals(graphBgSource)) {
				_comboGraphBgSource.select(itemIndex);
				return;
			}
		}

		// set default
		_comboGraphBgSource.select(0);
	}

	private void select_GraphBgStyle(final GraphBackgroundStyle graphBgStyle) {

		final GraphBgStyleType[] graphBgStyleType = TourChartConfiguration.GRAPH_BACKGROUND_STYLE_TYPE;
		for (int itemIndex = 0; itemIndex < graphBgStyleType.length; itemIndex++) {

			final GraphBgStyleType bgSource = graphBgStyleType[itemIndex];

			if (bgSource.graphBgStyle.equals(graphBgStyle)) {
				_comboGraphBgStyle.select(itemIndex);
				return;
			}
		}

		// set default
		_comboGraphBgSource.select(0);
	}

	private void updateGraphUI() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		if (tcc.isGraphBgStyleVisible == false) {

			// graph background style is not yet displayed
			_tourChart.action_ShowGraphBgStyle();

		} else {

			_tourChart.updateTourChart();
		}
	}
}
