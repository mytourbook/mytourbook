/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.time;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;

public class AllTimeZones {

	public static String[]	allTimeZones;


	private static void createListWithAllTimeZones() {

		// create a list with all zone Id's

		final StringBuilder sbZones = new StringBuilder();

		final ArrayList<String> allZoneIds = new ArrayList<>(ZoneId.getAvailableZoneIds());

		// sort by name
		Collections.sort(allZoneIds);

		for (int zoneIndex = 0; zoneIndex < allZoneIds.size(); zoneIndex++) {

			final String rawZoneId = allZoneIds.get(zoneIndex);

//			sbZones.append(String.format("\tallTimeZones[%d] = \"%s\"; //$NON-NLS-1$\n", zoneIndex, rawZoneId)); //$NON-NLS-1$
			sbZones.append(String.format("\tallTimeZones[%d] = \"%s\";\n", zoneIndex, rawZoneId)); //$NON-NLS-1$
		}

		final StringBuilder sb = new StringBuilder();

		final Package javaRuntime = Runtime.class.getPackage();

		sb.append("static {\n\n");

		sb.append(String.format("\t// java.version:          %s\n\n", System.getProperty("java.version"))); //$NON-NLS-1$

		sb.append(String.format("\t// ImplementationVendor:  %s\n", javaRuntime.getImplementationVendor())); //$NON-NLS-1$
		sb.append(String.format("\t// ImplementationVersion: %s\n\n", javaRuntime.getImplementationVersion())); //$NON-NLS-1$

		sb.append(String.format("\t// SpecificationVendor:   %s\n", javaRuntime.getSpecificationVendor())); //$NON-NLS-1$
		sb.append(String.format("\t// SpecificationVersion:  %s\n", javaRuntime.getSpecificationVersion())); //$NON-NLS-1$
		sb.append("\n\n");

		sb.append("	/**\n");
		sb.append("	 * This list is create with Java 8, the index is the key for the zone id in the\n");
		sb.append("	 * Tourdatabase.\n");
		sb.append("	*/\n\n");

		sb.append(String.format("\tallTimeZones = new String[%d];\n\n", allZoneIds.size())); //$NON-NLS-1$
		sb.append(sbZones);

		sb.append("}\n");

		System.out.println(sb.toString());
	}

	public static void main(final String[] args) {

		createListWithAllTimeZones();
	}

}
