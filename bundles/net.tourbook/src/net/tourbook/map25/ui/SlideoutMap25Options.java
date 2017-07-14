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
package net.tourbook.map25.ui;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2.5D properties slideout.
 */
public class SlideoutMap25Options extends ToolbarSlideout {

	private SelectionAdapter	_defaultSelectionListener;

	private Action				_actionRestoreDefaults;

	private PixelConverter		_pc;

	private Font				_boldFont;
	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private Map25View	_map25View;

	/*
	 * UI controls
	 */
	private Button		_chkShowLayer_Building;
	private Button		_chkShowLayer_Map;
	private Button		_chkShowLayer_Scale;
	private Button		_chkShowLayer_TextLabel;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map25View
	 */
	public SlideoutMap25Options(final Control ownerControl,
								final ToolBar toolBar,
								final Map25View map25View) {

		super(ownerControl, toolBar);

		_map25View = map25View;
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

		initUI(parent);

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
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			{
				createUI_10_Title(container);
				createUI_12_Actions(container);

				createUI_20_Layer(container);
			}
		}

		return shellContainer;
	}

	private void createUI_10_Title(final Composite parent) {

		/*
		 * Label: Slideout title
		 */
		final Label label = new Label(parent, SWT.NONE);
		label.setFont(_boldFont);
		label.setText("Map Options");
		GridDataFactory
				.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(label);
	}

	private void createUI_12_Actions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.END, SWT.BEGINNING)
				.indent(_pc.convertWidthInCharsToPixels(5), 0)
				.applyTo(toolbar);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_actionRestoreDefaults);

		tbm.update(true);
	}

	private void createUI_20_Layer(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText("2.5D Map Layer");
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			{
				/*
				 * Text label
				 */
				_chkShowLayer_TextLabel = new Button(group, SWT.CHECK);
				_chkShowLayer_TextLabel.setText("Text &label");
				_chkShowLayer_TextLabel.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Building
				 */
				_chkShowLayer_Building = new Button(group, SWT.CHECK);
				_chkShowLayer_Building.setText("&Building");
				_chkShowLayer_Building.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Scale
				 */
				_chkShowLayer_Scale = new Button(group, SWT.CHECK);
				_chkShowLayer_Scale.setText("&Scale bar");
				_chkShowLayer_Scale.addSelectionListener(_defaultSelectionListener);
			}
			{
				/*
				 * Map
				 */
				_chkShowLayer_Map = new Button(group, SWT.CHECK);
				_chkShowLayer_Map.setText("&Map (background)");
				_chkShowLayer_Map.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	private void onChangeUI() {

		saveState();
	}

	private void resetToDefaults() {

		onChangeUI();
	}

	private void restoreState() {

	}

	private void saveState() {

	}

}
