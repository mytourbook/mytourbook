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
package net.tourbook.ui.views.tourBook;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class SlideoutTourBookOptions extends ToolbarSlideout {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private SelectionAdapter		_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private Button					_chkShowSummaryRow;

	private TourBookView			_tourBookView;

	private IDialogSettings			_state;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param tourBookView
	 * @param state
	 * @param gridPrefPrefix
	 */
	public SlideoutTourBookOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final TourBookView tourBookView,
									final IDialogSettings state) {

		super(ownerControl, toolBar);

		_tourBookView = tourBookView;
		_state = state;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();

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
//					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_20_Controls(container);
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
		label.setText(Messages.Slideout_TourBookOptions_Label_Title);
		label.setFont(JFaceResources.getBannerFont());

		MTFont.setBannerFont(label);
	}

	private void createUI_20_Controls(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				//				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Show break time values
				 */
				_chkShowSummaryRow = new Button(container, SWT.CHECK);
				_chkShowSummaryRow.setText(Messages.Slideout_TourBookOptions_Checkbox_ShowTotalRow);
				_chkShowSummaryRow.setToolTipText(Messages.Slideout_TourBookOptions_Checkbox_ShowTotalRow_Tooltip);

				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowSummaryRow);

				_chkShowSummaryRow.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		saveState();

		// update chart with new settings
		_tourBookView.updateTourBookOptions();
	}

	private void restoreState() {

		_chkShowSummaryRow.setSelection(Util.getStateBoolean(_state,
				TourBookView.STATE_IS_SHOW_SUMMARY_ROW,
				TourBookView.STATE_IS_SHOW_SUMMARY_ROW_DEFAULT));
	}

	private void saveState() {

		_state.put(TourBookView.STATE_IS_SHOW_SUMMARY_ROW, _chkShowSummaryRow.getSelection());
	}

}
