/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package net.tourbook.photo;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.dawb.common.util.list.SortNatural;

/**
 * @author fcp94556
 *
 */
public class SortingUtils {

	/**
	 * 
	 */
	public static final Comparator<File> NATURAL_SORT = new SortNatural<File>(true);
	
	/**
	 * 
	 */
	public static final Comparator<File> NATURAL_SORT_CASE_INSENSITIVE = new SortNatural<File>(false);
	
	/**
	 * 
	 */
	public static final Comparator<File> DATE_SORT = new Comparator<File>() {
		@Override
		public int compare(File one, File two) {
			final long diff = one.lastModified()-two.lastModified();
			if (diff==0) {
				return NATURAL_SORT.compare(one, two);
			}
			if (diff>0) return (int)two.lastModified();
			return -1;
		}
	};
	
	/**
	 * 
	 */
	public static final Comparator<File> DATE_SORT_BACKWARDS = new Comparator<File>() {
		@Override
		public int compare(File one, File two) {
			final long diff = two.lastModified()-one.lastModified();
			if (diff==0) {
				return NATURAL_SORT.compare(one, two);
			}
			if (diff>0) return (int)one.lastModified();
			return -1;
		}
	};


	public static final Comparator<File> DEFAULT_COMPARATOR = new Comparator<File>() {
			@Override
			public int compare(File one, File two) {
				return one.compareTo(two);
			}
		};

	/**
	 * @param dir
	 * @return List<File>
	 */
	public static List<File> getSortedFileList(final File dir) {
		return getSortedFileList(dir, DEFAULT_COMPARATOR);
	}
	
	/**
	 * @param dir
	 * @param comp
	 * @return  List<File>
	 */
	public static List<File> getSortedFileList(final File dir, final Comparator<File> comp) {  
	    return getSortedFileList(dir.listFiles(), comp);
	}
	
	/**
	 * @param dir
	 * @param fileFilter 
	 * @param comp
	 * @return  List<File>
	 */
	public static List<File> getSortedFileList(final File dir, FileFilter fileFilter, final Comparator<File> comp) {  
	    return getSortedFileList(dir.listFiles(fileFilter), comp);
	}
	
	/**
	 * @param dir
	 * @param fileFilter
	 * @return List<File> 
	 */
	public static List<File> getSortedFileList(File dir, FileFilter fileFilter) {
	    return getSortedFileList(dir.listFiles(fileFilter), DEFAULT_COMPARATOR);
	}

	/**
	 * @param comp
	 * @return  List<File>
	 */
	public static List<File> getSortedFileList(final File[] fa, final Comparator<File> comp) {
	    
	    if (fa == null || fa.length<1) return null;
	    
	    final List<File> files = new ArrayList<File>(fa.length);
	    files.addAll(Arrays.asList(fa));
	    Collections.sort(files, comp);

	    return files;
	}

	public static void removeIgnoredNames(Collection<String> sets, Collection<Pattern> patterns) {
		if (patterns==null) return;
		if (sets==null)     return;
		for (Iterator<String> it = sets.iterator(); it.hasNext();) {
			final String name = it.next();
			PATTERN_LOOP: for (Pattern pattern : patterns) {
				if (pattern.matcher(name).matches()) {
					it.remove();
					break PATTERN_LOOP;
				}
			}
		}
	}

}



	