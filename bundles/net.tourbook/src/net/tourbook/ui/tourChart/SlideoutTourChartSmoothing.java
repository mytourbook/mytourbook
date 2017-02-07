/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.PrefPageComputedValues;
import net.tourbook.ui.views.SmoothingUI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Tour chart properties slideout.
 */
public class SlideoutTourChartSmoothing extends ToolbarSlideout {

	private ActionOpenPrefDialog	_actionPrefDialog;

	private Action					_actionRestoreDefaults;
	/*
	 * UI controls
	 */
	private TourChart				_tourChart;

	private SmoothingUI				_smoothingUI;
	private FormToolkit				_tk;

	private Composite				_parent;

	public class SlideoutSmoothingUI extends SmoothingUI {

		public SlideoutSmoothingUI(final FormToolkit tk, final ToolbarSlideout slideout) {
			super(tk, slideout);
		}

		@Override
		protected void onModifySmoothingAlgo() {

			// pack the UI, it could have been changed
			_parent.getShell().pack(true);

			_parent.update();
		}
	}

	public SlideoutTourChartSmoothing(final Control ownerControl, final ToolBar toolBar, final TourChart tourChart) {

		super(ownerControl, toolBar);

		_tourChart = tourChart;
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
				Messages.Tour_Action_EditSmoothingPreferences,
				PrefPageComputedValues.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_tourChart.getShell());

		// select smoothing folder when opened
		_actionPrefDialog.setPrefData(PrefPageComputedValues.TAB_FOLDER_SMOOTHING);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		_parent = parent;

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		// set background color to a dialog background color otherwise it would be white
		_tk.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

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
			}

			final Composite smoothingContainer = new Composite(shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(smoothingContainer);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(smoothingContainer);
			{
				_smoothingUI = new SlideoutSmoothingUI(_tk, this);
				_smoothingUI.createUI(smoothingContainer, false, false);
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
		label.setText(Messages.Slideout_TourChartSmoothing_Label_Title);
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

	private void initUI(final Composite parent) {

	}

	private void onChangeUI() {

		saveState();

		// update chart with new settings
		_tourChart.updateTourChart();
	}

	@Override
	protected void onDispose() {

		_smoothingUI.dispose();
	}

	private void resetToDefaults() {

		_smoothingUI.performDefaults();

		onChangeUI();
	}

	private void restoreState() {

	}

	private void saveState() {

	}

}
