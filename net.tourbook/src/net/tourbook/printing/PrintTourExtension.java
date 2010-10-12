package net.tourbook.printing;

import java.util.ArrayList;

import net.tourbook.data.TourData;

public abstract class PrintTourExtension {
	
	private String	_printId;
	private String	_visibleName;

 	/**
	 * Prints the tours in the {@link TourData} list. If only one tour is printed, the values of
	 * tourStartIndex and tourEndIndex is the range which points are printed, when the index is -1,
	 * the whole tour is printed.
	 * 
	 * @param tourDataList
	 * @param tourStartIndex
	 * @param tourEndIndex
	 */
	public abstract void printTours(ArrayList<TourData> tourDataList, int tourStartIndex, int tourEndIndex);
	
	public String getPrintId() {
		return _printId;
	}

	public String getVisibleName() {
		return _visibleName;
	}

	public void setPrintId(final String fPrintId) {
		this._printId = fPrintId;
	}

	public void setVisibleName(final String fVisibleName) {
		this._visibleName = fVisibleName;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("id: ")//$NON-NLS-1$
				.append(_printId)
				.append(" \t") //$NON-NLS-1$
				//
				.append("name: ") //$NON-NLS-1$
				.append(_visibleName)
				.append(" \t") //$NON-NLS-1$
				//
				.toString();

	}
}
