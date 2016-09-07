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
import java.time.ZonedDateTime;
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

		sb.append("static {\n\n"); //$NON-NLS-1$

		sb.append(String.format("\t// java.version:          %s\n\n", System.getProperty("java.version"))); //$NON-NLS-1$ //$NON-NLS-2$

		sb.append(String.format("\t// ImplementationVendor:  %s\n", javaRuntime.getImplementationVendor())); //$NON-NLS-1$
		sb.append(String.format("\t// ImplementationVersion: %s\n\n", javaRuntime.getImplementationVersion())); //$NON-NLS-1$

		sb.append(String.format("\t// SpecificationVendor:   %s\n", javaRuntime.getSpecificationVendor())); //$NON-NLS-1$
		sb.append(String.format("\t// SpecificationVersion:  %s\n", javaRuntime.getSpecificationVersion())); //$NON-NLS-1$
		sb.append("\n\n"); //$NON-NLS-1$

		sb.append("	/**\n"); //$NON-NLS-1$
		sb.append("	 * This list is create with Java 8, the index is the key for the zone id in the\n"); //$NON-NLS-1$
		sb.append("	 * Tourdatabase.\n"); //$NON-NLS-1$
		sb.append("	*/\n\n"); //$NON-NLS-1$

		sb.append(String.format("\tallTimeZones = new String[%d];\n\n", allZoneIds.size())); //$NON-NLS-1$
		sb.append(sbZones);

		sb.append("}\n"); //$NON-NLS-1$

		System.out.println(sb.toString());
	}

	public static void main(final String[] args) {

		createListWithAllTimeZones();
		testInstant();
	}

	private static void sysOut(final ZonedDateTime now, final String tzId) {

//		final ZonedDateTime zdt = now.withZoneSameInstant(ZoneId.of(tzId));
//		final ZonedDateTime zdt = now.withZoneSameLocal(ZoneId.of(tzId));

		final ZonedDateTime zdt = now.toInstant().atZone(ZoneId.of(tzId));

		final long instant = zdt.toInstant().toEpochMilli();

		System.out.println(String.format("%-50s %d", // //$NON-NLS-1$
				zdt,
				instant));
	}

	private static void testInstant() {

		final ZonedDateTime now = ZonedDateTime.now();

		sysOut(now, "Europe/Busingen"); //$NON-NLS-1$
		sysOut(now, "Europe/Dublin"); //$NON-NLS-1$
		sysOut(now, "Europe/Riga"); //$NON-NLS-1$
		sysOut(now, "America/Vancouver"); //$NON-NLS-1$

	}

}
