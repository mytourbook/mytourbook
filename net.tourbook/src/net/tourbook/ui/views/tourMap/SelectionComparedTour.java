package net.tourbook.ui.views.tourMap;

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection contains data for a compared tour
 */
public class SelectionComparedTour implements ISelection {

	private Long	fCompareId;

	private int		fCompareStartIndex;
	private int		fCompareEndIndex;

	private float	fSpeed;

	public SelectionComparedTour(Long compareId, int startIndex, int endIndex, float speed) {

		fCompareId = compareId;

		fCompareStartIndex = startIndex;
		fCompareEndIndex = endIndex;

		fSpeed = speed;
	}

	public boolean isEmpty() {
		return false;
	}

	Long getCompareId() {
		return fCompareId;
	}

	int getCompareStartIndex() {
		return fCompareStartIndex;
	}

	int getCompareEndIndex() {
		return fCompareEndIndex;
	}

	float getSpeed() {
		return fSpeed;
	}

}
