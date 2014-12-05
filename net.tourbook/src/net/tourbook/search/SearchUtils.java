/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;

import org.apache.derby.iapi.error.PublicAPI;
import org.apache.derby.iapi.error.StandardException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/*
 * Copied from org.apache.derby.optional.api.LuceneUtils and modified
 */

/**
 *
 */
public class SearchUtils {

	/**
	 * Map of Analyzers keyed by language code
	 */
	private static HashMap<String, Class<? extends Analyzer>>	_analyzerClasses;

	static {
		_analyzerClasses = new HashMap<String, Class<? extends Analyzer>>();

		storeAnalyzerClass(org.apache.lucene.analysis.ar.ArabicAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.hy.ArmenianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.eu.BasqueAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.br.BrazilianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.bg.BulgarianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.ca.CatalanAnalyzer.class);
		// deprecated, use StandardAnalyzer instead: storeAnalyzerClass( org.apache.lucene.analysis.cn.ChineseAnalyzer.class );
		storeAnalyzerClass(org.apache.lucene.analysis.cz.CzechAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.da.DanishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.nl.DutchAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.en.EnglishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.fi.FinnishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.fr.FrenchAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.gl.GalicianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.de.GermanAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.el.GreekAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.hi.HindiAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.hu.HungarianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.id.IndonesianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.ga.IrishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.it.ItalianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.lv.LatvianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.no.NorwegianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.fa.PersianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.pt.PortugueseAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.ro.RomanianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.ru.RussianAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.es.SpanishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.sv.SwedishAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.th.ThaiAnalyzer.class);
		storeAnalyzerClass(org.apache.lucene.analysis.tr.TurkishAnalyzer.class);
	}

	/**
	 * Enable Lucene tool.
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */

	/**
	 * Get the Analyzer associated with the given Locale.
	 */
	public static Analyzer getAnalyzerForLocale(final Locale locale) throws SQLException {
		final String language = locale.getLanguage();

		try {
			final Class<? extends Analyzer> analyzerClass = _analyzerClasses.get(language);

			if (analyzerClass == null) {

				return new StandardAnalyzer();

			} else {

				final Constructor<? extends Analyzer> constructor = analyzerClass.getConstructor();

				return constructor.newInstance();
			}
		} catch (final IllegalAccessException iae) {
			throw wrap(iae);
		} catch (final InstantiationException ie) {
			throw wrap(ie);
		} catch (final InvocationTargetException ite) {
			throw wrap(ite);
		} catch (final NoSuchMethodException nsme) {
			throw wrap(nsme);
		}
	}

	/**
	 * <p>
	 * Get the language code for a Lucene Analyzer. Each of the Analyzers lives in a package whose
	 * last leg is the language code.
	 * </p>
	 */
	private static String getLanguageCode(final Class<? extends Analyzer> analyzerClass) {
		final String className = analyzerClass.getName();
		final String packageName = className.substring(0, className.lastIndexOf("."));
		final String languageCode = packageName.substring(packageName.lastIndexOf(".") + 1, packageName.length());

		return languageCode;
	}

	/** Turn a StandardException into a SQLException */
	private static SQLException sqlException(final StandardException se) {

		return PublicAPI.wrapStandardException(se);
	}

	/** Store an Analyzer class in the HashMap of Analyzers, keyed by language code */
	private static void storeAnalyzerClass(final Class<? extends Analyzer> analyzerClass) {

		_analyzerClasses.put(getLanguageCode(analyzerClass), analyzerClass);
	}

	/** Wrap an external exception */
	private static SQLException wrap(final Throwable t) {

		return sqlException(StandardException.plainWrapException(t));
	}
}
