package net.tourbook.ui.views.tourMap;

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * This selection contains data for a compared tour
 */
public class SelectionComparedTour implements ISelection {

	/**
	 * Id of the reference tour
	 */
	private Long			fRefId;

	private TreeViewer		fTreeViewer;

	private Long			fCompTourId;
	private Long			fCompareId;
	private int				fCompareStartIndex;
	private int				fCompareEndIndex;

	private TVITourMapYear	fYearItem;

	public SelectionComparedTour(TreeViewer treeViewer, Long refId) {
		fTreeViewer = treeViewer;
		fRefId = refId;
	}

	public int getCompareEndIndex() {
		return fCompareEndIndex;
	}

	/**
	 * @return Returns the key for the {@link TourCompared} instance
	 */
	public Long getCompareId() {
		return fCompareId;
	}

	public int getCompareStartIndex() {
		return fCompareStartIndex;
	}

	/**
	 * @return Returns the tour Id for the {@link TourData} of the compared tour
	 */
	public Long getCompTourId() {
		return fCompTourId;
	}

	public Long getRefId() {
		return fRefId;
	}

	TreeViewer getTourViewer() {
		return fTreeViewer;
	}

	public TVITourMapYear getYearItem() {
		return fYearItem;
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Set data for the compared tour
	 * 
	 * @param compareId
	 *        database Id for the compared tour
	 * @param compTourId
	 *        database Id for the compared tour data
	 * @param compStartIndex
	 *        start index of the x-marker
	 * @param compEndIndex
	 *        end index of the x-marker
	 */
	public void setTourCompareData(	long compareId,
									long compTourId,
									int compStartIndex,
									int compEndIndex) {

		fCompareId = compareId;
		fCompTourId = compTourId;
		fCompareStartIndex = compStartIndex;
		fCompareEndIndex = compEndIndex;
	}

	public void setYearItem(TVITourMapYear yearItem) {
		fYearItem = yearItem;
	}

}
