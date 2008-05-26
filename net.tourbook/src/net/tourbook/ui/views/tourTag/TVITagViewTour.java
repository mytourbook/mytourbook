package net.tourbook.ui.views.tourTag;

public class TVITagViewTour extends TVITagViewItem {

	long	tourId;

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
