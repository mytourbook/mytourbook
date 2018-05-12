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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.PageBook;

public class PrefPageMap2Appearance extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

// SET_FORMATTING_OFF
	
	private static final String	LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE			= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate;
	private static final String	LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE_TOOLTIP	= net.tourbook.map2.Messages.LegendColor_Dialog_Check_LiveUpdate_Tooltip;
	
	public static final String	ID												= "net.tourbook.preferences.PrefPageMap2Appearance";										//$NON-NLS-1$
	
// SET_FORMATTING_ON

	static {}

	private static final IPreferenceStore	_prefStore					= TourbookPlugin.getPrefStore();

	public static final String				PLOT_TYPE_LINE				= "line";						//$NON-NLS-1$
	public static final String				PLOT_TYPE_DOT				= "dot";						//$NON-NLS-1$
	public static final String				PLOT_TYPE_SQUARE			= "square";						//$NON-NLS-1$
	public static final String				DEFAULT_PLOT_TYPE			= PLOT_TYPE_LINE;
	//
	public static final String				TOUR_PAINT_METHOD_SIMPLE	= "simple";						//$NON-NLS-1$
	public static final String				TOUR_PAINT_METHOD_COMPLEX	= "complex";					//$NON-NLS-1$
	//
	public static final int					BORDER_TYPE_COLOR			= 0;
	public static final int					BORDER_TYPE_DARKER			= 1;
	public static final int					DEFAULT_BORDER_TYPE			= BORDER_TYPE_COLOR;
	//
	public static int						MAP_OPACITY_MINIMUM			= 20;							// %
	//
	private boolean							_isModified;
	//
	private SelectionAdapter				_defaultSelectionListener;
	private MouseWheelListener				_defaultMouseWheelListener;
	private IPropertyChangeListener			_defaultChangePropertyListener;
	//
	private PixelConverter					_pc;
	private int								_firstColumnIndent;
	//
	/*
	 * UI controls
	 */
	private Composite						_containerPage;
	private PageBook						_pageBookPaintMethod;

	private RadioGroupFieldEditor			_editorTourPaintMethod;

	private Button							_chkLiveUpdate;
	private Button							_chkPaintWithBorder;
	private Button							_chkTrackOpacity;
	private Button							_rdoBorderColorDarker;
	private Button							_rdoBorderColorColor;
	private Button							_rdoSymbolLine;
	private Button							_rdoSymbolDot;
	private Button							_rdoSymbolSquare;

	private ColorSelector					_colorBorderColor;
	private ColorSelector					_colorMapDimmColor;

	private Label							_lblBorderWidth;
	private Label							_lblBorderColor;

	private Spinner							_spinnerBorderColorDarker;
	private Spinner							_spinnerBorderWidth;
	private Spinner							_spinnerLineWidth;
	private Spinner							_spinnerTrackOpacity;

	private StyledText						_pageSimple;
	private StyledText						_pageComplex;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUI(parent);

		restoreState();
	}

	private void createUI(final Composite parent) {

		initUI(parent);

		_containerPage = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_containerPage);
		GridLayoutFactory.fillDefaults().applyTo(_containerPage);
		{
			createUI_10_TourProperties(_containerPage);
			createUI_60_PaintingMethod(_containerPage);
			createUI_80_DimmingColor(_containerPage);
			createUI_90_LiveUpdate(_containerPage);
		}
	}

	private void createUI_10_TourProperties(final Composite parent) {

		final Group groupContainer = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupContainer);
		groupContainer.setText(Messages.Pref_MapLayout_Group_TourInMapProperties);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupContainer);
		{
			/*
			 * radio: plot symbol
			 */
			{
				// label
				final Label label = new Label(groupContainer, NONE);
				label.setText(Messages.pref_map_layout_symbol);

				final Composite radioContainer = new Composite(groupContainer, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(radioContainer);
				GridLayoutFactory.fillDefaults().numColumns(3).applyTo(radioContainer);
				{
					// Radio: Line
					_rdoSymbolLine = new Button(radioContainer, SWT.RADIO);
					_rdoSymbolLine.setText(Messages.pref_map_layout_symbol_line);
					_rdoSymbolLine.addSelectionListener(_defaultSelectionListener);

					// Radio: Dot
					_rdoSymbolDot = new Button(radioContainer, SWT.RADIO);
					_rdoSymbolDot.setText(Messages.pref_map_layout_symbol_dot);
					_rdoSymbolDot.addSelectionListener(_defaultSelectionListener);

					// Radio: Squares
					_rdoSymbolSquare = new Button(radioContainer, SWT.RADIO);
					_rdoSymbolSquare.setText(Messages.pref_map_layout_symbol_square);
					_rdoSymbolSquare.addSelectionListener(_defaultSelectionListener);
				}
			}

			/*
			 * Line width
			 */
			{
				// label: line width
				final Label label = new Label(groupContainer, NONE);
				label.setText(Messages.pref_map_layout_symbol_width);

				// spinner: line width
				_spinnerLineWidth = new Spinner(groupContainer, SWT.BORDER);
				_spinnerLineWidth.setMinimum(1);
				_spinnerLineWidth.setMaximum(50);
				_spinnerLineWidth.setPageIncrement(5);

				_spinnerLineWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerLineWidth.addMouseWheelListener(_defaultMouseWheelListener);
			}
			{
				/*
				 * Tour track opacity
				 */
				{
					// checkbox
					_chkTrackOpacity = new Button(groupContainer, SWT.CHECK);
					_chkTrackOpacity.setText(Messages.Pref_Map2_Checkbox_TrackOpacity);
					_chkTrackOpacity.setToolTipText(Messages.Pref_Map2_Checkbox_TrackOpacity_Tooltip);
					_chkTrackOpacity.addSelectionListener(_defaultSelectionListener);
				}
				{
					// spinner
					_spinnerTrackOpacity = new Spinner(groupContainer, SWT.BORDER);
					_spinnerTrackOpacity.setMinimum(MAP_OPACITY_MINIMUM);
					_spinnerTrackOpacity.setMaximum(100);
					_spinnerTrackOpacity.setIncrement(1);
					_spinnerTrackOpacity.setPageIncrement(10);
					_spinnerTrackOpacity.addSelectionListener(_defaultSelectionListener);
					_spinnerTrackOpacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
			createUI_50_Border(groupContainer);
		}
	}

	private void createUI_50_Border(final Composite parent) {

		{
			/*
			 * Checkbox: paint with border
			 */
			_chkPaintWithBorder = new Button(parent, SWT.CHECK);
			GridDataFactory
					.fillDefaults()//
					.grab(true, false)
					.span(2, 1)
					.applyTo(_chkPaintWithBorder);
			_chkPaintWithBorder.setText(Messages.pref_map_layout_PaintBorder);
			_chkPaintWithBorder.addSelectionListener(_defaultSelectionListener);
		}

		{
			/*
			 * border width
			 */
			// label: border width
			_lblBorderWidth = new Label(parent, NONE);
			GridDataFactory
					.fillDefaults()//
					.indent(_firstColumnIndent, 0)
					.applyTo(_lblBorderWidth);
			_lblBorderWidth.setText(Messages.pref_map_layout_BorderWidth);

			// spinner: border width
			_spinnerBorderWidth = new Spinner(parent, SWT.BORDER);
			_spinnerBorderWidth.setMinimum(1);
			_spinnerBorderWidth.setMaximum(30);
			_spinnerBorderWidth.addSelectionListener(_defaultSelectionListener);
			_spinnerBorderWidth.addMouseWheelListener(_defaultMouseWheelListener);
		}

		{
			/*
			 * Border color
			 */

			// label
			_lblBorderColor = new Label(parent, NONE);
			GridDataFactory
					.fillDefaults()//
					.indent(_firstColumnIndent, 0)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_lblBorderColor);
			_lblBorderColor.setText(Messages.Pref_MapLayout_Label_BorderColor);

			final Composite containerBorderColor = new Composite(parent, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, false)
					.applyTo(containerBorderColor);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBorderColor);
			{
				// Radio: darker
				_rdoBorderColorDarker = new Button(containerBorderColor, SWT.RADIO);
				_rdoBorderColorDarker.setText(Messages.Pref_MapLayout_Checkbox_BorderColor_Darker);
				_rdoBorderColorDarker.addSelectionListener(_defaultSelectionListener);

				// spinner: border width
				_spinnerBorderColorDarker = new Spinner(containerBorderColor, SWT.BORDER);
				_spinnerBorderColorDarker.setMinimum(0);
				_spinnerBorderColorDarker.setMaximum(100);
				_spinnerBorderColorDarker.setPageIncrement(10);
				_spinnerBorderColorDarker.addSelectionListener(_defaultSelectionListener);
				_spinnerBorderColorDarker.addMouseWheelListener(_defaultMouseWheelListener);

				// Radio: color
				_rdoBorderColorColor = new Button(containerBorderColor, SWT.RADIO);
				_rdoBorderColorColor.setText(Messages.Pref_MapLayout_Checkbox_BorderColor_Color);
				_rdoBorderColorColor.addSelectionListener(_defaultSelectionListener);

				// border color
				_colorBorderColor = new ColorSelector(containerBorderColor);
				_colorBorderColor.addListener(_defaultChangePropertyListener);
			}
		}
	}

	private void createUI_60_PaintingMethod(final Composite parent) {

		final Display display = parent.getDisplay();

		/*
		 * checkbox: paint tour method
		 */

		final Group groupMethod = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(groupMethod);
		groupMethod.setText(Messages.Pref_MapLayout_Label_TourPaintMethod);
//		groupMethod.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			_editorTourPaintMethod = new RadioGroupFieldEditor(
					ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,
					UI.EMPTY_STRING,
					2,
					new String[][] {
							{ Messages.Pref_MapLayout_Label_TourPaintMethod_Simple, TOUR_PAINT_METHOD_SIMPLE },
							{ Messages.Pref_MapLayout_Label_TourPaintMethod_Complex, TOUR_PAINT_METHOD_COMPLEX } },
					groupMethod);

			addField(_editorTourPaintMethod);

			_pageBookPaintMethod = new PageBook(groupMethod, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.grab(true, false)
					.span(2, 1)
					.hint(350, SWT.DEFAULT)
					.indent(16, 0)
					.applyTo(_pageBookPaintMethod);

			// use a bulleted list to display this info
			final StyleRange style = new StyleRange();
			style.metrics = new GlyphMetrics(0, 0, 10);
			final Bullet bullet = new Bullet(style);

			/*
			 * simple painting method
			 */
			String infoText = Messages.Pref_MapLayout_Label_TourPaintMethod_Simple_Tooltip;
			int lineCount = Util.countCharacter(infoText, '\n');

			_pageSimple = new StyledText(_pageBookPaintMethod, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageSimple);
			_pageSimple.setText(infoText);
			_pageSimple.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			_pageSimple.setLineBullet(0, lineCount + 1, bullet);
			_pageSimple.setLineWrapIndent(0, lineCount + 1, 10);

			/*
			 * complex painting method
			 */
			infoText = Messages.Pref_MapLayout_Label_TourPaintMethod_Complex_Tooltip;
			lineCount = Util.countCharacter(infoText, '\n');

			_pageComplex = new StyledText(_pageBookPaintMethod, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageComplex);
			_pageComplex.setText(infoText);
			_pageComplex.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			_pageComplex.setLineBullet(0, lineCount + 1, bullet);
			_pageComplex.setLineWrapIndent(0, lineCount + 1, 10);
		}

		// set group margin after the fields are created
		GridLayoutFactory.swtDefaults().margins(0, 5).numColumns(2).applyTo(groupMethod);
	}

	private void createUI_80_DimmingColor(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory
					.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.pref_map_layout_dim_color);

			// dimming color
			_colorMapDimmColor = new ColorSelector(container);
			_colorMapDimmColor.addListener(_defaultChangePropertyListener);
		}
	}

	private void createUI_90_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			/*
			 * Checkbox: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_chkLiveUpdate);
			_chkLiveUpdate.setText(LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE);
			_chkLiveUpdate.setToolTipText(LEGEND_COLOR_DIALOG_CHECK_LIVE_UPDATE_TOOLTIP);
			_chkLiveUpdate.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void enableControls() {

		final boolean isTrackOpacity = _chkTrackOpacity.getSelection();
		final boolean isWithBorder = _chkPaintWithBorder.getSelection();
		final boolean isWithBorderColor = _rdoBorderColorColor.getSelection();
		final boolean isWithBorderDarker = _rdoBorderColorDarker.getSelection();

		_rdoBorderColorColor.setEnabled(isWithBorder);
		_rdoBorderColorDarker.setEnabled(isWithBorder);

		_lblBorderColor.setEnabled(isWithBorder);
		_lblBorderWidth.setEnabled(isWithBorder);

		_spinnerBorderWidth.setEnabled(isWithBorder);

		_colorBorderColor.setEnabled(isWithBorder && isWithBorderColor);
		_spinnerBorderColorDarker.setEnabled(isWithBorder && isWithBorderDarker);

		_spinnerTrackOpacity.setEnabled(isTrackOpacity);
	}

	/**
	 * fire one event for all modifications
	 */
	private void fireModificationEvent() {

		_prefStore.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	private int getBorderType() {

		final int borderType = _rdoBorderColorColor.getSelection() //
				? BORDER_TYPE_COLOR
				: _rdoBorderColorDarker.getSelection() //
						? BORDER_TYPE_DARKER
						: DEFAULT_BORDER_TYPE;

		return borderType;
	}

	private String getPlotType() {

		final String plotType;

		if (_rdoSymbolDot.getSelection()) {
			plotType = PLOT_TYPE_DOT;
		} else if (_rdoSymbolLine.getSelection()) {
			plotType = PLOT_TYPE_LINE;
		} else if (_rdoSymbolSquare.getSelection()) {
			plotType = PLOT_TYPE_SQUARE;
		} else {
			plotType = DEFAULT_PLOT_TYPE;
		}

		return plotType;
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUI(final Control parent) {

		_pc = new PixelConverter(parent);
		_firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		};

		_defaultChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeProperty();
			}
		};
	}

	/**
	 * Property was changed.
	 */
	private void onChangeProperty() {

		_isModified = true;

		enableControls();

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	@Override
	protected void performApply() {

		saveState();

		super.performApply();

		fireModificationEvent();
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

		updateUI_SetPlotType(_prefStore.getDefaultString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE));

		// opacity
		_chkTrackOpacity.setSelection(
				_prefStore.getDefaultBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(
				_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));

		/*
		 * Line
		 */
		_spinnerLineWidth.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));

		/*
		 * Border
		 */
		_chkPaintWithBorder.setSelection(
				_prefStore.getDefaultBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER));
		_spinnerBorderWidth.setSelection(
				_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

		updateUI_SetBorderType(
				_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE));
		_spinnerBorderColorDarker.setSelection(
				_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE));

		_colorBorderColor.setColorValue(
				PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR));

		/*
		 * Dimming
		 */
		_colorMapDimmColor.setColorValue(
				PreferenceConverter.getDefaultColor(
						_prefStore,
						ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR));

		_chkLiveUpdate.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.MAP_LAYOUT_LIVE_UPDATE));

		super.performDefaults();

		// display info for the selected paint method
		setUIPaintMethodInfo(_prefStore.getDefaultString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD));

		enableControls();

		performApply();
	}

	@Override
	public boolean performOk() {

		saveState();

		final boolean isOK = super.performOk();
		if (isOK && _isModified) {

			_isModified = false;

			fireModificationEvent();
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

		final String eventProperty = event.getProperty();

		if (eventProperty.equals(FieldEditor.VALUE)) {

			_isModified = true;

			if (event.getSource() == _editorTourPaintMethod) {

				// display info for the selected paint method
				final String newValue = (String) event.getNewValue();
				final String oldValue = (String) event.getOldValue();

				if (oldValue.equals(TOUR_PAINT_METHOD_SIMPLE)
						&& newValue.equals(TOUR_PAINT_METHOD_COMPLEX)
						&& net.tourbook.common.UI.IS_OSX) {

					MessageDialog.openWarning(
							getShell(),
							Messages.Pref_MapLayout_Dialog_OSX_Warning_Title,
							Messages.Pref_MapLayout_Dialog_OSX_Warning_Message);
				}

				setUIPaintMethodInfo(newValue);
			}

			enableControls();
		}

		super.propertyChange(event);

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	private void restoreState() {

		updateUI_SetPlotType(_prefStore.getString(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE));

		// opacity
		_chkTrackOpacity.setSelection(_prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));

		/*
		 * Line
		 */
		_spinnerLineWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));

		/*
		 * Border
		 */
		_chkPaintWithBorder.setSelection(_prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER));
		_spinnerBorderWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

		updateUI_SetBorderType(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE));

		_spinnerBorderColorDarker.setSelection(
				_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE));

		_colorBorderColor.setColorValue(
				PreferenceConverter.getColor(
						_prefStore,
						ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR));

		/*
		 * Map dimming
		 */
		_colorMapDimmColor.setColorValue(
				PreferenceConverter.getColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR));

		_chkLiveUpdate.setSelection(_prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_LIVE_UPDATE));

		// display info for the selected paint method
		setUIPaintMethodInfo(_prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD));

		enableControls();
	}

	private void saveState() {

		// plot type
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE, getPlotType());

		// opacity
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY, _chkTrackOpacity.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY, _spinnerTrackOpacity.getSelection());

		/*
		 * Line
		 */
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH, _spinnerLineWidth.getSelection());

		/*
		 * Border
		 */
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER, _chkPaintWithBorder.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH, _spinnerBorderWidth.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE, getBorderType());
		_prefStore.setValue(
				ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE,
				_spinnerBorderColorDarker.getSelection());

		PreferenceConverter.setValue(
				_prefStore,
				ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR,
				_colorBorderColor.getColorValue());

		/*
		 * Map dimming
		 */
		PreferenceConverter.setValue(
				_prefStore,
				ITourbookPreferences.MAP_LAYOUT_MAP_DIMM_COLOR,
				_colorMapDimmColor.getColorValue());

		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_LIVE_UPDATE, _chkLiveUpdate.getSelection());
	}

	private void setUIPaintMethodInfo(final String value) {

		if (value.equals(TOUR_PAINT_METHOD_SIMPLE)) {
			_pageBookPaintMethod.showPage(_pageSimple);
		} else {
			_pageBookPaintMethod.showPage(_pageComplex);
		}

		// 2x parents are required that the pagebook page is correctly rendered
		_containerPage.getParent().getParent().layout(true, true);
	}

	private void updateUI_SetBorderType(int borderType) {

		if (borderType != BORDER_TYPE_COLOR && borderType != BORDER_TYPE_DARKER) {
			borderType = DEFAULT_BORDER_TYPE;
		}

		_rdoBorderColorColor.setSelection(borderType == BORDER_TYPE_COLOR);
		_rdoBorderColorDarker.setSelection(borderType == BORDER_TYPE_DARKER);
	}

	private void updateUI_SetPlotType(String plotType) {

		if (plotType.equals(PLOT_TYPE_DOT) == false
				&& plotType.equals(PLOT_TYPE_LINE) == false
				&& plotType.equals(PLOT_TYPE_SQUARE) == false) {

			plotType = DEFAULT_PLOT_TYPE;
		}

		_rdoSymbolDot.setSelection(plotType.equals(PLOT_TYPE_DOT));
		_rdoSymbolLine.setSelection(plotType.equals(PLOT_TYPE_LINE));
		_rdoSymbolSquare.setSelection(plotType.equals(PLOT_TYPE_SQUARE));
	}
}
