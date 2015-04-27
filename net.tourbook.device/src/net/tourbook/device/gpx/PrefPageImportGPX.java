/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.device.gpx;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.device.Activator;
import net.tourbook.device.IPreferences;
import net.tourbook.device.Messages;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageImportGPX extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID				= "net.tourbook.device.PrefPageGPX";			//$NON-NLS-1$

	private IPreferenceStore		_prefStore		= Activator.getDefault().getPreferenceStore();
	private final IDialogSettings	_importState	= TourbookPlugin.getState(RawDataView.ID);

	private RawDataManager			_rawDataMgr		= RawDataManager.getInstance();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Button					_chkOneTour;
	private Button					_rdoDistanceRelative;
	private Button					_rdoDistanceAbsolute;

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUI_10_OneTour(container);
			createUI_20_Distance(container);
		}

		return container;
	}

	private void createUI_10_OneTour(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// checkbox: merge all tracks into one tour
			{
				_chkOneTour = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkOneTour);
				_chkOneTour.setText(Messages.PrefPage_GPX_Checkbox_OneTour);
			}
		}
	}

	private void createUI_20_Distance(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, _pc.convertVerticalDLUsToPixels(4))
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		group.setText(Messages.PrefPage_GPX_Group_DistanceValues);
		{
			// label
			{
				final Label label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
				label.setText(Messages.PrefPage_GPX_Label_DistanceValues);
			}

			// radio
			{
				final Composite container = new Composite(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.indent(_pc.convertWidthInCharsToPixels(3), 0)
						.applyTo(container);
				GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
				{
					_rdoDistanceAbsolute = new Button(container, SWT.RADIO);
					_rdoDistanceAbsolute.setText(Messages.PrefPage_GPX_Radio_DistanceAbsolute);
					_rdoDistanceAbsolute.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceAbsolute_Tooltip);

					_rdoDistanceRelative = new Button(container, SWT.RADIO);
					_rdoDistanceRelative.setText(Messages.PrefPage_GPX_Radio_DistanceRelative);
					_rdoDistanceRelative.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceRelative_Tooltip);
				}
			}
		}
	}

	@Override
	public void init(final IWorkbench workbench) {}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	@Override
	protected void performDefaults() {

		// merge all tracks into one tour
		_chkOneTour.setSelection(RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);

		// relative/absolute distance
		final boolean isRelativeDistance = _prefStore.getDefaultBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

		_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
		_rdoDistanceRelative.setSelection(isRelativeDistance);

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {
			saveState();
		}

		return isOK;
	}

	private void restoreState() {

		// merge all tracks into one tour
		final boolean isMergeIntoOneTour = Util.getStateBoolean(
				_importState,
				RawDataView.STATE_IS_MERGE_TRACKS,
				RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);
		_chkOneTour.setSelection(isMergeIntoOneTour);

		// relative/absolute distance
		final boolean isRelativeDistance = _prefStore.getBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

		_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
		_rdoDistanceRelative.setSelection(isRelativeDistance);
	}

	private void saveState() {

		// merge all tracks into one tour
		final boolean isMergeIntoOneTour = _chkOneTour.getSelection();
		_importState.put(RawDataView.STATE_IS_MERGE_TRACKS, isMergeIntoOneTour);
		_rawDataMgr.setMergeTracks(isMergeIntoOneTour);

		// relative/absolute distance
		_prefStore.setValue(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE, _rdoDistanceRelative.getSelection());
	}
}
