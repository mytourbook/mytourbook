package net.tourbook.printing;

import java.util.ArrayList;

import net.tourbook.data.TourData;

public abstract class PrintTourExtension {
	
	private String	fPrintId;
	private String	fVisibleName;

 	/**
	 * Prints the tour in the {@link TourData} list. If only one tour is printed, the values of
	 * tourStartIndex and tourEndIndex is the range which points are printed, when the index is -1,
	 * the whole tour is printed.
	 * 
	 * @param tourDataList
	 * @param tourStartIndex
	 * @param tourEndIndex
	 */
	public abstract void printTours(ArrayList<TourData> tourDataList, int tourStartIndex, int tourEndIndex);

	public String getPrintId() {
		return fPrintId;
	}

	public String getVisibleName() {
		return fVisibleName;
	}

	public void setPrintId(final String fPrintId) {
		this.fPrintId = fPrintId;
	}

	public void setVisibleName(final String fVisibleName) {
		this.fVisibleName = fVisibleName;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("id: ")//$NON-NLS-1$
				.append(fPrintId)
				.append(" \t") //$NON-NLS-1$
				//
				.append("name: ") //$NON-NLS-1$
				.append(fVisibleName)
				.append(" \t") //$NON-NLS-1$
				//
				.toString();

	}
}
