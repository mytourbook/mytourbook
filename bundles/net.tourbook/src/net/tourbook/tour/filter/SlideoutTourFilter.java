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
package net.tourbook.tour.filter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.PrefPageAppearanceTourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourFilter extends ToolbarSlideout implements IColorSelectorListener {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private PixelConverter			_pc;

	private ActionOpenPrefDialog	_actionPrefDialog;
	private Action					_actionRestoreDefaults;

	/*
	 * UI controls
	 */
	private Shell					_appShell;

	public SlideoutTourFilter(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl, toolBar);

		_appShell = ownerControl.getShell();
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

		_actionPrefDialog = new ActionOpenPrefDialog(
				Messages.Tour_Action_EditChartPreferences,
				PrefPageAppearanceTourChart.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_appShell);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);
		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

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
				createUI_20_Filter_Photo(container);
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
		label.setText(Messages.Slideout_TourFilter_Label_Title);
		label.setFont(JFaceResources.getBannerFont());
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

	private void createUI_20_Filter_Photo(final Composite parent) {

//		Action_TourPhotoFilter         = Show only tours with photos
//				Action_TourPhotoFilter_Tooltip = Tour Photo Filter:\n\
//				                                 \n\
//				                                 When this filter is activated, only tours with photos \n\
//				                                 will be displayed in ALL views, except the tour import view.

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

		}
	}

	private void enableControls() {

	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	private void onChangeUI() {

		enableControls();
	}

	private void resetToDefaults() {

		onChangeUI();
	}

	private void restoreState() {

	}

}
