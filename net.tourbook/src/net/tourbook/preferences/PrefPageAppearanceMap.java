/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
 
public class PrefPageAppearanceMap extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		MAP_TOUR_SYMBOL_LINE		= "line";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_DOT			= "dot";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_SQUARE		= "square";										//$NON-NLS-1$

	public static final String		TOUR_PAINT_METHOD_SIMPLE	= "simple";										//$NON-NLS-1$
	public static final String		TOUR_PAINT_METHOD_COMPLEX	= "complex";										//$NON-NLS-1$

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					_isModified;

	private Label					_lblBorderWidth;
	private Spinner					_spinnerLineWidth;
	private Spinner					_spinnerBorderWidth;
	private Text					_txtTourPaintMethod;
	private BooleanFieldEditor		_editorTourWithBorder;
	private RadioGroupFieldEditor	_editorTourPaintMethod;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUITourInMap(container);

			// spacer
			new Label(container, NONE);
			new Label(container, NONE);

			/*
			 * dimming color
			 */
			addField(new ColorFieldEditor(
					ITourbookPreferences.MAP_LAYOUT_DIM_COLOR,
					Messages.pref_map_layout_dim_color,
					container));

		}
		// force layout after the fields are set
		final GridLayout gl = (GridLayout) container.getLayout();
		gl.numColumns = 2;

		restoreState();
	}

	private void createUITourInMap(final Composite parent) {

		final Group groupContainer = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(groupContainer);
		groupContainer.setText(Messages.Pref_MapLayout_Group_TourInMapProperties);
		{
			final PixelConverter pc = new PixelConverter(groupContainer);

			/*
			 * checkbox: plot symbol
			 */
			{
				addField(new RadioGroupFieldEditor(
						ITourbookPreferences.MAP_LAYOUT_SYMBOL,
						Messages.pref_map_layout_symbol,
						3,
						new String[][] {
								{ Messages.pref_map_layout_symbol_line, MAP_TOUR_SYMBOL_LINE },
								{ Messages.pref_map_layout_symbol_dot, MAP_TOUR_SYMBOL_DOT },
								{ Messages.pref_map_layout_symbol_square, MAP_TOUR_SYMBOL_SQUARE } },
						groupContainer,
						false));

				// label: line width
				final Label label = new Label(groupContainer, NONE);
				label.setText(Messages.pref_map_layout_symbol_width);

				// spinner: line width
				_spinnerLineWidth = new Spinner(groupContainer, SWT.BORDER);
				_spinnerLineWidth.setMinimum(1);
				_spinnerLineWidth.setMaximum(50);
				_spinnerLineWidth.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeProperty();
					}
				});
				_spinnerLineWidth.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeProperty();
					}
				});
			}

			/*
			 * checkbox: paint with border
			 */
			{
				_editorTourWithBorder = new BooleanFieldEditor(
						ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER,
						Messages.pref_map_layout_PaintBorder,
						groupContainer);
				addField(_editorTourWithBorder);

				// spacer
				new Label(groupContainer, NONE);

				/*
				 * border width
				 */

				// label: border width
				_lblBorderWidth = new Label(groupContainer, NONE);
				GridDataFactory.fillDefaults().indent(UI.FORM_FIRST_COLUMN_INDENT, 0).applyTo(_lblBorderWidth);
				_lblBorderWidth.setText(Messages.pref_map_layout_BorderWidth);

				// spinner: border width
				_spinnerBorderWidth = new Spinner(groupContainer, SWT.BORDER);
				_spinnerBorderWidth.setMinimum(1);
				_spinnerBorderWidth.setMaximum(30);
				_spinnerBorderWidth.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeProperty();
					}
				});
				_spinnerBorderWidth.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeProperty();
					}
				});
			}

			/*
			 * checkbox: paint tour method
			 */

			final Group groupMethod = new Group(parent, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(groupMethod);
			groupMethod.setText(Messages.Pref_MapLayout_Label_TourPaintMethod);
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

				_txtTourPaintMethod = new Text(groupMethod, SWT.WRAP | SWT.READ_ONLY);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.span(2, 1)
						.hint(pc.convertWidthInCharsToPixels(40), pc.convertHeightInCharsToPixels(12))
						.applyTo(_txtTourPaintMethod);
				_txtTourPaintMethod.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
			// set group margin after the fields are created
			GridLayoutFactory.swtDefaults().margins(5, 5).numColumns(2).applyTo(groupMethod);
		}

		// force layout after the fields are set !!!
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupContainer);
	}

	private void enableControls(final boolean isWithBorder) {

		_lblBorderWidth.setEnabled(isWithBorder);
		_spinnerBorderWidth.setEnabled(isWithBorder);
	}

	/**
	 * fire one event for all modifications
	 */
	private void fireModificationEvent() {
		_prefStore.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {
		_isModified = true;
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

		_spinnerLineWidth.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));
		_spinnerBorderWidth.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

		super.performDefaults();

		// display info for the selected paint method
		setUIPaintMethodInfo(_prefStore.getDefaultString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD));

		// this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
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

		if (event.getProperty().equals(FieldEditor.VALUE)) {

			_isModified = true;

			if (event.getSource() == _editorTourPaintMethod) {

				// display info for the selected paint method
				final String newValue = (String) event.getNewValue();
				final String oldValue = (String) event.getOldValue();

				if (oldValue.equals(TOUR_PAINT_METHOD_SIMPLE)
						&& newValue.equals(TOUR_PAINT_METHOD_COMPLEX)
						&& net.tourbook.util.UI.IS_OSX) {

					MessageDialog.openWarning(
							getShell(),
							Messages.Pref_MapLayout_Dialog_OSX_Warning_Title,
							Messages.Pref_MapLayout_Dialog_OSX_Warning_Message);
				}

				setUIPaintMethodInfo(newValue);
			}

			enableControls(_editorTourWithBorder.getBooleanValue());
		}

		super.propertyChange(event);
	}

	private void restoreState() {

		_spinnerLineWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));
		_spinnerBorderWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

		// display info for the selected paint method
		setUIPaintMethodInfo(_prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD));

		enableControls(_prefStore.getBoolean(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER));
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH, _spinnerLineWidth.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH, _spinnerBorderWidth.getSelection());
	}

	private void setUIPaintMethodInfo(final String value) {

		if (value.equals(TOUR_PAINT_METHOD_SIMPLE)) {
			_txtTourPaintMethod.setText(Messages.Pref_MapLayout_Label_TourPaintMethod_Simple_Tooltip);
		} else {
			_txtTourPaintMethod.setText(Messages.Pref_MapLayout_Label_TourPaintMethod_Complex_Tooltip);
		}
	}
}
