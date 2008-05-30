package net.tourbook.ui.views.tourTag;

import org.joda.time.DateTime;

public class TVITagViewTour extends TVITagViewItem {

	long		tourId;
	DateTime	tourDate;
	String		tourTitle;
	long		tourTypeId;

	public TVITagViewTour(final TagView tagView) {
		super(tagView);
	}

	@Override
	protected void fetchChildren() {}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

}
