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
package org.eclipse.babel.build.core.xml;

import static org.eclipse.babel.build.core.xml.Builder.*;

/**
 * Implements grammar specific builder elements to aid in the construction of coverage reports.
 * For example:
 * 
 * <pre>
 * Document report = root("coverage",
 * 	element("locales",
 * 		locale("en"),
 * 		locale("zh_TW"),
 * 		...
 * 	)
 * );
 * 
 * report.render(new XmlWriter(System.out));
 * </pre>
 */
public class Coverage {
	
	public static Element coverage(String timestamp, Element... children){
		return root("coverage", children).attribute("timestamp", timestamp);
	}
	
	public static Element archive(String location){
		return element("archive").attribute("location", location);
	}
	
	public static Element translations(String location){
		return element("translations").attribute("location", location);
	}
	
	public static Element output(String location){
		return element("output").attribute("location", location);
	}
	
	/**
	 * Creates a coverage report resource element.
	 * 
	 * @param location The location of the resource relative to the root of the eclipse archive.
	 * @param locales The locales into which the resource was localized.
	 * @return The resource element.
	 */
	public static Element resource(String location, Element... locales){
		return resource(location, false, locales);
	}
	
	/**
	 * Creates a coverage report resource element.
	 * 
	 * @param location The location of the resource element relative to the root of the eclipse archive.
	 * @param excluded Whether or not the resource was excluded from the coverage report.
	 * @param locales The locales into which the resource was localized.
	 * @return The resource element.
	 */
	public static Element resource(String location, boolean excluded, Element... locales){
		return element("resource", locales).attribute("location", location
				).attribute("excluded", (excluded ? "true" : "false"));
	}
	
	/**
	 * Creates a coverage report locale element, representing a target locale. 
	 * 
	 * @param name The name of the locale.
	 * @param coverage The percentage of the resources that were translated into this locale.
	 * @return The locale element.
	 */
	public static Element locale(String name, int coverage){
		return locale(name).attribute("coverage", "" + coverage);
	}
	
	/**
	 * Creates a coverage report locale element.
	 * 
	 * @param name The name of the locale.
	 * @return The locale element.
	 */
	public static Element locale(String name){
		return element("locale").attribute("name", name); 
	}
	
	public static Element locales(Element... children){
		return element("locales", children);
	}
	
	/** 
	 * Creates a coverage report plugin element.
	 * 
	 * @param name The name of the plugin (eg. "org.eclipse.core")
	 * @param version The version of the plugin that was translated (eg. 3.5.0)
	 * @param resources The resources in the plugin that were translated.
	 * @return The pluin element.
	 */
	public static Element plugin(String name, String version, Element... resources){
		return element("plugin", resources).attribute("name", name).attribute("version", version);		
	}
	
	public static Element plugins(Element... plugins){
		return element("plugins", plugins);
	}
}
