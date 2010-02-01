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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;
 
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceMap extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		MAP_TOUR_SYMBOL_LINE	= "line";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_DOT		= "dot";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_SQUARE	= "square";										//$NON-NLS-1$

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					_isModified;

	private Spinner					_spinnerLineWidth;
	private Spinner					_borderWidth;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * checkbox: plot symbol
			 */
			final RadioGroupFieldEditor groupEditor = new RadioGroupFieldEditor(
					ITourbookPreferences.MAP_LAYOUT_SYMBOL,
					Messages.pref_map_layout_symbol,
					1,
					new String[][] {
							{ Messages.pref_map_layout_symbol_line, MAP_TOUR_SYMBOL_LINE },
							{ Messages.pref_map_layout_symbol_dot, MAP_TOUR_SYMBOL_DOT },
							{ Messages.pref_map_layout_symbol_square, MAP_TOUR_SYMBOL_SQUARE } },
					container,
					true);
			addField(groupEditor);
//			groupEditor.getRadioBoxControl(container);

			// spacer
			Label label = new Label(container, NONE);

			// label: line width
			label = new Label(container, NONE);
			label.setText(Messages.pref_map_layout_symbol_width);

			// spinner: line width
			_spinnerLineWidth = new Spinner(container, SWT.BORDER);
			_spinnerLineWidth.setMinimum(1);
			_spinnerLineWidth.setMaximum(50);
			_spinnerLineWidth.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeProperty();
				}
			});
			_spinnerLineWidth.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeProperty();
				}
			});

			/*
			 * checkbox: paint with border
			 */
			addField(new BooleanFieldEditor(
					ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER,
					Messages.pref_map_layout_PaintBorder,
					container));

			// spacer
			label = new Label(container, NONE);

			/*
			 * border width
			 */

			// label: border width
			label = new Label(container, NONE);
			label.setText(Messages.pref_map_layout_BorderWidth);

			// spinner: border width
			_borderWidth = new Spinner(container, SWT.BORDER);
			_borderWidth.setMinimum(1);
			_borderWidth.setMaximum(10);
			_borderWidth.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onChangeProperty();
				}
			});
			_borderWidth.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					onChangeProperty();
				}
			});

			// spacer
			label = new Label(container, NONE);
			label = new Label(container, NONE);

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

	/**
	 * fire one event for all modifications
	 */
	private void fireModificationEvent() {
		_prefStore.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {

		_isModified = true;

//		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH, _spinnerLineWidth.getSelection());
//		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH, _borderWidth.getSelection());
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
		_borderWidth.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

		super.performDefaults();

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
		_isModified = true;
		super.propertyChange(event);
	}

	private void restoreState() {

		_spinnerLineWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));
		_borderWidth.setSelection(_prefStore.getInt(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));

	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH, _spinnerLineWidth.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH, _borderWidth.getSelection());
	}
}
