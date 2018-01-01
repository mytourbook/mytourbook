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
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour info slideout.
 */
public class SlideoutLinkWithOtherViews extends ToolbarSlideout {

	private IDialogSettings		_state;

	private SelectionAdapter	_defaultSelectionListener;
	private MouseWheelListener	_defaultMouseWheelListener;

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
	}

	private PixelConverter	_pc;

	private TourBookView	_tourbookView;

	/*
	 * UI controls
	 */
	private Button			_chkShowInfoTitle;

	public SlideoutLinkWithOtherViews(	final Control ownerControl,
										final ToolBar toolBar,
										final TourBookView tourbookView) {

		super(ownerControl, toolBar);

		_tourbookView = tourbookView;
		_state = _tourbookView.getState();
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

	private void createActions() {

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
		label.setText(Messages.Slideout_LinkWithOtherViews_Label_Title);

		MTFont.setBannerFont(label);
	}

	private void createUI_20_Controls(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Show tour title
				 */
				_chkShowInfoTitle = new Button(container, SWT.CHECK);
				_chkShowInfoTitle.setText(Messages.Slideout_LinkWithOtherViews_Label_Title_Checkbox_IsShowTourTitle);
				_chkShowInfoTitle.setToolTipText(
						Messages.Slideout_LinkWithOtherViews_Label_Title_Checkbox_IsShowTourTitle_Tooltip);
				_chkShowInfoTitle.addSelectionListener(_defaultSelectionListener);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowInfoTitle);
			}
		}
	}

	private void enableControls() {

	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	private void onChangeUI() {

		final boolean isCollapseOthers = _chkShowInfoTitle.getSelection();

		_state.put(TourBookView.STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS, isCollapseOthers);

		_tourbookView.setLinkAndCollapse(isCollapseOthers);

		enableControls();
	}

	private void restoreState() {

		_chkShowInfoTitle.setSelection(
				Util.getStateBoolean(
						_state,
						TourBookView.STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS,
						TourBookView.STATE_LINK_AND_COLLAPSE_ALL_OTHER_ITEMS_DEFAULT));
	}

}
