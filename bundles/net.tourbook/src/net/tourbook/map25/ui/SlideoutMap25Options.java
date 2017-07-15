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

import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25View;

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

	private SelectionAdapter	_layerSelectionListener;

//	private Action				_actionRestoreDefaults;

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
	private Button		_chkShowLayer_TileInfo;
	private Button		_chkShowLayer_BaseMap;
	private Button		_chkShowLayer_Scale;
	private Button		_chkShowLayer_Label;

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
//		_actionRestoreDefaults = new Action() {
//			@Override
//			public void run() {
//				resetToDefaults();
//			}
//		};
//
//		_actionRestoreDefaults.setImageDescriptor(//
//				TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
//		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
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

//		tbm.add(_actionRestoreDefaults);

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
				_chkShowLayer_Label = new Button(group, SWT.CHECK);
				_chkShowLayer_Label.setText("&Label + Symbol");
				_chkShowLayer_Label.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Building
				 */
				_chkShowLayer_Building = new Button(group, SWT.CHECK);
				_chkShowLayer_Building.setText("&3D Building");
				_chkShowLayer_Building.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Scale
				 */
				_chkShowLayer_Scale = new Button(group, SWT.CHECK);
				_chkShowLayer_Scale.setText("&Scale bar");
				_chkShowLayer_Scale.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Map
				 */
				_chkShowLayer_BaseMap = new Button(group, SWT.CHECK);
				_chkShowLayer_BaseMap.setText("&Cartography");
				_chkShowLayer_BaseMap.setToolTipText(
						"When hiding this layer then label + symbols will not be updated !");
				_chkShowLayer_BaseMap.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Tile info
				 */
				_chkShowLayer_TileInfo = new Button(group, SWT.CHECK);
				_chkShowLayer_TileInfo.setText("&Tile info");
				_chkShowLayer_TileInfo.addSelectionListener(_layerSelectionListener);
			}
		}
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_layerSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyLayer();
			}
		};
	}

	private void onModifyLayer() {

		final Map25App mapApp = _map25View.getMapApp();

		mapApp.getLayer_BaseMap().setEnabled(_chkShowLayer_BaseMap.getSelection());
		mapApp.getLayer_Building().setEnabled(_chkShowLayer_Building.getSelection());
		mapApp.getLayer_Label().setEnabled(_chkShowLayer_Label.getSelection());
		mapApp.getLayer_TileInfo().setEnabled(_chkShowLayer_TileInfo.getSelection());
		mapApp.getLayer_ScaleBar().setEnabled(_chkShowLayer_Scale.getSelection());

		mapApp.getMap().updateMap(true);
	}

	private void restoreState() {

		final Map25App mapApp = _map25View.getMapApp();

		_chkShowLayer_BaseMap.setSelection(mapApp.getLayer_BaseMap().isEnabled());
		_chkShowLayer_Building.setSelection(mapApp.getLayer_Building().isEnabled());
		_chkShowLayer_Label.setSelection(mapApp.getLayer_Label().isEnabled());
		_chkShowLayer_TileInfo.setSelection(mapApp.getLayer_TileInfo().isEnabled());
		_chkShowLayer_Scale.setSelection(mapApp.getLayer_ScaleBar().isEnabled());
	}

}
