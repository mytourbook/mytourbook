package net.tourbook.device.garmin.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.tourChart.ChartLabel;

/**
 * Wrapper for tour data used by {@link FitContext}.
 * 
 * @author Marcin Kuthan <marcin.kuthan@gmail.com>
 */
public class FitContextData {

	private final List<ContextTourData>							_allTourData	= new ArrayList<ContextTourData>();
	private final Map<ContextTourData, List<ContextTimeData>>	_attTimeData	= new HashMap<ContextTourData, List<ContextTimeData>>();
	private final Map<ContextTourData, List<ContextTourMarker>>	_allTourMarker	= new HashMap<FitContextData.ContextTourData, List<ContextTourMarker>>();

	private ContextTourData										_currentTourData;
	private ContextTimeData										_currentTimeData;
	private ContextTourMarker									_currentTourMarker;

	private List<ContextTimeData>								_currentContextTimeDataList;

	class ContextTimeData {

		private TimeData	__data;

		TimeData getTimeData() {
			return __data;
		}

		@Override
		public String toString() {
			return "ContextTimeData [__data=" + __data + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class ContextTourData {

		private TourData	__data;

		@Override
		public String toString() {
			return "ContextTourData [__data=" + __data + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class ContextTourMarker {

		private TourMarker	__data;

		@Override
		public String toString() {
			return "ContextTourMarker [__data=" + __data + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void ctxTimeData_10_Initialize() {

		ctxTourData_10_Initialize();

		_currentContextTimeDataList = _attTimeData.get(_currentTourData);
		if (_currentContextTimeDataList == null) {

			_currentContextTimeDataList = new ArrayList<ContextTimeData>();
			_attTimeData.put(_currentTourData, _currentContextTimeDataList);
		}

		_currentTimeData = new ContextTimeData();
		_currentTimeData.__data = new TimeData();

		_currentContextTimeDataList.add(_currentTimeData);
	}

	public void ctxTimeData_20_Finalize() {

		_currentTimeData = null;
	}

	public void ctxTourData_10_Initialize() {

		if (_currentTourData == null) {

			_currentTourData = new ContextTourData();
			_currentTourData.__data = new TourData();

			_allTourData.add(_currentTourData);
		}
	}

	public void ctxTourData_20_Finalize() {

		ctxTimeData_20_Finalize();

		_currentTourData = null;
		_currentContextTimeDataList = null;
	}

	public void ctxTourMarker_10_Initialize() {

		ctxTourData_10_Initialize();

		List<ContextTourMarker> currentContextTourMarkerList = _allTourMarker.get(_currentTourData);
		if (currentContextTourMarkerList == null) {

			currentContextTourMarkerList = new ArrayList<ContextTourMarker>();
			_allTourMarker.put(_currentTourData, currentContextTourMarkerList);
		}

		_currentTourMarker = new ContextTourMarker();
		_currentTourMarker.__data = new TourMarker(getCurrentTourData(), ChartLabel.MARKER_TYPE_DEVICE);

		currentContextTourMarkerList.add(_currentTourMarker);
	}

	public void ctxTourMarker_20_Finalize() {
		_currentTourMarker = null;
	}

	public List<ContextTimeData> getAllTimeData() {
		return _currentContextTimeDataList;
	}

	public TimeData getCurrentTimeData() {

		if (_currentTimeData == null) {
			throw new IllegalArgumentException("Time data is not initialized"); //$NON-NLS-1$
		}

		return _currentTimeData.__data;

	}

	public TourData getCurrentTourData() {

		if (_currentTourData == null) {
			throw new IllegalArgumentException("Tour data is not initialized"); //$NON-NLS-1$
		}

		return _currentTourData.__data;
	}

	public TourMarker getCurrentTourMarker() {

		if (_currentTourMarker == null) {
			throw new IllegalArgumentException("Tour marker is not initialized"); //$NON-NLS-1$
		}

		return _currentTourMarker.__data;
	}

	public void processData(final FitContextDataHandler handler) {

		for (final ContextTourData contextTourData : _allTourData) {

			final List<ContextTimeData> contextTimeDataList = _attTimeData.get(contextTourData);

			final ArrayList<TimeData> timeDataList = new ArrayList<TimeData>(contextTimeDataList.size());
			for (final ContextTimeData contextTimeData : contextTimeDataList) {
				timeDataList.add(contextTimeData.__data);
			}

			final int lastSerieIndex = timeDataList.size() - 1;

			final List<ContextTourMarker> contextTourMarkerList = _allTourMarker.get(contextTourData);

			final Set<TourMarker> tourMarkerSet = new HashSet<TourMarker>(contextTourMarkerList.size());
			for (final ContextTourMarker contextTourMarker : contextTourMarkerList) {

				final TourMarker tourMarker = contextTourMarker.__data;

				/*
				 * Fit devices adds a marker at the end, this is annoing therefore it is removed. It
				 * is not only the last time slice it can also be about the last 5 time slices.
				 */
				if (tourMarker.getSerieIndex() > lastSerieIndex - 5) {
					continue;
				}

				tourMarkerSet.add(tourMarker);
			}

			handler.handleTour(contextTourData.__data, timeDataList, tourMarkerSet);
		}
	}

}
