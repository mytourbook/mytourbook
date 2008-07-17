package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVIWizardCompareYear extends TVIWizardCompareItem {

	int		tourYear;

	TVIWizardCompareYear(final TVIWizardCompareItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT");

		sb.append(" startYear, ");
		sb.append(" startMonth ");

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA);

		sb.append(" WHERE startYear=?");

		sb.append(" GROUP BY startYear, startMonth");
		sb.append(" ORDER BY startMonth");

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setInt(1, tourYear);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVIWizardCompareMonth monthItem = new TVIWizardCompareMonth(this);
				children.add(monthItem);

				final int dbYear = result.getInt(1);
				final int dbMonth = result.getInt(2);
				fCalendar.set(dbYear, dbMonth - 1, 1);
				monthItem.treeColumn = UI.MonthFormatter.format(fCalendar.getTime());

				monthItem.tourYear = dbYear;
				monthItem.tourMonth = dbMonth;
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}

}
