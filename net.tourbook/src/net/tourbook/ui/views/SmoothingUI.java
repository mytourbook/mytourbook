/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.database.IComputeTourValues;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;

public class SmoothingUI {

	private final IPreferenceStore		_prefStore			= TourbookPlugin.getDefault().getPreferenceStore();

	private ITourEventListener			_tourEventListener;

	private boolean						_isUpdateUI;

	private ISmoothingAlgorithm			_smoothingInitial	= new SmoothingAlgorithmInitial();
	private ISmoothingAlgorithm			_smoothingJamet		= new SmoothingAlgorithmJamet();

	private NumberFormat				_nf0				= NumberFormat.getNumberInstance();
	{
		_nf0.setMinimumFractionDigits(0);
		_nf0.setMaximumFractionDigits(0);
	}

	private static SmoothingAlgorithm[]	SMOOTHING_ALGORITHM	= {
			//
			new SmoothingAlgorithm(
					ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL,
					Messages.TourChart_Smoothing_Algorithm_Initial),
			new SmoothingAlgorithm(
					ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET,
					Messages.TourChart_Smoothing_Algorithm_Jamet),
															//
															};

	/*
	 * UI controls
	 */
	private FormToolkit					_tk;

	private Composite					_uiContainer;
	private Combo						_comboAlgorithm;

	private PageBook					_pagebookSmoothingAlgo;
	private Composite					_pageJamet;
	private Composite					_pageInitial;

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				// don't listen to the own events
				if (part == SmoothingUI.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {
					updateUIFromPropertyEvent();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	public void computeSmoothingForAllTours() {

		// get smoothing algorithm
		final String prefAlgoId = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);
		int prefAlgoIndex = -1;
		for (int algoIndex = 0; algoIndex < SMOOTHING_ALGORITHM.length; algoIndex++) {
			if (SMOOTHING_ALGORITHM[algoIndex].algorithmId.equals(prefAlgoId)) {
				prefAlgoIndex = algoIndex;
				break;
			}
		}
		if (prefAlgoIndex == -1) {
			// this case should not happen
			prefAlgoIndex = 0;
		}

		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.TourChart_Smoothing_Dialog_SmoothAllTours_Title,
				NLS.bind(
						Messages.TourChart_Smoothing_Dialog_SmoothAllTours_Message,
						SMOOTHING_ALGORITHM[prefAlgoIndex].uiText)) == false) {
			return;
		}

		TourDatabase.computeValuesForAllTours(new IComputeTourValues() {

			public boolean computeTourValues(final TourData oldTourData) {

				oldTourData.computeComputedValues();

				return true;
			}

			public String getResultText() {
				return null;
			}

			public String getSubTaskText(final TourData savedTourData) {
				return null;
			}
		});

		fireTourModifyEvent();
	}

	public void createUI(final Composite parent, final boolean isShowDescription) {

		createUI10(parent, isShowDescription);
		setupUI();

		restoreState();
		updateUI();

		addTourEventListener();
	}

	private void createUI10(final Composite parent, final boolean isShowDescription) {

		initUI(parent);

		_uiContainer = _tk.createComposite(parent);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_uiContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_uiContainer);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		{
			createUI10SmoothingAlgorithm(_uiContainer);

			/*
			 * pagebook: smoothing algorithm
			 */
			_pagebookSmoothingAlgo = new PageBook(_uiContainer, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.span(2, 1)
					.applyTo(_pagebookSmoothingAlgo);
			{
				_pageInitial = _smoothingInitial.createUI(this, _pagebookSmoothingAlgo, isShowDescription);
				_pageJamet = _smoothingJamet.createUI(this, _pagebookSmoothingAlgo, isShowDescription);
			}
		}
	}

	private void createUI10SmoothingAlgorithm(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 5).applyTo(container);
		{
			/*
			 * label: smoothing algorithm
			 */
			final Label label = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_SmoothingAlgorithm);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			/*
			 * combo: smoothing algorithm
			 */
			_comboAlgorithm = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboAlgorithm);
			_tk.adapt(_comboAlgorithm, true, true);
			_comboAlgorithm.setVisibleItemCount(10);
			_comboAlgorithm.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					if (_isUpdateUI) {
						return;
					}
					onSelectSmoothingAlgo();
				}
			});
		}
	}

	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_smoothingInitial.dispose();
		_smoothingJamet.dispose();

		_tk.dispose();
	}

	private void fireTourModifyEvent() {

		TourManager.getInstance().removeAllToursFromCache();
		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.ALL_TOURS_ARE_MODIFIED);
	}

	private SmoothingAlgorithm getSelectedAlgorithm() {
		return SMOOTHING_ALGORITHM[_comboAlgorithm.getSelectionIndex()];
	}

	private void initUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());
	}

	private void onSelectSmoothingAlgo() {

		updateUI();

		// update pref store
		saveState();

		// force tours to be recomputed
		TourManager.getInstance().removeAllToursFromCache();

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
	}

	public void performDefaults() {

		final String defaultSmoothingId = _prefStore.getDefaultString(//
				ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);

		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM, defaultSmoothingId);

		selectSmoothingAlgo(defaultSmoothingId);

		updateUI();

		final SmoothingAlgorithm selectedSmoothingAlgo = getSelectedAlgorithm();

		_smoothingInitial.performDefaults(//
				selectedSmoothingAlgo.algorithmId.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL));

		_smoothingJamet.performDefaults(//
				selectedSmoothingAlgo.algorithmId.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET));
	}

	private void restoreState() {

		_isUpdateUI = true;
		{
			// smoothing algorithm
			selectSmoothingAlgo(_prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM));
		}
		_isUpdateUI = false;
	}

	/**
	 * Update new values in the pref store
	 */
	private void saveState() {

		// smoothing algorithm
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM,
				getSelectedAlgorithm().algorithmId);
	}

	private void selectSmoothingAlgo(final String prefAlgoId) {

		int prefAlgoIndex = -1;
		for (int algoIndex = 0; algoIndex < SMOOTHING_ALGORITHM.length; algoIndex++) {
			if (SMOOTHING_ALGORITHM[algoIndex].algorithmId.equals(prefAlgoId)) {
				prefAlgoIndex = algoIndex;
				break;
			}
		}
		if (prefAlgoIndex == -1) {
			prefAlgoIndex = 0;
		}
		_comboAlgorithm.select(prefAlgoIndex);
	}

	private void setupUI() {

		_isUpdateUI = true;
		{
			/*
			 * fillup algorithm combo
			 */
			for (final SmoothingAlgorithm algo : SMOOTHING_ALGORITHM) {
				_comboAlgorithm.add(algo.uiText);
			}
			_comboAlgorithm.select(0);
		}
		_isUpdateUI = false;
	}

	private void updateUI() {

		final String selectedSmoothingAlgo = getSelectedAlgorithm().algorithmId;

		// select smoothing page
		if (selectedSmoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_INITIAL)) {

			_pagebookSmoothingAlgo.showPage(_pageInitial);

		} else if (selectedSmoothingAlgo.equals(ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET)) {

			_pagebookSmoothingAlgo.showPage(_pageJamet);
		}

		UI.updateScrolledContent(_uiContainer);
	}

	private void updateUIFromPropertyEvent() {

		_smoothingInitial.updateUIFromPrefStore();
		_smoothingJamet.updateUIFromPrefStore();

		restoreState();

		updateUI();
	}

}
