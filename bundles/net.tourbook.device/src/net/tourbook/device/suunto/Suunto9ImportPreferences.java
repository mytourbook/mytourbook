package net.tourbook.device.suunto;

import java.util.ArrayList;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.tourbook.application.TourbookPlugin;

public class Suunto9ImportPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final static ArrayList<String>	AltitudeData	= new ArrayList<String>() {
																				{
																					add(Messages.pref_altitude_gps);
																					add(Messages.pref_altitude_barometer);
																				}
																			};
	private final static ArrayList<String>	DistanceData	= new ArrayList<String>() {
																				{
																					add(Messages.pref_distance_gps);
																					add(Messages.pref_distance_providedvalues);
																				}
																			};

	private final IPreferenceStore			_prefStore		= TourbookPlugin.getDefault().getPreferenceStore();

	/*
	 * UI controls
	 */
	private Group	_groupData;

	private Combo	_comboAltitudeDataSource;
	private Combo	_comboDistanceDataSource;

	private Label	_lblAltitudeDataSource;
	private Label	_lblDistanceDataSource;

	@Override
	protected void createFieldEditors() {

		createUI();

		setupUI();

	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		/*
		 * Data
		 */
		_groupData = new Group(parent, SWT.NONE);
		_groupData.setText(Messages.pref_data_source);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupData);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupData);
		{
			// label: Altitude data source
			_lblAltitudeDataSource = new Label(_groupData, SWT.NONE);
			_lblAltitudeDataSource.setText(Messages.pref_altitude_source);
			/*
			 * combo: Altitude source
			 */
			_comboAltitudeDataSource = new Combo(_groupData, SWT.READ_ONLY | SWT.BORDER);
			_comboAltitudeDataSource.setVisibleItemCount(2);

			// label: Distance data source
			_lblDistanceDataSource = new Label(_groupData, SWT.NONE);
			_lblDistanceDataSource.setText(Messages.pref_distance_source);

			/*
			 * combo: Distance source
			 */
			_comboDistanceDataSource = new Combo(_groupData, SWT.READ_ONLY | SWT.BORDER);
			_comboDistanceDataSource.setVisibleItemCount(2);
		}
	}

	private void setupUI() {

		/*
		 * Fill-up the altitude data choices
		 */
		for (int index = 0; index < AltitudeData.size(); ++index) {
			_comboAltitudeDataSource.add(AltitudeData.get(index));
		}
		_comboAltitudeDataSource.select(_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE));
		/*
		 * Fill-up the distance data choices
		 */
		for (String distanceChoice : DistanceData) {
			_comboDistanceDataSource.add(distanceChoice);
		}
		_comboDistanceDataSource.select(_prefStore.getInt(IPreferences.DISTANCE_DATA_SOURCE));
	}

	@Override
	public void init(final IWorkbench workbench) {}

	@Override
	protected void performDefaults() {

		_comboAltitudeDataSource.select(0);
		_comboDistanceDataSource.select(0);
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {
			_prefStore.setValue(IPreferences.ALTITUDE_DATA_SOURCE, _comboAltitudeDataSource.getSelectionIndex());
			_prefStore.setValue(IPreferences.DISTANCE_DATA_SOURCE, _comboDistanceDataSource.getSelectionIndex());
		}
		return isOK;
	}

}
