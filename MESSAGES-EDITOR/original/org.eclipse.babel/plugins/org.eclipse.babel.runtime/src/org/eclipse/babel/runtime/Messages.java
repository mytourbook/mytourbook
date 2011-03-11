/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime;

import org.eclipse.babel.runtime.external.TranslatableNLS;
import org.eclipse.babel.runtime.external.ITranslatableText;

public class Messages extends TranslatableNLS {
	private static final String BUNDLE_NAME = "org.eclipse.babel.runtime.messages"; //$NON-NLS-1$

	static {
		// A little risky using the plugin activator in a static initializer.
		// Let's hope it is in a good enough state.
		initializeMessages(BUNDLE_NAME, Messages.class, Activator.getDefault().getBundle());
	}

	public static ITranslatableText LocalizeDialog_TabTitle_EditorPart;
	public static ITranslatableText LocalizeDialog_TabTitle_ViewPart;
	public static ITranslatableText LocalizeDialog_TabTitle_OtherPart;
	public static ITranslatableText LocalizeDialog_Title;
	public static ITranslatableText LocalizeDialog_TabTitle_Plugins;
	public static ITranslatableText LocalizeDialog_Title_DialogPart;
	public static ITranslatableText LocalizeDialog_TabTitle_Menu;
	public static ITranslatableText LocalizeDialog_TabTitle_PluginXml;
	public static ITranslatableText LocalizeDialog_Command_Translate;
	public static ITranslatableText LocalizeDialog_CommandLabel_Revert;
	public static ITranslatableText LocalizeDialog_CommandTooltip_Revert;
	public static ITranslatableText LocalizeDialog_TableTooltip_Plugin;
	public static ITranslatableText LocalizeDialog_TableTooltip_ResourceBundle;
	public static ITranslatableText LocalizeDialog_TableTooltip_Key;
	public static ITranslatableText LocalizeDialog_TabTitle_Dialog; 
	public static ITranslatableText LocalizeDialog_Title_PluginPart;
	
	public static ITranslatableText exception_failedDelete;
	public static ITranslatableText exception_loadException;
	public static ITranslatableText exception_saveException;

	public static ITranslatableText AboutPluginsDialog_provider;
	public static ITranslatableText AboutPluginsDialog_pluginName;
	public static ITranslatableText AboutPluginsDialog_version;
	public static ITranslatableText AboutPluginsDialog_pluginId;
}
