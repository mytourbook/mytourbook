package net.tourbook.ui;

import java.util.ArrayList;

public class SQLData {

	String			whereString;
	ArrayList<Long>	longParameters;

	public SQLData(final String whereString, final ArrayList<Long> longParameters) {
		this.whereString = whereString;
		this.longParameters = longParameters;
	}

}
