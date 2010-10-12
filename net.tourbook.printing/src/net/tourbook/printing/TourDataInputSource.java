package net.tourbook.printing;

import net.tourbook.data.TourData;

import org.xml.sax.InputSource;

public class TourDataInputSource extends InputSource {

	private TourData tourData;
	
	public TourDataInputSource(TourData tourData){
		this.tourData = tourData;
	}

	public TourData getTourData() {
		return tourData;
	}

	public void setTourData(TourData tourData) {
		this.tourData = tourData;
	}
}
