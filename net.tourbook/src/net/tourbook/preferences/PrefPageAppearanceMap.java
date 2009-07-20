/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceMap extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		MAP_TOUR_SYMBOL_LINE	= "line";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_DOT		= "dot";											//$NON-NLS-1$
	public static final String		MAP_TOUR_SYMBOL_SQUARE	= "square";										//$NON-NLS-1$

	private final IPreferenceStore	fPrefStore				= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					fIsModified;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		addField(new RadioGroupFieldEditor(ITourbookPreferences.MAP_LAYOUT_SYMBOL,
				Messages.pref_map_layout_symbol,
				1,
				new String[][] {
						{ Messages.pref_map_layout_symbol_line, MAP_TOUR_SYMBOL_LINE },
						{ Messages.pref_map_layout_symbol_dot, MAP_TOUR_SYMBOL_DOT },
						{ Messages.pref_map_layout_symbol_square, MAP_TOUR_SYMBOL_SQUARE } },
				parent,
				true));

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);

		// line width
		final IntegerFieldEditor editor = new IntegerFieldEditor(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH,
				Messages.pref_map_layout_symbol_width,
				container);
		addField(editor);
		UI.setFieldWidth(container, editor, UI.DEFAULT_FIELD_WIDTH);
		editor.setValidRange(1, 50);

		// dim color
		addField(new ColorFieldEditor(ITourbookPreferences.MAP_LAYOUT_DIM_COLOR,
				Messages.pref_map_layout_dim_color,
				container));
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void performDefaults() {
		fIsModified = true;
		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK && fIsModified) {

			fIsModified = false;

			// fire one event for all modifications
			getPreferenceStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		fIsModified = true;
		super.propertyChange(event);
	}
}
