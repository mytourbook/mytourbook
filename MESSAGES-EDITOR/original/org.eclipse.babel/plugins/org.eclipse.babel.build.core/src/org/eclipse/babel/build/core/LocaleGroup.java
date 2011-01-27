/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.core;

public enum LocaleGroup {

	GROUP_ALL (Messages.getString("Groups_all"), Messages.getString("Groups_all_full"), 0), //$NON-NLS-1$
	GROUP_1 (Messages.getString("Groups_group1"), Messages.getString("Groups_group1_full"), 1), //$NON-NLS-1$
	GROUP_2 (Messages.getString("Groups_group2"), Messages.getString("Groups_group2_full"), 2), //$NON-NLS-1$
	GROUP_2A (Messages.getString("Groups_group2a"), Messages.getString("Groups_group2a_full"), 3), //$NON-NLS-1$
	GROUP_BIDI (Messages.getString("Groups_groupBidi"), Messages.getString("Groups_groupBidi_full"), 4); //$NON-NLS-1$	
	
	private String name;
	private String fullName;
	private int number;

	private LocaleGroup(String name, String fullName, int number) {
		this.name = name;
		this.fullName = fullName;
		this.number = number;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return fullName;
	}

	public int getNumber() {
		return number;
	}
	
	public static LocaleGroup get(String name) {
		for (LocaleGroup group : LocaleGroup.values()) {
			if (group.name.compareToIgnoreCase(name) == 0) {
				return group;
			}
		}
		return LocaleGroup.GROUP_ALL;
	}
	
	public static LocaleGroup getByFullName(String fullName) {
		for (LocaleGroup group : LocaleGroup.values()) {
			if (group.fullName.compareToIgnoreCase(fullName) == 0) {
				return group;
			}
		}
		return LocaleGroup.GROUP_ALL;
	}
	
	public static LocaleGroup get(Integer number) {
		for (LocaleGroup group : LocaleGroup.values()) {
			if (group.number == number) {
				return group;
			}
		}
		return LocaleGroup.GROUP_ALL;
	}
	
	public static boolean isValidGroupName(String name) {
		for (LocaleGroup group : LocaleGroup.values()) {
			if (group.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isValidGroupFullName(String fullName) {
		for (LocaleGroup group : LocaleGroup.values()) {
			if (group.fullName.compareToIgnoreCase(fullName) == 0) {
				return true;
			}
		}
		return false;
	}
}
