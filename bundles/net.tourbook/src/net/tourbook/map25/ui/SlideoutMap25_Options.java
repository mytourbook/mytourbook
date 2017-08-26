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

import java.util.LinkedList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map.bookmark.MapBookmark;
import net.tourbook.map.bookmark.MapBookmarkManager;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.oscim.utils.Easing;
import org.oscim.utils.Easing.Type;

/**
 * Map 2.5D properties slideout.
 */
public class SlideoutMap25_Options extends ToolbarSlideout {


	private SelectionAdapter	_defaultSelectionListener;
	private MouseWheelListener	_defaultMouseWheelListener;
	private FocusListener		_keepOpenListener;
	private SelectionAdapter	_layerSelectionListener;

	private PixelConverter		_pc;

	private Font				_boldFont;
	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	private Map25View		_map25View;

	/*
	 * UI controls
	 */
	private Button			_chkShowLayer_Building;
	private Button			_chkShowLayer_TileInfo;
	private Button			_chkShowLayer_BaseMap;
	private Button			_chkShowLayer_Scale;
	private Button			_chkShowLayer_Label;

	private Button			_chkIsAnimateLocation;

	private Label			_lblAnimationTime;
	private Label			_lblAnimationEasingType;
	private Label			_lblSeconds;

	private Spinner			_spinnerAnimationTime;

	private Combo			_comboAnimationEasingType;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map25View
	 */
	public SlideoutMap25_Options(	final Control ownerControl,
									final ToolBar toolBar,
									final Map25View map25View) {

		super(ownerControl, toolBar);

		_map25View = map25View;
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableActions();

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

				createUI_40_Options_Animation(container);
				createUI_50_Layer(container);
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
		label.setText(Messages.Slideout_Map25MapOptions_Label_MapOptions);
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

	private void createUI_40_Options_Animation(final Composite parent) {

		final SelectionAdapter animationListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
				onSelectAnimation();
			}
		};

		{
			/*
			 * Is animate location
			 */
			_chkIsAnimateLocation = new Button(parent, SWT.CHECK);
			_chkIsAnimateLocation.setText(Messages.Slideout_Map25MapOptions_Checkbox_IsAnimationLocation);
			_chkIsAnimateLocation.setToolTipText(Messages.Slideout_Map25MapOptions_Checkbox_IsAnimationLocation_Tooltip);
			_chkIsAnimateLocation.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsAnimateLocation);
		}
		{
			/*
			 * Animation time
			 */

			// Label
			_lblAnimationTime = new Label(parent, SWT.NONE);
			_lblAnimationTime.setText(Messages.Slideout_Map25MapOptions_Label_AnimationTime);
			_lblAnimationTime.setToolTipText(Messages.Slideout_Map25MapOptions_Label_AnimationTime_Tooltip);
			GridDataFactory.fillDefaults().indent(_pc.convertWidthInCharsToPixels(3), 0).applyTo(_lblAnimationTime);

			final Composite timeContainer = new Composite(parent, SWT.NONE);
//				GridDataFactory.fillDefaults().grab(true, false).applyTo(timeContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(timeContainer);
			{

				// Spinner
				_spinnerAnimationTime = new Spinner(timeContainer, SWT.BORDER);
				_spinnerAnimationTime.setMinimum((int) (Map25ConfigManager.LOCATION_ANIMATION_TIME_MIN * 10));
				_spinnerAnimationTime.setMaximum((int) (Map25ConfigManager.LOCATION_ANIMATION_TIME_MAX * 10));
				_spinnerAnimationTime.setPageIncrement(10);
				_spinnerAnimationTime.setDigits(1);
				_spinnerAnimationTime.addSelectionListener(animationListener);
				_spinnerAnimationTime.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeUI();
						onSelectAnimation();
					}
				});

				// Label
				_lblSeconds = new Label(timeContainer, SWT.NONE);
				_lblSeconds.setText(Messages.App_Unit_Seconds_Small);
			}
		}
		{
			/*
			 * Easing type
			 */

			// Label
			_lblAnimationEasingType = new Label(parent, SWT.NONE);
			_lblAnimationEasingType.setText(Messages.Slideout_Map25MapOptions_Label_AnimationEasingType);
			_lblAnimationEasingType.setToolTipText(Messages.Slideout_Map25MapOptions_Label_AnimationEasingType_Tooltip);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(_pc.convertWidthInCharsToPixels(3), 0)
					.applyTo(_lblAnimationEasingType);

			// combo
			_comboAnimationEasingType = new Combo(parent, SWT.READ_ONLY);
			_comboAnimationEasingType.addFocusListener(_keepOpenListener);
			_comboAnimationEasingType.addSelectionListener(animationListener);

			// fill combo
			for (final Type easingType : Easing.Type.values()) {
				_comboAnimationEasingType.add(easingType.name());
			}
		}
	}

	private void createUI_50_Layer(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Slideout_Map25MapOptions_Group_MapLayer);
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
				_chkShowLayer_Label.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_LabelSymbol);
				_chkShowLayer_Label.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Building
				 */
				_chkShowLayer_Building = new Button(group, SWT.CHECK);
				_chkShowLayer_Building.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_3DBuilding);
				_chkShowLayer_Building.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Scale
				 */
				_chkShowLayer_Scale = new Button(group, SWT.CHECK);
				_chkShowLayer_Scale.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_ScaleBar);
				_chkShowLayer_Scale.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Map
				 */
				_chkShowLayer_BaseMap = new Button(group, SWT.CHECK);
				_chkShowLayer_BaseMap.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography);
				_chkShowLayer_BaseMap.setToolTipText(
						Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography_Tooltip);
				_chkShowLayer_BaseMap.addSelectionListener(_layerSelectionListener);
			}
			{
				/*
				 * Tile info
				 */
				_chkShowLayer_TileInfo = new Button(group, SWT.CHECK);
				_chkShowLayer_TileInfo.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_TileInfo);
				_chkShowLayer_TileInfo.addSelectionListener(_layerSelectionListener);
			}
		}
	}

	private Type easingType_GetSelected() {

		final int selectedIndex = _comboAnimationEasingType.getSelectionIndex();

		final Type[] easingTypes = Easing.Type.values();

		return easingTypes[selectedIndex];
	}

	private void easingType_SetSelected(final Type selectEasingType) {

		final Type[] easingTypes = Easing.Type.values();

		int selectIndex = 0;

		for (int typeIndex = 0; typeIndex < easingTypes.length; typeIndex++) {

			if (selectEasingType == easingTypes[typeIndex]) {

				selectIndex = typeIndex;

				break;
			}
		}

		_comboAnimationEasingType.select(selectIndex);
	}

	private void enableActions() {

		final boolean isAnimation = _chkIsAnimateLocation.getSelection();

		_comboAnimationEasingType.setEnabled(isAnimation);
		_lblAnimationEasingType.setEnabled(isAnimation);
		_lblAnimationTime.setEnabled(isAnimation);
		_lblSeconds.setEnabled(isAnimation);
		_spinnerAnimationTime.setEnabled(isAnimation);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

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
		_layerSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyLayer();
			}
		};
	}

	private void onChangeUI() {

		saveState();

		enableActions();
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

	private void onSelectAnimation() {

		final LinkedList<MapBookmark> recentBookmarks = MapBookmarkManager.getAllRecentBookmarks();

		if (recentBookmarks.size() < 2) {
			return;
		}

		final MapBookmark prevBookmark = recentBookmarks.get(1);

		_map25View.moveToMapLocation(prevBookmark);
	}

	private void restoreState() {

		final Map25App mapApp = _map25View.getMapApp();

		_chkShowLayer_BaseMap.setSelection(mapApp.getLayer_BaseMap().isEnabled());
		_chkShowLayer_Building.setSelection(mapApp.getLayer_Building().isEnabled());
		_chkShowLayer_Label.setSelection(mapApp.getLayer_Label().isEnabled());
		_chkShowLayer_TileInfo.setSelection(mapApp.getLayer_TileInfo().isEnabled());
		_chkShowLayer_Scale.setSelection(mapApp.getLayer_ScaleBar().isEnabled());

		_chkIsAnimateLocation.setSelection(Map25ConfigManager.isAnimateLocation);
		_spinnerAnimationTime.setSelection((int) (Map25ConfigManager.animationTime * 10));

		easingType_SetSelected(Map25ConfigManager.animationEasingType);
	}

	private void saveState() {

		Map25ConfigManager.isAnimateLocation = _chkIsAnimateLocation.getSelection();
		Map25ConfigManager.animationTime = (float) _spinnerAnimationTime.getSelection() / 10;

		Map25ConfigManager.animationEasingType = easingType_GetSelected();
	}

}
