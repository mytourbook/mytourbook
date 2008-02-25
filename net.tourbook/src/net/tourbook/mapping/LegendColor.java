/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;


public class LegendColor {

	public static final String[]	BRIGHTNESS_LABELS		= new String[] { "Keep Color", "Dim Color", "Lighten Color" };

	public static final int			BRIGHTNESS_DEFAULT		= 0;
	public static final int			BRIGHTNESS_DIMMING		= 1;
	public static final int			BRIGHTNESS_LIGHTNING	= 2;

	public ValueColor[]				valueColors				= new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 100, 100, 0),
			new ValueColor(100, 0, 255, 0),
			new ValueColor(150, 0, 100, 100),
			new ValueColor(190, 0, 0, 255)					};

	/**
	 * min and max value is painted black when {@link #minBrightnessFactor}==100, a value below 100
	 * will dim the color
	 */
	public int						minBrightness			= BRIGHTNESS_DEFAULT;
	public int						minBrightnessFactor		= 100;

	public int						maxBrightness			= BRIGHTNESS_DEFAULT;
	public int						maxBrightnessFactor		= 100;

	public LegendColor() {}

	public LegendColor(	ValueColor[] valueColors,
						int minBrightness,
						int minBrightnessFactor,
						int maxBrightness,
						int maxBrightnessFactor) {

		this.valueColors = valueColors;
		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

//	/**
//	 * Read filter list from xml file
//	 * 
//	 * @return Returns a list with all filters from the xml file
//	 */
//	private static ArrayList<TourTypeFilter> readXMLData() {
//
//		ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
//		ArrayList<TourTypeFilter> filterList = new ArrayList<TourTypeFilter>();
//
//		IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
//		String filename = stateLocation.append(MEMENTO_LEGEND_COLOR_FILE).toFile().getAbsolutePath();
//
//		// check if filter file is available
//		File inputFile = new File(filename);
//		if (inputFile.exists() == false) {
//			return filterList;
//		}
//
//		InputStreamReader reader = null;
//		long tourTypeId;
//
////		try {
////			reader = new InputStreamReader(new FileInputStream(inputFile), "UTF-8"); //$NON-NLS-1$
////			XMLMemento mementoFilterList = XMLMemento.createReadRoot(reader);
////
////			IMemento[] mementoFilters = mementoFilterList.getChildren(MEMENTO_CHILD_FILTER);
////
////			for (IMemento mementoFilter : mementoFilters) {
////
////				Integer filterType = mementoFilter.getInteger(TAG_FILTER_TYPE);
////				String filterName = mementoFilter.getString(TAG_NAME);
////
////				if (filterType == null || filterName == null) {
////					continue;
////				}
////
////				switch (filterType) {
////				case TourTypeFilter.FILTER_TYPE_SYSTEM:
////					Integer systemId = mementoFilter.getInteger(TAG_SYSTEM_ID);
////					if (systemId == null) {
////						continue;
////					}
////
////					filterList.add(new TourTypeFilter(systemId, filterName));
////
////					break;
////
////				case TourTypeFilter.FILTER_TYPE_DB:
////
////					String tourTypeIdString = mementoFilter.getString(TAG_TOUR_TYPE_ID);
////
////					if (tourTypeIdString == null) {
////						continue;
////					}
////
////					tourTypeId = Long.parseLong(tourTypeIdString);
////
////					// find the tour type in the available tour types
////					for (TourType tourType : tourTypes) {
////						if (tourType.getTypeId() == tourTypeId) {
////							filterList.add(new TourTypeFilter(tourType));
////							break;
////						}
////					}
////
////					break;
////
////				case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
////
////					ArrayList<TourType> tourTypesInFilter = new ArrayList<TourType>();
////					IMemento[] mementoTourTypes = mementoFilter.getChildren(MEMENTO_CHILD_TOURTYPE);
////
////					// get all tour types
////					for (IMemento mementoTourType : mementoTourTypes) {
////
////						tourTypeIdString = mementoTourType.getString(TAG_TOUR_TYPE_ID);
////						if (tourTypeIdString == null) {
////							continue;
////						}
////
////						tourTypeId = Long.parseLong(tourTypeIdString);
////
////						// find the tour type in the available tour types
////						for (TourType tourType : tourTypes) {
////							if (tourType.getTypeId() == tourTypeId) {
////								tourTypesInFilter.add(tourType);
////								break;
////							}
////						}
////					}
////
////					TourTypeFilterSet filterSet = new TourTypeFilterSet();
////					filterSet.setName(filterName);
////					filterSet.setTourTypes(tourTypesInFilter.toArray());
////
////					filterList.add(new TourTypeFilter(filterSet));
////
////					break;
////
////				default:
////					break;
////				}
////			}
////
////		} catch (UnsupportedEncodingException e) {
////			e.printStackTrace();
////		} catch (FileNotFoundException e) {
////			e.printStackTrace();
////		} catch (WorkbenchException e) {
////			e.printStackTrace();
////		} catch (NumberFormatException e) {
////			e.printStackTrace();
////		} finally {
////			if (reader != null) {
////				try {
////					reader.close();
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////			}
////		}
//
//		return filterList;
//	}

	/**
	 * create a string which is a java contructor for the {@link LegendColor}
	 * 
	 * <pre>
	 * 
	 * new LegendColor(new ValueColor[] {
	 * 		new ValueColor(10, 161, 85, 0),
	 * 		new ValueColor(50, 232, 169, 0),
	 * 		new ValueColor(100, 96, 218, 0),
	 * 		new ValueColor(150, 107, 193, 255),
	 * 		new ValueColor(190, 206, 247, 255) }, LegendColor.BRIGHTNESS_DIMMING, 15, LegendColor.BRIGHTNESS_LIGHTNING, 100)
	 * 
	 * </pre>
	 */
	public String createConstructor() {

		StringBuffer buffer = new StringBuffer();

		buffer.append('\n');
		buffer.append('\n');
		buffer.append("new LegendColor(new ValueColor[] {");
		buffer.append('\n');

		int index = 0;
		for (ValueColor valueColor : valueColors) {
			buffer.append(valueColor.toString());

			if (index++ != valueColors.length - 1) {
				buffer.append(',');
			}
			buffer.append('\n');
		}
		buffer.append("}, ");
		buffer.append('\n');

		buffer.append("LegendColor.");
		buffer.append(getBrightnessText(minBrightness));
		buffer.append(",\n");

		buffer.append(minBrightnessFactor);
		buffer.append(",\n");

		buffer.append("LegendColor.");
		buffer.append(getBrightnessText(maxBrightness));
		buffer.append(",\n");

		buffer.append(maxBrightnessFactor);
		buffer.append(")\n");

		return buffer.toString();
	}

	private String getBrightnessText(int brightness) {

		switch (brightness) {
		case BRIGHTNESS_DEFAULT:
			return "BRIGHTNESS_DEFAULT";
		case BRIGHTNESS_DIMMING:
			return "BRIGHTNESS_DIMMING";
		case BRIGHTNESS_LIGHTNING:
			return "BRIGHTNESS_LIGHTNING";

		default:
			break;
		}

		return "";
	}

	/**
	 * Creates a copy for this {@link LegendColor}
	 * 
	 * @return
	 */
	public LegendColor getCopy() {

		LegendColor copy = new LegendColor();

		copy.valueColors = new ValueColor[valueColors.length];
		for (int colorIndex = 0; colorIndex < valueColors.length; colorIndex++) {
			copy.valueColors[colorIndex] = new ValueColor(valueColors[colorIndex]);
		}

		copy.minBrightness = minBrightness;
		copy.minBrightnessFactor = minBrightnessFactor;

		copy.maxBrightness = maxBrightness;
		copy.maxBrightnessFactor = maxBrightnessFactor;

		return copy;
	}

}
