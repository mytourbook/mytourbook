/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Contains a filter for tour types
 */
public class TourTypeFilter {

	/**
	 * this is a system filter like all tour types or no tour types
	 */
	public static final int		FILTER_TYPE_SYSTEM				= 1;

	/**
	 * filter contains one tour type which is stored in the database, the tour type can be fetched
	 * with {@link #getTourType()}
	 */
	public static final int		FILTER_TYPE_DB					= 2;

	/**
	 * filter contains several tour types
	 */
	public static final int		FILTER_TYPE_TOURTYPE_SET		= 3;

	public static final int		SYSTEM_FILTER_ID_ALL			= 1;
	public static final int		SYSTEM_FILTER_ID_NOT_DEFINED	= 2;

	private int					_filterType;

	private String				_systemFilterName;
	private int					_systemFilterId;

	/**
	 * contains the tour type from the database when {@link TourTypeFilter#getFilterType()} is
	 * {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	private TourType			_tourType;

	private TourTypeFilterSet	_tourTypeSet;

	/**
	 * Create a filter with type {@link #FILTER_TYPE_SYSTEM}
	 * 
	 * @param tourType
	 */
	public TourTypeFilter(final int systemFilterId, final String filterName) {

		_filterType = FILTER_TYPE_SYSTEM;

		_systemFilterName = filterName;
		_systemFilterId = systemFilterId;
	}

	/**
	 * Create a filter with type {@link #FILTER_TYPE_DB}
	 * 
	 * @param tourType
	 */
	public TourTypeFilter(final TourType tourType) {
		_filterType = FILTER_TYPE_DB;
		_tourType = tourType;
	}

	/**
	 * Create a filter with type {@link #FILTER_TYPE_TOURTYPE_SET}
	 * 
	 * @param filterSet
	 */
	public TourTypeFilter(final TourTypeFilterSet filterSet) {
		_filterType = FILTER_TYPE_TOURTYPE_SET;
		_tourTypeSet = filterSet;
	}

	public static Image getFilterImage(final TourTypeFilter filter) {

		final int filterType = filter.getFilterType();

		Image filterImage = null;

		// set filter name/image
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filter.getTourType();
			filterImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
			break;

		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER);
			break;

		default:
			break;
		}

		return filterImage;
	}

	public static ImageDescriptor getFilterImageDescriptor(final TourTypeFilter filter) {

		final int filterType = filter.getFilterType();

		ImageDescriptor filterImageDescriptor = null;

		// set filter name/image
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filter.getTourType();
			filterImageDescriptor = UI.getInstance().getTourTypeImageDescriptor(tourType.getTypeId());
			break;

		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			filterImageDescriptor = UI.IMAGE_REGISTRY.getDescriptor(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			filterImageDescriptor = UI.IMAGE_REGISTRY.getDescriptor(UI.IMAGE_TOUR_TYPE_FILTER);
			break;

		default:
			break;
		}

		return filterImageDescriptor;
	}

	public String getFilterName() {
		switch (_filterType) {
		case FILTER_TYPE_SYSTEM:
			return _systemFilterName;
		case FILTER_TYPE_DB:
			return _tourType.getName();
		case FILTER_TYPE_TOURTYPE_SET:
			return _tourTypeSet.getName();
		default:
			break;
		}
		return "?"; //$NON-NLS-1$
	}

//	/**
//	 * @param tourTypeList
//	 * @return Returns a list with all {@link TourType}'s which this filter contains.<br>
//	 *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
//	 *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
//	 */
//	public ArrayList<TourType> getActiveTourTypes(ArrayList<TourType> tourTypeList) {
//
//		ArrayList<TourType> returnList;
//
//		switch (fFilterType) {
//		case FILTER_TYPE_SYSTEM:
//			if (fSystemFilterId == SYSTEM_FILTER_ID_ALL) {
//				return tourTypeList;
//			} else {
//				// tour type is not defined
//				return new ArrayList<TourType>();
//			}
//
//		case FILTER_TYPE_DB:
//
//			returnList = new ArrayList<TourType>();
//			returnList.add(fTourType);
//
//			return returnList;
//
//		case FILTER_TYPE_TOURTYPE_SET:
//
//			final Object[] tourTypes = fTourTypeSet.getTourTypes();
//
//			if (tourTypes.length == 0) {
//
//				return null;
//
//			} else {
//
//				// select all tours were the tour type is defined in the tour type list
//
//				returnList = new ArrayList<TourType>();
//
//				for (Object item : tourTypes) {
//					returnList.add((TourType) item);
//				}
//
//				return returnList;
//			}
//
//		default:
//			break;
//		}
//
//		return null;
//	}

	/**
	 * @return Returns the filter type of this filter which is one of
	 *         {@link TourTypeFilter#FILTER_TYPE_*}
	 */
	public int getFilterType() {
		return _filterType;
	}

	/**
	 * @return Returns a sql string for the WHERE clause to select the tour types in the database
	 */
	public SQLData getSQLData() {

		String sqlWhereClause = UI.EMPTY_STRING;
		final ArrayList<Long> sqlTourTypes = new ArrayList<Long>();

		switch (_filterType) {
		case FILTER_TYPE_SYSTEM:
			if (_systemFilterId == SYSTEM_FILTER_ID_ALL) {
				// select all tour types also not defined tour types
				sqlWhereClause = UI.EMPTY_STRING;
			} else {
				// select tour types which are not defined
				sqlWhereClause = " AND TourData.tourType_typeId IS NULL"; //$NON-NLS-1$
			}
			break;

		case FILTER_TYPE_DB:

			sqlWhereClause = " AND TourData.tourType_typeId=?"; //$NON-NLS-1$
			sqlTourTypes.add(_tourType.getTypeId());
			break;

		case FILTER_TYPE_TOURTYPE_SET:

			final Object[] tourTypes = _tourTypeSet.getTourTypes();

			if (tourTypes.length == 0) {
				// select nothing
				sqlWhereClause = " AND 1=0"; //$NON-NLS-1$

			} else {

				// select all tours were the tour type is defined in the tour type list

				int itemIndex = 0;
				final StringBuilder sb = new StringBuilder();

				for (final Object item : tourTypes) {

					if (itemIndex > 0) {
						sb.append(" OR "); //$NON-NLS-1$
					}

					sb.append(" TourData.tourType_typeId=?"); //$NON-NLS-1$

					sqlTourTypes.add(((TourType) item).getTypeId());

					itemIndex++;
				}

				sqlWhereClause = " AND (" + sb.toString() + ") \n"; //$NON-NLS-1$ //$NON-NLS-2$
			}

			break;

		default:
			break;
		}

		return new SQLData(sqlWhereClause, sqlTourTypes);
	}

	public int getSystemFilterId() {
		return _systemFilterId;
	}

	public String getSystemFilterName() {
		return _systemFilterName;
	}

	/**
	 * @return Returns the tour type from the database when {@link TourTypeFilter#getFilterType()}
	 *         is {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	public TourType getTourType() {
		return _tourType;
	}

	/**
	 * @return Returns the filterset when the filter type {@link TourTypeFilter#getFilterType()}
	 *         returns {@link TourTypeFilter#FILTER_TYPE_TOURTYPE_SET}
	 */
	public TourTypeFilterSet getTourTypeSet() {
		return _tourTypeSet;
	}

	public void setName(final String filterName) {
		switch (_filterType) {
		case FILTER_TYPE_SYSTEM:
			// not supported
			break;

		case FILTER_TYPE_DB:
			// not supported
			break;

		case FILTER_TYPE_TOURTYPE_SET:
			_tourTypeSet.setName(filterName);
			break;

		default:
			break;
		}
	}

	/**
	 * @return Returns <code>true</code> when the filter allows {@link TourData} which has no
	 *         {@link TourType}, this tours will be painted with the default color
	 */
	public boolean showUndefinedTourTypes() {
		return _filterType == FILTER_TYPE_SYSTEM;

	}

	@Override
	public String toString() {
		return "TourTypeFilter ["
				+ ("filterType=" + _filterType)
				+ (", tourType=" + _tourType)
				+ (", tourTypeSet=" + _tourTypeSet)
				+ (", systemFilterName=" + _systemFilterName)
				+ (", systemFilterId=" + _systemFilterId)
				+ "]";
	}

}
