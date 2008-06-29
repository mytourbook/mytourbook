package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;

public class ChangedTags {

	private HashMap<Long, TourTag>	fModifiedTags;
	private ArrayList<TourData>		fModifiedTours;
	private boolean					fIsAddMode;

	/**
	 * Creates a copy of the modifiedTags parameter and modifiedTours parameter
	 * 
	 * @param modifiedTags
	 * @param modifiedTours
	 * @param isAddMode
	 */
	@SuppressWarnings("unchecked")
	public ChangedTags(	final HashMap<Long, TourTag> modifiedTags,
						final ArrayList<TourData> modifiedTours,
						final boolean isAddMode) {
		
		if (fModifiedTags == null) {
			fModifiedTags = new HashMap<Long, TourTag>();
		}
		
		fModifiedTags.putAll(modifiedTags);
		fModifiedTours = (ArrayList<TourData>) modifiedTours.clone();
		fIsAddMode = isAddMode;
	}

	@SuppressWarnings("unchecked")
	public ChangedTags(final TourTag modifiedTag, final ArrayList<TourData> modifiedTours, final boolean isAddMode) {

		if (fModifiedTags == null) {
			fModifiedTags = new HashMap<Long, TourTag>();
		}

		fModifiedTags.put(modifiedTag.getTagId(), modifiedTag);
		fModifiedTours = (ArrayList<TourData>) modifiedTours.clone();
		fIsAddMode = isAddMode;
	}

	/**
	 * @return Returns the modified tags
	 */
	public HashMap<Long, TourTag> getModifiedTags() {
		return fModifiedTags;
	}

	public ArrayList<TourData> getModifiedTours() {
		return fModifiedTours;
	}

	public boolean isAddMode() {
		return fIsAddMode;
	}

}
