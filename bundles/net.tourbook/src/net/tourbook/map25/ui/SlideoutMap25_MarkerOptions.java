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
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEntry;
import net.tourbook.map25.ClusterAlgorithmItem;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.marker.ClusterAlgorithm;
import net.tourbook.map25.layer.marker.MarkerConfig;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutMap25_MarkerOptions extends ToolbarSlideout implements IColorSelectorListener {

// SET_FORMATTING_OFF
	
	private static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP	= net.tourbook.map3.Messages.TourTrack_Properties_Button_Default_Tooltip;
	private static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT			= net.tourbook.map3.Messages.TourTrack_Properties_Button_Default;

// SET_FORMATTING_ON

	private static final int		DEFAULT_COMBO_WIDTH								= 30;

	private SelectionListener		_defaultSelectionListener;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private FocusListener			_keepOpenListener;

	private PixelConverter			_pc;

	private Map25View				_map25View;

	private boolean					_isUpdateUI;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private ColorSelectorExtended	_colorClusterSymbol_Outline;
	private ColorSelectorExtended	_colorClusterSymbol_Fill;
	private ColorSelectorExtended	_colorMarkerSymbol_Outline;
	private ColorSelectorExtended	_colorMarkerSymbol_Fill;

	private Button					_btnSwapClusterSymbolColor;
	private Button					_btnSwapMarkerColor;
	private Button					_btnReset;

	private Button					_chkIsMarkerClustering;
//	private Button					_chkIsShowMarkerLabel;
	private Button					_chkIsShowMarkerPoint;

	private Combo					_comboClusterAlgorithm;
	private Combo					_comboClusterOrientation;
	private Combo					_comboConfigName;
	private Combo					_comboMarkerOrientation;

	private Label					_lblConfigName;
	private Label					_lblClusterGridSize;
	private Label					_lblClusterOpacity;
	private Label					_lblClusterAlgorithm;
	private Label					_lblClusterOrientation;
	private Label					_lblClusterSymbol;
	private Label					_lblClusterSymbolSize;
	private Label					_lblMarkerOpacity;
	private Label					_lblMarkerOrientation;
	private Label					_lblMarkerColor;
	private Label					_lblMarkerSize;

	private Spinner					_spinnerClusterGrid_Size;
	private Spinner					_spinnerClusterFill_Opacity;
	private Spinner					_spinnerClusterOutline_Opacity;
	private Spinner					_spinnerClusterOutline_Size;
	private Spinner					_spinnerClusterSymbol_Size;
	private Spinner					_spinnerClusterSymbol_Weight;

	private Spinner					_spinnerMarkerFill_Opacity;
	private Spinner					_spinnerMarkerOutline_Opacity;
	private Spinner					_spinnerMarkerOutline_Size;
	private Spinner					_spinnerMarkerSymbol_Size;

	private Text					_textConfigName;

	public SlideoutMap25_MarkerOptions(	final Control ownerControl,
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
		fillUI_Config();
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
//				createUI_10_MarkerLabel(container);
				createUI_20_MarkerPoint(container);
				createUI_30_Cluster(container);
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
		title.setText(Messages.Slideout_Map25MarkerOptions_Label_Title);
		MTFont.setBannerFont(title);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(title);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Combo: Configuration
				 */
				_comboConfigName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				_comboConfigName.setVisibleItemCount(20);
				_comboConfigName.addFocusListener(_keepOpenListener);
				_comboConfigName.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectConfig();
					}
				});
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
						.applyTo(_comboConfigName);
			}
			{
				/*
				 * Button: Reset
				 */
				_btnReset = new Button(container, SWT.PUSH);
				_btnReset.setText(TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT);
				_btnReset.setToolTipText(TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP);
				_btnReset.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectConfig_Default(e);
					}
				});
				GridDataFactory
						.fillDefaults()//
						.align(SWT.END, SWT.CENTER)
						.applyTo(_btnReset);
			}
		}
	}

//	private void createUI_10_MarkerLabel(final Composite parent) {
//
//		{
//			// checkbox: Show label
//			_chkIsShowMarkerLabel = new Button(parent, SWT.CHECK);
//			_chkIsShowMarkerLabel.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowMarkerLabel);
//			_chkIsShowMarkerLabel.addSelectionListener(_defaultSelectionListener);
//			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowMarkerLabel);
//		}
//	}

	private void createUI_20_MarkerPoint(final Composite parent) {

		{
			// checkbox: Show point
			_chkIsShowMarkerPoint = new Button(parent, SWT.CHECK);
			_chkIsShowMarkerPoint.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowMarkerPoint);
			_chkIsShowMarkerPoint.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowMarkerPoint);
		}
		{
			// label: symbol
			_lblMarkerColor = new Label(parent, SWT.NONE);
			_lblMarkerColor.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor);
			_lblMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor_Tooltip);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblMarkerColor);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				// outline color
				_colorMarkerSymbol_Outline = new ColorSelectorExtended(container);
				_colorMarkerSymbol_Outline.addListener(_defaultPropertyChangeListener);
				_colorMarkerSymbol_Outline.addOpenListener(this);

				// fill color
				_colorMarkerSymbol_Fill = new ColorSelectorExtended(container);
				_colorMarkerSymbol_Fill.addListener(_defaultPropertyChangeListener);
				_colorMarkerSymbol_Fill.addOpenListener(this);

				// button: swap color
				_btnSwapMarkerColor = new Button(container, SWT.PUSH);
				_btnSwapMarkerColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
				_btnSwapMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
				_btnSwapMarkerColor.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSwapMarkerColor(e);
					}
				});
			}
		}
		{
			/*
			 * Opacity
			 */
			// label
			_lblMarkerOpacity = new Label(parent, SWT.NONE);
			_lblMarkerOpacity.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity);
			_lblMarkerOpacity.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity_Tooltip);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblMarkerOpacity);

			/*
			 * Symbol
			 */
			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				{
					// spinner: outline
					_spinnerMarkerOutline_Opacity = new Spinner(container, SWT.BORDER);
					_spinnerMarkerOutline_Opacity.setMinimum(0);
					_spinnerMarkerOutline_Opacity.setMaximum(100);
					_spinnerMarkerOutline_Opacity.setIncrement(1);
					_spinnerMarkerOutline_Opacity.setPageIncrement(10);
					_spinnerMarkerOutline_Opacity.addSelectionListener(_defaultSelectionListener);
					_spinnerMarkerOutline_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
				{
					// spinner: fill
					_spinnerMarkerFill_Opacity = new Spinner(container, SWT.BORDER);
					_spinnerMarkerFill_Opacity.setMinimum(0);
					_spinnerMarkerFill_Opacity.setMaximum(100);
					_spinnerMarkerFill_Opacity.setIncrement(1);
					_spinnerMarkerFill_Opacity.setPageIncrement(10);
					_spinnerMarkerFill_Opacity.addSelectionListener(_defaultSelectionListener);
					_spinnerMarkerFill_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
		{
			/*
			 * Size
			 */

			// label: size
			_lblMarkerSize = new Label(parent, SWT.NONE);
			_lblMarkerSize.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize);
			_lblMarkerSize.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize_Tooltip);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblMarkerSize);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				// outline size
				_spinnerMarkerOutline_Size = new Spinner(container, SWT.BORDER);
				_spinnerMarkerOutline_Size.setMinimum((int) Map25ConfigManager.MARKER_SYMBOL_SIZE_OUTLINE_MIN * 10);
				_spinnerMarkerOutline_Size.setMaximum((int) Map25ConfigManager.MARKER_SYMBOL_SIZE_OUTLINE_MAX * 10);
				_spinnerMarkerOutline_Size.setDigits(1);
				_spinnerMarkerOutline_Size.setIncrement(1);
				_spinnerMarkerOutline_Size.setPageIncrement(10);
				_spinnerMarkerOutline_Size.addSelectionListener(_defaultSelectionListener);
				_spinnerMarkerOutline_Size.addMouseWheelListener(_defaultMouseWheelListener);

				// symbol size
				_spinnerMarkerSymbol_Size = new Spinner(container, SWT.BORDER);
				_spinnerMarkerSymbol_Size.setMinimum(Map25ConfigManager.MARKER_SYMBOL_SIZE_MIN);
				_spinnerMarkerSymbol_Size.setMaximum(Map25ConfigManager.MARKER_SYMBOL_SIZE_MAX);
				_spinnerMarkerSymbol_Size.setIncrement(1);
				_spinnerMarkerSymbol_Size.setPageIncrement(10);
				_spinnerMarkerSymbol_Size.addSelectionListener(_defaultSelectionListener);
				_spinnerMarkerSymbol_Size.addMouseWheelListener(_defaultMouseWheelListener);

			}
		}
		{
			/*
			 * Orientation: billboard/ground
			 */
			{
				// label
				_lblMarkerOrientation = new Label(parent, SWT.NONE);
				_lblMarkerOrientation.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOrientation);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblMarkerOrientation);
			}
			{
				// combo
				_comboMarkerOrientation = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
				_comboMarkerOrientation.setVisibleItemCount(20);
				_comboMarkerOrientation.addFocusListener(_keepOpenListener);
				_comboMarkerOrientation.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyConfig();
					}
				});
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
						.applyTo(_comboMarkerOrientation);
			}
		}
	}

	private void createUI_30_Cluster(final Composite parent) {

		{
			/*
			 * Cluster
			 */

			// checkbox: Is clustering
			_chkIsMarkerClustering = new Button(parent, SWT.CHECK);
			_chkIsMarkerClustering.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsMarkerClustering);
			_chkIsMarkerClustering.addSelectionListener(_defaultSelectionListener);
			GridDataFactory
					.fillDefaults()
					.span(2, 1)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_chkIsMarkerClustering);
		}
		{
			/*
			 * Symbol color
			 */
			{
				// label
				_lblClusterSymbol = new Label(parent, SWT.NONE);
				_lblClusterSymbol.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor);
				_lblClusterSymbol.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblClusterSymbol);
			}

			{
				final Composite container = new Composite(parent, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
				GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

				// foreground color
				_colorClusterSymbol_Outline = new ColorSelectorExtended(container);
				_colorClusterSymbol_Outline.addListener(_defaultPropertyChangeListener);
				_colorClusterSymbol_Outline.addOpenListener(this);

				// foreground color
				_colorClusterSymbol_Fill = new ColorSelectorExtended(container);
				_colorClusterSymbol_Fill.addListener(_defaultPropertyChangeListener);
				_colorClusterSymbol_Fill.addOpenListener(this);

				// button: swap color
				_btnSwapClusterSymbolColor = new Button(container, SWT.PUSH);
				_btnSwapClusterSymbolColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
				_btnSwapClusterSymbolColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
				_btnSwapClusterSymbolColor.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSwapClusterColor(e);
					}
				});
			}
		}
		{
			/*
			 * Opacity
			 */
			// label
			_lblClusterOpacity = new Label(parent, SWT.NONE);
			_lblClusterOpacity.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOpacity);
			_lblClusterOpacity.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOpacity_Tooltip);
			GridDataFactory
					.fillDefaults()
					.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblClusterOpacity);

			/*
			 * Symbol
			 */
			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				{
					// spinner: outline
					_spinnerClusterOutline_Opacity = new Spinner(container, SWT.BORDER);
					_spinnerClusterOutline_Opacity.setMinimum(0);
					_spinnerClusterOutline_Opacity.setMaximum(100);
					_spinnerClusterOutline_Opacity.setIncrement(1);
					_spinnerClusterOutline_Opacity.setPageIncrement(10);
					_spinnerClusterOutline_Opacity.addSelectionListener(_defaultSelectionListener);
					_spinnerClusterOutline_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
				{
					// spinner: fill
					_spinnerClusterFill_Opacity = new Spinner(container, SWT.BORDER);
					_spinnerClusterFill_Opacity.setMinimum(0);
					_spinnerClusterFill_Opacity.setMaximum(100);
					_spinnerClusterFill_Opacity.setIncrement(1);
					_spinnerClusterFill_Opacity.setPageIncrement(10);
					_spinnerClusterFill_Opacity.addSelectionListener(_defaultSelectionListener);
					_spinnerClusterFill_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
		{
			/*
			 * Size
			 */
			{
				// label
				_lblClusterSymbolSize = new Label(parent, SWT.NONE);
				_lblClusterSymbolSize.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSize);
				_lblClusterSymbolSize.setToolTipText(
						Messages.Slideout_Map25MarkerOptions_Label_ClusterSize_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblClusterSymbolSize);
			}

			{
				final Composite container = new Composite(parent, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
				GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
				{

					// outline size
					_spinnerClusterOutline_Size = new Spinner(container, SWT.BORDER);
					_spinnerClusterOutline_Size.setMinimum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_OUTLINE_MIN * 10);
					_spinnerClusterOutline_Size.setMaximum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_OUTLINE_MAX * 10);
					_spinnerClusterOutline_Size.setDigits(1);
					_spinnerClusterOutline_Size.setIncrement(1);
					_spinnerClusterOutline_Size.setPageIncrement(10);
					_spinnerClusterOutline_Size.addSelectionListener(_defaultSelectionListener);
					_spinnerClusterOutline_Size.addMouseWheelListener(_defaultMouseWheelListener);

					// spinner: symbol size
					_spinnerClusterSymbol_Size = new Spinner(container, SWT.BORDER);
					_spinnerClusterSymbol_Size.setMinimum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_MIN);
					_spinnerClusterSymbol_Size.setMaximum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_MAX);
					_spinnerClusterSymbol_Size.setIncrement(1);
					_spinnerClusterSymbol_Size.setPageIncrement(10);
					_spinnerClusterSymbol_Size.addSelectionListener(_defaultSelectionListener);
					_spinnerClusterSymbol_Size.addMouseWheelListener(_defaultMouseWheelListener);

					// spinner: symbol size weight
					_spinnerClusterSymbol_Weight = new Spinner(container, SWT.BORDER);
					_spinnerClusterSymbol_Weight.setMinimum(Map25ConfigManager.CLUSTER_SYMBOL_WEIGHT_MIN);
					_spinnerClusterSymbol_Weight.setMaximum(Map25ConfigManager.CLUSTER_SYMBOL_WEIGHT_MAX);
					_spinnerClusterSymbol_Weight.setIncrement(1);
					_spinnerClusterSymbol_Weight.setPageIncrement(10);
					_spinnerClusterSymbol_Weight.addSelectionListener(_defaultSelectionListener);
					_spinnerClusterSymbol_Weight.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
		{
			/*
			 * Cluster orientation: billboard/ground
			 */
			{
				// label
				_lblClusterOrientation = new Label(parent, SWT.NONE);
				_lblClusterOrientation.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOrientation);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblClusterOrientation);
			}
			{
				/*
				 * Combo: Orientaion
				 */
				_comboClusterOrientation = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
				_comboClusterOrientation.setVisibleItemCount(20);
				_comboClusterOrientation.addFocusListener(_keepOpenListener);
				_comboClusterOrientation.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyConfig();
					}
				});
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
						.applyTo(_comboClusterOrientation);
			}
		}
		{
			/*
			 * Cluster placement: first marker/distance/grid
			 */
			{
				// label
				_lblClusterAlgorithm = new Label(parent, SWT.NONE);
				_lblClusterAlgorithm.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterPlacement);
				_lblClusterAlgorithm.setToolTipText(
						Messages.Slideout_Map25MarkerOptions_Label_ClusterPlacement_Tooltip);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblClusterAlgorithm);
			}
			{
				/*
				 * Combo: Placement
				 */
				_comboClusterAlgorithm = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
				_comboClusterAlgorithm.setVisibleItemCount(20);
				_comboClusterAlgorithm.addFocusListener(_keepOpenListener);
				_comboClusterAlgorithm.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onModifyConfig();
					}
				});
				GridDataFactory
						.fillDefaults()
						.grab(true, false)
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
						.applyTo(_comboClusterAlgorithm);
			}
		}
		{
			/*
			 * Cluster size
			 */
			{
				// label
				_lblClusterGridSize = new Label(parent, SWT.NONE);
				_lblClusterGridSize.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterGridSize);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblClusterGridSize);

				// spinner
				_spinnerClusterGrid_Size = new Spinner(parent, SWT.BORDER);
				_spinnerClusterGrid_Size.setMinimum(Map25ConfigManager.CLUSTER_GRID_MIN_SIZE);
				_spinnerClusterGrid_Size.setMaximum(Map25ConfigManager.CLUSTER_GRID_MAX_SIZE);
				_spinnerClusterGrid_Size.setIncrement(1);
				_spinnerClusterGrid_Size.setPageIncrement(10);
				_spinnerClusterGrid_Size.addSelectionListener(_defaultSelectionListener);
				_spinnerClusterGrid_Size.addMouseWheelListener(_defaultMouseWheelListener);
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
			_lblConfigName.setText(Messages.Slideout_Map25MarkerOptions_Label_Name);
			_lblConfigName.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_Name_Tooltip);
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

		final boolean isMarkerPoint = _chkIsShowMarkerPoint.getSelection();
		final boolean isClustering = _chkIsMarkerClustering.getSelection() && isMarkerPoint;

		_btnSwapClusterSymbolColor.setEnabled(isClustering);
		_btnSwapMarkerColor.setEnabled(isMarkerPoint);

		_chkIsMarkerClustering.setEnabled(isMarkerPoint);

		_colorClusterSymbol_Outline.setEnabled(isClustering);
		_colorClusterSymbol_Fill.setEnabled(isClustering);
		_colorMarkerSymbol_Fill.setEnabled(isMarkerPoint);
		_colorMarkerSymbol_Outline.setEnabled(isMarkerPoint);

		_comboClusterAlgorithm.setEnabled(isClustering);
		_comboClusterOrientation.setEnabled(isClustering);
		_comboMarkerOrientation.setEnabled(isMarkerPoint);

		_lblClusterGridSize.setEnabled(isClustering);
		_lblClusterAlgorithm.setEnabled(isClustering);
		_lblClusterOrientation.setEnabled(isClustering);
		_lblClusterOpacity.setEnabled(isClustering);
		_lblClusterSymbol.setEnabled(isClustering);
		_lblClusterSymbolSize.setEnabled(isClustering);
		_lblMarkerOpacity.setEnabled(isMarkerPoint);
		_lblMarkerOrientation.setEnabled(isMarkerPoint);
		_lblMarkerColor.setEnabled(isMarkerPoint);
		_lblMarkerSize.setEnabled(isMarkerPoint);

		_spinnerClusterGrid_Size.setEnabled(isClustering);
		_spinnerClusterFill_Opacity.setEnabled(isClustering);
		_spinnerClusterOutline_Opacity.setEnabled(isClustering);
		_spinnerClusterOutline_Size.setEnabled(isMarkerPoint);
		_spinnerClusterSymbol_Size.setEnabled(isClustering);
		_spinnerClusterSymbol_Weight.setEnabled(isClustering);

		_spinnerMarkerFill_Opacity.setEnabled(isMarkerPoint);
		_spinnerMarkerOutline_Opacity.setEnabled(isMarkerPoint);
		_spinnerMarkerSymbol_Size.setEnabled(isMarkerPoint);
		_spinnerMarkerOutline_Size.setEnabled(isMarkerPoint);
	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			for (final ComboEntry comboItem : Map25ConfigManager.SYMBOL_ORIENTATION) {

				_comboClusterOrientation.add(comboItem.label);
				_comboMarkerOrientation.add(comboItem.label);
			}

			for (final ClusterAlgorithmItem item : Map25ConfigManager.ALL_CLUSTER_ALGORITHM) {
				_comboClusterAlgorithm.add(item.label);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void fillUI_Config() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{
			_comboConfigName.removeAll();

			for (final MarkerConfig config : Map25ConfigManager.getAllMarkerConfigs()) {
				_comboConfigName.add(config.name);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private int getClusterAlgorithmIndex(final Enum<ClusterAlgorithm> requestedAlgorithm) {

		final ClusterAlgorithmItem[] allClusterAlgorithm = Map25ConfigManager.ALL_CLUSTER_ALGORITHM;

		for (int algorithmIndex = 0; algorithmIndex < allClusterAlgorithm.length; algorithmIndex++) {

			final ClusterAlgorithm algorithm = allClusterAlgorithm[algorithmIndex].clusterAlgorithm;

			if (algorithm == requestedAlgorithm) {
				return algorithmIndex;
			}
		}

		// this should not occure
		return 0;
	}

	private int getOrientationIndex(final int orientationId) {

		final ComboEntry[] symbolOrientation = Map25ConfigManager.SYMBOL_ORIENTATION;

		for (int itemIndex = 0; itemIndex < symbolOrientation.length; itemIndex++) {

			final ComboEntry comboItem = symbolOrientation[itemIndex];

			if (comboItem.value == orientationId) {
				return itemIndex;
			}
		}

		return 0;
	}

	private Enum<ClusterAlgorithm> getSelectedClusterAlgorithm() {

		final int selectedIndex = Math.max(0, _comboClusterAlgorithm.getSelectionIndex());

		return Map25ConfigManager.ALL_CLUSTER_ALGORITHM[selectedIndex].clusterAlgorithm;
	}

	private int getSelectedOrientation(final Combo combo) {

		final int selectedIndex = Math.max(0, combo.getSelectionIndex());

		return Map25ConfigManager.SYMBOL_ORIENTATION[selectedIndex].value;
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
		final ArrayList<MarkerConfig> allConfigurations = Map25ConfigManager.getAllMarkerConfigs();

		final MarkerConfig selectedConfig = allConfigurations.get(selectedIndex);
		final MarkerConfig activeConfig = Map25ConfigManager.getActiveMarkerConfig();

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

	private void onSelectConfig_Default(final SelectionEvent selectionEvent) {

		if (Util.isCtrlKeyPressed(selectionEvent)) {

			// reset All configurations

			Map25ConfigManager.resetAllMarkerConfigurations();

			fillUI_Config();

		} else {

			// reset active config

			Map25ConfigManager.resetActiveMarkerConfiguration();
		}

		restoreState();
		enableControls();

		_map25View.getMapApp().onModifyMarkerConfig();
	}

	private void onSwapClusterColor(final SelectionEvent e) {

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		final RGB fgColor = config.clusterOutline_Color;
		final RGB bgColor = config.clusterFill_Color;

		config.clusterOutline_Color = bgColor;
		config.clusterFill_Color = fgColor;

		restoreState();
		onModifyConfig();
	}

	private void onSwapMarkerColor(final SelectionEvent event) {

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		final RGB fgColor = config.markerOutline_Color;
		final RGB bgColor = config.markerFill_Color;

		config.markerOutline_Color = bgColor;
		config.markerFill_Color = fgColor;

		restoreState();
		onModifyConfig();
	}

	/**
	 * Restores state values from the tour marker configuration and update the UI.
	 */
	public void restoreState() {

		_isUpdateUI = true;
		{
			final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

			// get active config AFTER getting the index because this could change the active config
			final int activeConfigIndex = Map25ConfigManager.getActiveMarkerConfigIndex();

			_comboConfigName.select(activeConfigIndex);
			_textConfigName.setText(config.name);

			/*
			 * Marker
			 */
//			_chkIsShowMarkerLabel.setSelection(config.isShowMarkerLabel);
			_chkIsShowMarkerPoint.setSelection(config.isShowMarkerPoint);
			_comboMarkerOrientation.select(getOrientationIndex(config.markerOrientation));

			_colorClusterSymbol_Outline.setColorValue(config.clusterOutline_Color);
			_colorClusterSymbol_Fill.setColorValue(config.clusterFill_Color);

			_spinnerMarkerSymbol_Size.setSelection(config.markerSymbolSize);
			_spinnerMarkerOutline_Size.setSelection((int) (config.markerSymbolSize_Outline * 10));
			_spinnerMarkerFill_Opacity.setSelection(config.markerFill_Opacity);
			_spinnerMarkerOutline_Opacity.setSelection(config.markerOutline_Opacity);

			/*
			 * Cluster
			 */
			_chkIsMarkerClustering.setSelection(config.isMarkerClustered);
			_comboClusterAlgorithm.select(getClusterAlgorithmIndex(config.clusterAlgorithm));
			_comboClusterOrientation.select(getOrientationIndex(config.clusterOrientation));

			_colorMarkerSymbol_Outline.setColorValue(config.markerOutline_Color);
			_colorMarkerSymbol_Fill.setColorValue(config.markerFill_Color);

			_spinnerClusterFill_Opacity.setSelection(config.clusterFill_Opacity);
			_spinnerClusterOutline_Opacity.setSelection(config.clusterOutline_Opacity);
			_spinnerClusterOutline_Size.setSelection((int) (config.clusterSymbolSize_Outline * 10));
			_spinnerClusterGrid_Size.setSelection(config.clusterGridSize);
			_spinnerClusterSymbol_Size.setSelection(config.clusterSymbolSize);
			_spinnerClusterSymbol_Weight.setSelection(config.clusterSymbolWeight);
		}
		_isUpdateUI = false;
	}

	private void saveState() {

		// update config

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		config.name = _textConfigName.getText();

		/*
		 * Marker
		 */
//		config.isShowMarkerLabel = _chkIsShowMarkerLabel.getSelection();
		config.isShowMarkerPoint = _chkIsShowMarkerPoint.getSelection();
		config.markerOrientation = getSelectedOrientation(_comboMarkerOrientation);
		config.markerSymbolSize = _spinnerMarkerSymbol_Size.getSelection();
		config.markerSymbolSize_Outline = _spinnerMarkerOutline_Size.getSelection() / 10.0f;
		config.markerOutline_Color = _colorMarkerSymbol_Outline.getColorValue();
		config.markerOutline_Opacity = _spinnerMarkerOutline_Opacity.getSelection();
		config.markerFill_Color = _colorMarkerSymbol_Fill.getColorValue();
		config.markerFill_Opacity = _spinnerMarkerFill_Opacity.getSelection();

		/*
		 * Cluster
		 */
		config.isMarkerClustered = _chkIsMarkerClustering.getSelection();
		config.clusterAlgorithm = getSelectedClusterAlgorithm();
		config.clusterOrientation = getSelectedOrientation(_comboClusterOrientation);

		config.clusterGridSize = _spinnerClusterGrid_Size.getSelection();
		config.clusterSymbolSize = _spinnerClusterSymbol_Size.getSelection();
		config.clusterSymbolWeight = _spinnerClusterSymbol_Weight.getSelection();
		config.clusterSymbolSize_Outline = _spinnerClusterOutline_Size.getSelection() / 10.0f;

		config.clusterFill_Color = _colorClusterSymbol_Fill.getColorValue();
		config.clusterFill_Opacity = _spinnerClusterFill_Opacity.getSelection();
		config.clusterOutline_Color = _colorClusterSymbol_Outline.getColorValue();
		config.clusterOutline_Opacity = _spinnerClusterOutline_Opacity.getSelection();
	}

}
