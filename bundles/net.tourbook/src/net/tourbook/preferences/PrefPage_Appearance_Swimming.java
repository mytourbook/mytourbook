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

import java.util.ArrayList;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.swimming.StrokeStyle;
import net.tourbook.common.swimming.SwimStrokeConfig;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

public class PrefPage_Appearance_Swimming extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String			ID				= "net.tourbook.preferences.PrefPage_Appearance_Swimming";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= CommonActivator.getPrefStore();

	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private SelectionAdapter			_defaultSelectionListener;

	private Button							_chkLiveUpdate;

	private IPropertyChangeListener	_defaultChangePropertyListener;

	private ArrayList<ColorSelector>	_allSwimColors	= new ArrayList<>();

	public PrefPage_Appearance_Swimming() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
		{
//			{
//				/*
//				 * Label: Info
//				 */
//				final Label label = new Label(container, SWT.NONE);
//				label.setText(Messages.Pref_DisplayFormat_Label_Info);
//				GridDataFactory.fillDefaults()//
//						.span(2, 1)
//						.applyTo(label);
//			}

			createUI_20_SwimColors(container);
			createUI_99_LiveUpdate(container);
		}

		return container;
	}

	private void createUI_20_SwimColors(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		GridDataFactory.fillDefaults()//
//				.grab(true, false)
//				.applyTo(container);
		GridLayoutFactory.fillDefaults()
				.numColumns(2)
//				.spacing(20, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			for (final StrokeStyle strokeStyle : SwimStrokeConfig.getAllStrokeStyleList()) {

				{
					final Label label = new Label(container, SWT.NONE);
					label.setText(strokeStyle.swimStrokeText);
				}

				{
					final ColorSelector swimColor = new ColorSelector(container);
					swimColor.setColorValue(strokeStyle.graphBgColor);
					swimColor.addListener(_defaultChangePropertyListener);

					_allSwimColors.add(swimColor);
				}
			}

		}
	}

	private void createUI_99_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(0, _pc.convertVerticalDLUsToPixels(8))
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * Checkbox: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			_chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
			_chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
			_chkLiveUpdate.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void doLiveUpdate() {

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUI(final Control parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty(null);
			}
		};

		_defaultChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeProperty(event.getSource());
			}
		};
	}

	/**
	 * Property was changed.
	 *
	 * @param source
	 */
	private void onChangeProperty(final Object source) {

		ColorSelector modifiedColorSelector = null;

		StrokeStyle strokeStyle = null;
		for (int selectorIndex = 0; selectorIndex < _allSwimColors.size(); selectorIndex++) {

			final ColorSelector colorSelector = _allSwimColors.get(selectorIndex);

			if (source == colorSelector) {
				modifiedColorSelector = colorSelector;
				strokeStyle = SwimStrokeConfig.getAllStrokeStyleList()[selectorIndex];
				break;
			}
		}

		if (modifiedColorSelector == null) {
			return;
		}

		strokeStyle.graphBgColor = modifiedColorSelector.getColorValue();

		doLiveUpdate();
	}

	@Override
	protected void performApply() {

		saveState();

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		final boolean isLiveUpdate = _prefStore.getDefaultBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		_chkLiveUpdate.setSelection(isLiveUpdate);

		super.performDefaults();

		doLiveUpdate();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		final boolean isLiveUpdate = _prefStore.getBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		_chkLiveUpdate.setSelection(isLiveUpdate);
	}

	private void saveState() {

		// live update
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());

		// publish modifications
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
	}
}
