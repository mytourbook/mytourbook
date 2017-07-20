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

import java.util.ArrayList;

import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2.5D tour track configuration.
 */
public class SlideoutMap25_TourTrackConfig extends ToolbarSlideout implements IColorSelectorListener {

	private static final int		SHELL_MARGIN	= 5;

	private Map25View				_map25View;

	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private SelectionAdapter		_defaultSelectionListener;
	private FocusListener			_keepOpenListener;

	private boolean					_isUpdateUI;

//	private PixelConverter			_pc;
	private Font					_boldFont;
	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;
//	private Button					_btnReset;
	private Combo					_comboName;

	private Label					_lblConfigName;

	private Spinner					_spinnerAnimationTime;
	private Spinner					_spinnerOutlineWidth;

	private Text					_textConfigName;

	private ColorSelectorExtended	_colorOutlineColor;

	public SlideoutMap25_TourTrackConfig(final Composite ownerControl, final ToolBar toolbar, final Map25View map25View) {

		super(ownerControl, toolbar);

		_map25View = map25View;
	}

	@Override
	public void colorDialogOpened(final boolean isAnotherDialogOpened) {

		setIsAnotherDialogOpened(isAnotherDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();

		createActions();

		final Composite ui = createUI(parent);

		updateUI_Initial();

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

//		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_shellContainer);
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final Composite container = new Composite(_shellContainer, SWT.NO_FOCUS);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			{
				createUI_000_Title(container);
				createUI_100_Line(container);
				createUI_999_ConfigName(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_000_Title(final Composite parent) {

		/*
		 * Label: Title
		 */
		final Label title = new Label(parent, SWT.LEAD);
		title.setFont(_boldFont);
//		title.setFont(JFaceResources.getHeaderFont());
		title.setText(Messages.TourTrack_Properties_Label_ConfigName);
		title.setToolTipText(Messages.TourTrack_Properties_Label_ConfigName_Tooltip);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(title);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			/*
			 * Combo: Configutation
			 */
			_comboName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.BEGINNING, SWT.CENTER)
					// this is too small in linux
					//					.hint(_pc.convertHorizontalDLUsToPixels(15 * 4), SWT.DEFAULT)
					.applyTo(_comboName);
			_comboName.setVisibleItemCount(20);
			_comboName.addFocusListener(_keepOpenListener);
			_comboName.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectConfig();
				}
			});

//			/*
//			 * Button: Reset
//			 */
//			_btnReset = new Button(container, SWT.PUSH);
//			GridDataFactory
//					.fillDefaults()//
//					.align(SWT.END, SWT.CENTER)
//					.applyTo(_btnReset);
//			_btnReset.setText(Messages.TourTrack_Properties_Button_Default);
//			_btnReset.setToolTipText(Messages.TourTrack_Properties_Button_Default_Tooltip);
//			_btnReset.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onSelectDefaultConfig(e);
//				}
//			});
		}
	}

	private void createUI_100_Line(final Composite parent) {

		{
			/*
			 * Line width
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.TourTrack_Properties_Label_OutlineWidth);
			label.setToolTipText(Messages.TourTrack_Properties_Label_OutlineWidth_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			// spinner
			_spinnerOutlineWidth = new Spinner(parent, SWT.BORDER);
			_spinnerOutlineWidth.setDigits(1);
			_spinnerOutlineWidth.setMinimum((int) (Map25ConfigManager.OUTLINE_WIDTH_MIN * 10.0f));
			_spinnerOutlineWidth.setMaximum((int) (Map25ConfigManager.OUTLINE_WIDTH_MAX * 10.0f));
			_spinnerOutlineWidth.setIncrement(1);
			_spinnerOutlineWidth.setPageIncrement(10);
			_spinnerOutlineWidth.addSelectionListener(_defaultSelectionListener);
			_spinnerOutlineWidth.addMouseWheelListener(_defaultMouseWheelListener);
			GridDataFactory
					.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerOutlineWidth);
		}
		{
			/*
			 * Line color
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.TourTrack_Properties_Label_OutlineColor);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			// Color: Segment alternate color
			_colorOutlineColor = new ColorSelectorExtended(parent);
			_colorOutlineColor.addListener(_defaultPropertyChangeListener);
			_colorOutlineColor.addOpenListener(this);

		}
		{
			/*
			 * Animation time
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.TourTrack_Properties_Label_AnimationTime);
			label.setToolTipText(Messages.TourTrack_Properties_Label_AnimationTime_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			// spinner
			_spinnerAnimationTime = new Spinner(parent, SWT.BORDER);
			_spinnerAnimationTime.setDigits(1);
			_spinnerAnimationTime.setMinimum(0);
			_spinnerAnimationTime.setMaximum(100);
			_spinnerAnimationTime.setIncrement(1);
			_spinnerAnimationTime.setPageIncrement(10);
			_spinnerAnimationTime.addSelectionListener(_defaultSelectionListener);
			_spinnerAnimationTime.addMouseWheelListener(_defaultMouseWheelListener);
			GridDataFactory
					.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerAnimationTime);
		}
	}

	private void createUI_999_ConfigName(final Composite parent) {

		/*
		 * Name
		 */
		{
			/*
			 * Label
			 */
			_lblConfigName = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblConfigName);

			_lblConfigName.setText(Messages.TourTrack_Properties_Label_Name);

			/*
			 * Text
			 */
			_textConfigName = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_textConfigName);
			_textConfigName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onModifyName();
				}
			});
		}
	}

	private void enableControls() {

	}

	private void initUI() {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyConfig();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				onModifyConfig();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onModifyConfig();
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

	private void onModifyConfig() {

		saveState();

		updateUI();

		enableControls();

		_map25View.getMapApp().getLayer_Tour().onModifyConfig();

		// update sliders
//		updateUI_Map25();
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboName.getSelectionIndex();

		_comboName.setItem(selectedIndex, _textConfigName.getText());

		saveState();
	}

	private void onSelectConfig() {

		final int selectedIndex = _comboName.getSelectionIndex();
		final ArrayList<TourTrackConfig> allConfigurations = Map25ConfigManager.getAllConfigurations();

		final TourTrackConfig selectedConfig = allConfigurations.get(selectedIndex);
		final TourTrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		if (selectedConfig == trackConfig) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState();

		final TourTrackConfig previousConfig = (TourTrackConfig) Map25ConfigManager.getActiveTourTrackConfig().clone();

		Map25ConfigManager.setActiveConfig(selectedConfig);

		updateUI_SetActiveConfig(previousConfig);
	}

	/**
	 * Restores state values from the tour track configuration and update the UI.
	 */
	public void restoreState() {

		_isUpdateUI = true;

		final TourTrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

		// get active config AFTER getting the index because this could change the active config
		final int activeConfigIndex = Map25ConfigManager.getActiveConfigIndex();

		_comboName.select(activeConfigIndex);
		_textConfigName.setText(config.name);

		// line color
		_colorOutlineColor.setColorValue(config.outlineColor);
		_spinnerOutlineWidth.setSelection((int) (config.outlineWidth * 10));

		_spinnerAnimationTime.setSelection(config.animationTime / 100);

		updateUI();

		_isUpdateUI = false;
	}

	private void saveState() {

		// update config

		final TourTrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

		config.name = _textConfigName.getText();

		// line
		config.outlineColor = _colorOutlineColor.getColorValue();
		config.outlineWidth = _spinnerOutlineWidth.getSelection() / 10.0f;

		config.animationTime = _spinnerAnimationTime.getSelection() * 100;
	}

	private void updateUI() {

		final TourTrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

		_lblConfigName.setToolTipText(
				NLS.bind(//
						Messages.TourTrack_Properties_Label_Name_Tooltip,
						config.defaultId));
	}

	private void updateUI_ComboConfigName(final boolean isReplaceItems) {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			int backupNameIndex = 0;

			if (isReplaceItems) {
				backupNameIndex = _comboName.getSelectionIndex();
				_comboName.removeAll();
			}

			for (final TourTrackConfig config : Map25ConfigManager.getAllConfigurations()) {
				_comboName.add(config.name);
			}

			if (isReplaceItems) {

				if (backupNameIndex < 0) {
					backupNameIndex = 0;
				}
				_comboName.select(backupNameIndex);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void updateUI_Initial() {

		updateUI_ComboConfigName(false);
	}

	private void updateUI_SetActiveConfig(final TourTrackConfig previousConfig) {

		restoreState();

		enableControls();

		_map25View.getMapApp().getLayer_Tour().onModifyConfig();

//		updateUI_Map25();
	}
}
