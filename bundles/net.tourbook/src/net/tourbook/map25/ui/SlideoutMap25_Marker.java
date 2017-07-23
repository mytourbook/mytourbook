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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25MarkerConfig;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutMap25_Marker extends ToolbarSlideout implements IColorSelectorListener {

	private SelectionListener		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private FocusListener			_keepOpenListener;

	private PixelConverter			_pc;

	private Map25View				_map25View;

	private boolean					_isUpdateUI;

	private Font					_boldFont;

	{
		_boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
	}

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private ColorSelectorExtended	_colorCluster_Background;
	private ColorSelectorExtended	_colorCluster_Foreground;

	private Combo					_comboConfigName;

	private Label					_lblConfigName;

	private Text					_textConfigName;

	public SlideoutMap25_Marker(final Control ownerControl,
								final ToolBar toolBar,
								final IDialogSettings state,
								final Map25View map25View) {

		super(ownerControl, toolBar);

		_map25View = map25View;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		setIsAnotherDialogOpened(isDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI();
		createActions();

		final Composite ui = createUI(parent);

		fillUI();
		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(UI.SHELL_MARGIN, UI.SHELL_MARGIN).applyTo(_shellContainer);
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
				createUI_20_Properties(container);
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
		title.setText(Messages.Slideout_Map25MarkerOptions_Label_Title);
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
			 * Combo: Configuration
			 */
			_comboConfigName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.BEGINNING, SWT.CENTER)
					// this is too small in linux
					//					.hint(_pc.convertHorizontalDLUsToPixels(15 * 4), SWT.DEFAULT)
					.applyTo(_comboConfigName);
			_comboConfigName.setVisibleItemCount(20);
			_comboConfigName.addFocusListener(_keepOpenListener);
			_comboConfigName.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectConfig();
				}
			});
		}
	}

	private void createUI_20_Properties(final Composite parent) {

		{
			/*
			 * Cluster color
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterColor);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				// foreground color
				_colorCluster_Foreground = new ColorSelectorExtended(container);
				_colorCluster_Foreground.addListener(_defaultPropertyChangeListener);
				_colorCluster_Foreground.addOpenListener(this);

				// foreground color
				_colorCluster_Background = new ColorSelectorExtended(container);
				_colorCluster_Background.addListener(_defaultPropertyChangeListener);
				_colorCluster_Background.addOpenListener(this);
			}
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
			_lblConfigName.setText("&Name");
			_lblConfigName.setToolTipText("Name for the currently selected tour marker configuration");
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblConfigName);

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

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			for (final Map25MarkerConfig config : Map25ConfigManager.getAllMarkerConfigs()) {
				_comboConfigName.add(config.name);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
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
				UI.adjustSpinnerValueOnMouseScroll(event);
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

		enableControls();

		_map25View.getMapApp().onModifyMarkerConfig();
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboConfigName.getSelectionIndex();

		_comboConfigName.setItem(selectedIndex, _textConfigName.getText());

		saveState();
	}

	private void onSelectConfig() {

		final int selectedIndex = _comboConfigName.getSelectionIndex();
		final ArrayList<Map25MarkerConfig> allConfigurations = Map25ConfigManager.getAllMarkerConfigs();

		final Map25MarkerConfig selectedConfig = allConfigurations.get(selectedIndex);
		final Map25MarkerConfig activeConfig = Map25ConfigManager.getActiveMarkerConfig();

		if (selectedConfig.equals(activeConfig)) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState();

		Map25ConfigManager.setActiveMarkerConfig(selectedConfig);

		restoreState();

		enableControls();

		_map25View.getMapApp().onModifyMarkerConfig();
	}

	/**
	 * Restores state values from the tour marker configuration and update the UI.
	 */
	public void restoreState() {

		_isUpdateUI = true;
		{
			final Map25MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

			// get active config AFTER getting the index because this could change the active config
			final int activeConfigIndex = Map25ConfigManager.getActiveMarkerConfigIndex();

			_comboConfigName.select(activeConfigIndex);
			_textConfigName.setText(config.name);

			_colorCluster_Foreground.setColorValue(config.clusterColorForeground);
			_colorCluster_Background.setColorValue(config.clusterColorBackground);
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		// update config

		final Map25MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		config.name = _textConfigName.getText();

		config.clusterColorForeground = _colorCluster_Foreground.getColorValue();
		config.clusterColorBackground = _colorCluster_Background.getColorValue();
	}

}
