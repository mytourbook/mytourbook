package net.tourbook.ui.views.tourCatalog;

public class TVIWizardCompareTour extends TVIWizardCompareItem {

	long	tourId;

	int		tourYear;
	int		tourMonth;
	int		tourDay;

	long	colDistance;
	long	colRecordingTime;
	long	colAltitudeUp;
	long	tourTypeId;

	public TVIWizardCompareTour(final TVIWizardCompareItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {}

	/**
	 * tour items do not have children
	 */
	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

}
