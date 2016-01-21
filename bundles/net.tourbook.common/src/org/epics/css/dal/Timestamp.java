/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

/**
 *
 */
package org.epics.css.dal;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * the original code has been modified:
 * - format modifications
 * - new format
 * 
 * 2012-04-25 original location
 * 
 * http://cs-studio.hg.sourceforge.net/hgweb/cs-studio/cs-studio/raw-file/b4516ab19b6b/core/plugins/org.csstudio.platform.libs.dal/src/org/epics/css/dal/Timestamp.java
 */

/**
 * This is timestamp object with nanosecond resolution. It holds two long values. One is with
 * millisoecnd resolution and represents Java standart UTC format. Second long value is with
 * nanosecond resolution and its absolute value is lower than 1ms or 1000000ns.
 * 
 * @author ikriznar
 */
public final class Timestamp implements Comparable<Timestamp> {

	private long							milliseconds;
	private long							nanoseconds;

	private final static SimpleDateFormat	formatFull				= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$
	private final static SimpleDateFormat	formatDateTimeSeconds	= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
	private final static SimpleDateFormat	formatDateTime			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"); //$NON-NLS-1$
	private final static SimpleDateFormat	formatDate				= new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
	private final static SimpleDateFormat	formatLog				= new SimpleDateFormat("HH:mm:ss.SSS "); //$NON-NLS-1$

	public enum Format {
		/** Format to ISO with "YYYY-MM-DD". */
		Date,

		/** Format to ISO with "YYYY-MM-DDTHH:MM". */
		DateTime,

		/** Format to ISO with "YYYY-MM-DD HH:MM:SS". */
		DateTimeSeconds,

		/** Format to ISO with full precision "YYYY/MM/DD HH:MM:SS.000000000". */
		Full,

		/** HH:mm:ss.SSS */
		Log;
	}

	/**
	 * Default constructor, uses system time for initialization.
	 */
	public Timestamp() {
		this((System.currentTimeMillis() / 1000) * 1000, currentSecondInNano());
	}

	/**
	 * Creates timestamp representing provided values. If nanoseconds exceed 1000000 or -1000000
	 * then they are truncated to nanoseconds within millisecond and millisecond is corrected.
	 * 
	 * @param milli
	 * @param nano
	 */
	public Timestamp(final long milli, long nano) {
		// reduce precision
		nano /= 1000;
		nano *= 1000;

		// correction if there is more nanoseconds than it fits in
		if (nano >= 1000000) {
			final long t = nano / 1000000;
			milliseconds = milli + t;
			nanoseconds = nano - t * 1000000;
		} else if (nano <= -1000000) {
			final long t = nano / 1000000;
			milliseconds = milli + t - 1;
			nanoseconds = nano - t * 1000000 + 1000000;
		} else if (nano < 0) {
			milliseconds = milli - 1;
			nanoseconds = nano + 1000000;
		} else {
			milliseconds = milli;
			nanoseconds = nano;
		}
	}

	private final static long currentSecondInNano() {
		final long l = System.nanoTime();

		return l - ((l / 1000000000) * 1000000000);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(T)
	 */
	public int compareTo(final Timestamp o) {
		if (o instanceof Timestamp) {
			final Timestamp t = o;
			long d = milliseconds - t.milliseconds;

			if (d != 0) {
				return (int) d;
			}

			d = nanoseconds - t.nanoseconds;

			return (int) d;
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Timestamp) {
			final Timestamp t = (Timestamp) obj;

			return t.milliseconds == milliseconds && t.nanoseconds == nanoseconds;
		}

		return false;
	}

	/**
	 * Returns time in milliseconds since epoch (standard Java UTC time, as returned by
	 * System.currentTimeMillis())
	 * 
	 * @return Returns the milliseconds.
	 */
	public long getMilliseconds() {
		return milliseconds;
	}

	/**
	 * @return Returns the nanoseconds within the millisecond.
	 */
	public long getNanoseconds() {
		return nanoseconds;
	}

	/**
	 * Get seconds since epoch, i.e. 1 January 1970 0:00 UTC.
	 * 
	 * @return Seconds since 1970.
	 */
	public long getSeconds() {
		return milliseconds / 1000L;
	}

	/**
	 * @return Returns <code>true</code> if this time stamp is greater than or equal to the
	 *         <code>other</code> time stamp.
	 * @param other
	 *            Other time stamp
	 */
	public boolean isGreaterOrEqual(final Timestamp other) {
		if (milliseconds < other.milliseconds) {
			return false;
		}
		if (milliseconds == other.milliseconds) {
			return nanoseconds >= other.nanoseconds;
		}
		return true;
	}

	/**
	 * @return Returns <code>true</code> if this time stamp is greater than the <code>other</code>
	 *         time stamp.
	 * @param other
	 *            Other time stamp
	 */
	public boolean isGreaterThan(final Timestamp other) {
		if (milliseconds < other.milliseconds) {
			return false;
		}
		if (milliseconds == other.milliseconds) {
			return nanoseconds > other.nanoseconds;
		}
		return true;
	}

	/**
	 * @return Returns <code>true</code> if this time stamp is smaller than or equal to the
	 *         <code>other</code> time stamp.
	 * @param other
	 *            Other time stamp
	 */
	public boolean isLessOrEqual(final Timestamp other) {
		if (milliseconds > other.milliseconds) {
			return false;
		}
		if (milliseconds == other.milliseconds) {
			return nanoseconds <= other.nanoseconds;
		}
		return true;
	}

	/**
	 * @return Returns <code>true</code> if this time stamp is smaller than the <code>other</code>
	 *         time stamp.
	 * @param other
	 *            Other time stamp
	 */
	public boolean isLessThan(final Timestamp other) {
		if (milliseconds > other.milliseconds) {
			return false;
		}
		if (milliseconds == other.milliseconds) {
			return nanoseconds < other.nanoseconds;
		}
		return true;
	}

	/**
	 * @return Returns <code>true</code> if time fields &gt; 0.
	 */
	public boolean isValid() {
		return milliseconds > 0;
	}

	/**
	 * Converts timestamp to double.
	 * 
	 * @return Return seconds and fractional nanoseconds.
	 */
	public double toDouble() {
		return milliseconds / 1000.0 + nanoseconds / 1000000000.0;
	}

	/**
	 * Returns time in nanoseconds since epoch. Not that this in only usefull for calculating time
	 * difference for up to 292 years (2<sup>63</sup> nanoseconds) since this is maximum time
	 * possible in nanoseconds due to long value range overflow.
	 * 
	 * @return up to approx. 292 years big nano time
	 */
	public long toNanoTime() {
		return milliseconds * 1000000 + nanoseconds;
	}

	@Override
	public String toString() {

		final StringBuffer sb = new StringBuffer(32);

		formatFull.format(new Date(milliseconds), sb, new FieldPosition(DateFormat.FULL));

//		if (nanoseconds < 100000) {
//			sb.append('0');
//
//			if (nanoseconds < 10000) {
//				sb.append('0');
//
//				if (nanoseconds < 1000) {
//					sb.append('0');
//
//					if (nanoseconds < 100) {
//						sb.append('0');
//
//						if (nanoseconds < 10) {
//							sb.append('0');
//						}
//					}
//				}
//			}
//		}

		sb.append(String.format("'%,03d", nanoseconds / 1000)); //$NON-NLS-1$

		return sb.toString();
	}

	/**
	 * @return Returns timestamp as string formated as specified.
	 */
	public String toString(final Format format) {
		final StringBuffer sb = new StringBuffer(32);
		switch (format.ordinal()) {
		case 0:
			formatDate.format(new Date(milliseconds), sb, new FieldPosition(DateFormat.FULL));
			return sb.toString();
		case 1:
			formatDateTime.format(new Date(milliseconds), sb, new FieldPosition(DateFormat.FULL));
			return sb.toString();
		case 2:
			formatDateTimeSeconds.format(new Date(milliseconds), sb, new FieldPosition(DateFormat.FULL));
			return sb.toString();
		case 4:
			formatLog.format(new Date(milliseconds), sb, new FieldPosition(DateFormat.FULL));
			return sb.toString();
		default:
			return toString();
		}
	}

}
