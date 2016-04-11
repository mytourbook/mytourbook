/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;

public class DialogAdjustTemperature_Wizard extends Wizard {

	private final IPreferenceStore				_prefStore	= TourbookPlugin.getPrefStore();

	private DialogAdjustTemperature_WizardPage	_wizardPage;

	private ArrayList<TourData>					_selectedTours;

	public DialogAdjustTemperature_Wizard(final ArrayList<TourData> selectedTours) {

		super();

		setNeedsProgressMonitor(true);

		_selectedTours = selectedTours;
	}

	@Override
	public void addPages() {

		_wizardPage = new DialogAdjustTemperature_WizardPage(Messages.Dialog_AdjustTemperature_Dialog_Title);

		addPage(_wizardPage);
	}

	@Override
	public String getWindowTitle() {
		return Messages.Dialog_AdjustTemperature_Dialog_Title;
	}

	@Override
	public boolean performFinish() {

		try {

			getContainer().run(true, true, performFinish_getRunnable());

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.log(e);
		}

		return true;
	}

	private IRunnableWithProgress performFinish_getRunnable() {

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {

			final float	avgTemperature	= _prefStore.getFloat(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE);
			final int	durationTime	= _prefStore.getInt(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME);

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				monitor.beginTask(Messages.Dialog_AdjustTemperature_Label_Progress_Task, _selectedTours.size());


				int savedTours = 0;

				for (final TourData tourData : _selectedTours) {

					monitor.worked(1);

					if (monitor.isCanceled()) {
						break;
					}

					Thread.currentThread().sleep(1000);

					monitor.subTask(NLS.bind(
							Messages.Dialog_AdjustTemperature_Label_Progress_SubTask,
							++savedTours,
							_selectedTours.size()));
				}

				// update the UI
			}
		};

		return runnable;
	}

}
