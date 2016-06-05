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
package net.tourbook.statistic;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 */
public class SlideoutStatisticsChartOptions extends ToolbarSlideout {

	private ActionOpenPrefDialog					_actionPrefDialog;
	private Action									_actionRestoreDefaults;

	private ChartOptions_Grid						_gridUI			= new ChartOptions_Grid();
	private ChartOptions_Statistics_TourFrequency	_statisticUI	= new ChartOptions_Statistics_TourFrequency();

	/*
	 * UI controls
	 */
	private Shell									_parentShell;
	private boolean									_isOption_ShowTourFrequency;

	public SlideoutStatisticsChartOptions(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl, toolBar);

		_parentShell = ownerControl.getShell();
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
		_actionPrefDialog.setShell(_parentShell);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			final Composite container = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);

				if (_isOption_ShowTourFrequency) {
					_statisticUI.createUI(container);
				}

				_gridUI.createUI(container);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.Slideout_TourChartOptions_Label_Title);
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

	void resetOptions() {

		_isOption_ShowTourFrequency = false;
	}

	private void resetToDefaults() {

		_gridUI.resetToDefaults();
		_gridUI.saveState();

		if (_isOption_ShowTourFrequency) {
			_statisticUI.resetToDefaults();
			_statisticUI.saveState();
		}
	}

	private void restoreState() {

		_gridUI.restoreState();

		if (_isOption_ShowTourFrequency) {
			_statisticUI.restoreState();
		}
	}

	public void setOption_IsShowTourFrequency() {

		_isOption_ShowTourFrequency = true;
	}
}
