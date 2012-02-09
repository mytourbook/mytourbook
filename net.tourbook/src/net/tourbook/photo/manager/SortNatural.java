/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package net.tourbook.photo.manager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Original: org.dawb.common.util.list.SortNatural<E>
 */
public class SortNatural<E> implements Comparator<E> {
	
	private final boolean isCaseSensitive;
	/**
	 * @param isCaseSensitive
	 */
	public SortNatural(final boolean isCaseSensitive) {
		this.isCaseSensitive = isCaseSensitive;
	}
	static char charAt(final String s, final int i) {
		if (i >= s.length()) {
			return 0;
		}
	    return s.charAt(i);

	}
	@Override
	public int compare(final Object o1, final Object o2) {
        return compare(o1,o2,isCaseSensitive);
	}
	/**
	 * @param o1
	 * @param o2
	 * @param isCaseSensitive
	 * @return i
	 */
	public int compare(Object o1, Object o2, final boolean isCaseSensitive) {
		
		String a,b;
		if (o1==null) {
			o1 = "";
		}
		if (o2==null) {
			o2 = "";
		}
		
		final String o1String = getName(o1);
		final String o2String = getName(o2);
				        
				        
		if (isCaseSensitive) {
		    a = o1String;
		    b = o2String;
		} else {
		    a = o1String;
		    b = o2String;
		    final String alc = o1String.toLowerCase();
		    final String blc = o2String.toLowerCase();
		    if (alc.equals(blc)&&!a.equals(b)) {
		    	return compare(o1,o2,true);
		    }
		    a = alc;
		    b = blc;
		}

		int ia = 0, ib = 0;
		int nza = 0, nzb = 0;
		char ca, cb;
		int result;

		while (true) {
			// only count the number of zeroes leading the last number compared
			nza = nzb = 0;

			ca = charAt(a, ia); cb = charAt(b, ib);

			// skip over leading spaces or zeros
			while (/**Character.isSpaceChar(ca) ||**/ ca == '0') {
				if (ca == '0') {
					nza++;
				} else {
					// only count consecutive zeroes
					nza = 0;
				}

				ca = charAt(a, ++ia);
			}

			while (/**Character.isSpaceChar(cb) || **/cb == '0') {
				if (cb == '0') {
					nzb++;
				} else {
					// only count consecutive zeroes
					nzb = 0;
				}

				cb = charAt(b, ++ib);
			}

			// process run of digits
			if (Character.isDigit(ca) && Character.isDigit(cb)) {
				if ((result = compareRight(a.substring(ia), b.substring(ib))) != 0) {
					return result;
				}
			}

			if (ca == 0 && cb == 0) {
				// The strings compare the same.  Perhaps the caller
				// will want to call strcmp to break the tie.
				return nza - nzb;
			}

			if (ca < cb) {
				return -1;
			} else if (ca > cb) {
				return +1;
			}

			++ia; ++ib;
		}
	}

	int compareRight(final String a, final String b)
	{
		int bias = 0;
		int ia = 0;
		int ib = 0;

		// The longest run of digits wins.  That aside, the greatest
		// value wins, but we can't know that it will until we've scanned
		// both numbers to know that they have the same magnitude, so we
		// remember it in BIAS.
		for (;; ia++, ib++) {
			final char ca = charAt(a, ia);
			final char cb = charAt(b, ib);

			if (!Character.isDigit(ca)
					&& !Character.isDigit(cb)) {
				return bias;
			} else if (!Character.isDigit(ca)) {
				return -1;
			} else if (!Character.isDigit(cb)) {
				return +1;
			} else if (ca < cb) {
				if (bias == 0) {
					bias = -1;
				}
			} else if (ca > cb) {
				if (bias == 0) {
					bias = +1;
				}
			} else if (ca == 0 && cb == 0) {
				return bias;
			}
		}
	}
	private String getName(final Object o) {
		if (o instanceof File) {
			return ((File)o).getName();
		}
		try {
			final Method getName = o.getClass().getMethod("getName");
			if (getName.isAccessible()) {
				return (String)getName.invoke(o);
			}
		} catch (final Exception ignored) {
			
		}
		return o.toString();
	}
}
