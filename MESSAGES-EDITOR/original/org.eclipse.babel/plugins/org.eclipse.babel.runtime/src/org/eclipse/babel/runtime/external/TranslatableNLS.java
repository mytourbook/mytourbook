/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation of NLS class
 *     Nigel Westbury - Modifications to NLS class to support dynamic messages
 *******************************************************************************/

package org.eclipse.babel.runtime.external;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.Bundle;

/**
 * Common superclass for all message bundle classes.  Provides convenience
 * methods for manipulating messages.
 * <p>
 * The <code>#bind</code> methods perform string substitution and should be considered a
 * convenience and <em>not</em> a full substitute replacement for <code>MessageFormat#format</code>
 * method calls. 
 * </p>
 * <p>
 * Text appearing within curly braces in the given message, will be interpreted
 * as a numeric index to the corresponding substitution object in the given array. Calling
 * the <code>#bind</code> methods with text that does not map to an integer will result in an
 * {@link IllegalArgumentException}.
 * </p>
 * <p>
 * Text appearing within single quotes is treated as a literal. A single quote is escaped by
 * a preceeding single quote.
 * </p>
 * <p>
 * Clients who wish to use the full substitution power of the <code>MessageFormat</code> class should
 * call that class directly and not use these <code>#bind</code> methods.
 * </p>
 * <p>
 * Clients may subclass this type.
 * </p>
 * 
 * @since 3.1
 */
public abstract class TranslatableNLS {

	private static final Object[] EMPTY_ARGS = new Object[0];
	private static final String EXTENSION = ".properties"; //$NON-NLS-1$
	private static String[] nlSuffixes;
	/*
	 * NOTE do not change the name of this field; it is set by the Framework using reflection
	 */
	private static FrameworkLog frameworkLog;

	static final int SEVERITY_ERROR = 0x04;
	static final int SEVERITY_WARNING = 0x02;
	/*
	 * This object is assigned to the value of a field map to indicate
	 * that a translated message has already been assigned to that field.
	 */
	static final Object ASSIGNED = new Object();

	/**
	 * Creates a new NLS instance.
	 */
	protected TranslatableNLS() {
		super();
	}

	/**
	 * Bind the given message's substitution locations with the given string value.
	 * 
	 * @param message the message to be manipulated
	 * @param binding the object to be inserted into the message
	 * @return the manipulated String
	 */
	public static ITranslatableText bind(ITranslatableText message, Object binding) {
		return internalBind(message, convert(binding));
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 * 
	 * @param message the message to be manipulated
	 * @param binding1 An object to be inserted into the message
	 * @param binding2 A second object to be inserted into the message
	 * @return the manipulated String
	 */
	public static ITranslatableText bind(ITranslatableText message, Object binding1, Object binding2) {
		return internalBind(message, convert(binding1), convert(binding2));
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 * 
	 * @param message the message to be manipulated
	 * @param bindings An array of objects to be inserted into the message
	 * @return the manipulated String
	 */
	public static ITranslatableText bind(ITranslatableText message, Object[] bindings) {
		return internalBind(message, bindings);
	}

	private static ITranslatableText convert(Object binding) {
		return (binding instanceof ITranslatableText)
		? (ITranslatableText)binding
				: new NonTranslatableText(String.valueOf(binding));
	}

	/**
	 * Initialize the given class with the values from the specified message bundle.
	 * 
	 * @param bundleName fully qualified path of the class name
	 * @param clazz the class where the constants will exist
	 */
	public static void initializeMessages(final String bundleName, final Class clazz, final Bundle osgiBundle) {
		if (System.getSecurityManager() == null) {
			load(bundleName, clazz, osgiBundle);
			return;
		}
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				load(bundleName, clazz, osgiBundle);
				return null;
			}
		});
	}

	/*
	 * Perform the string substitution on the given message with the specified args.
	 * See the class comment for exact details.
	 */
	private static ITranslatableText internalBind(ITranslatableText message, Object... args) {
		if (message == null)
			return new NonTranslatableText("No message available."); //$NON-NLS-1$
		if (args == null || args.length == 0)
			args = EMPTY_ARGS;

		ITranslatableText [] passOnArgs = new ITranslatableText[1 + args.length];
		passOnArgs[0] = message;
		for (int i = 0; i < args.length; i++) {
			passOnArgs[i+1] = convert(args[i]);
		}
		return new FormattedTranslatableText(passOnArgs);
	}

	/*
	 * Build an array of property files to search.  The returned array contains
	 * the property fields in order from most specific to most generic.
	 * So, in the FR_fr locale, it will return file_fr_FR.properties, then
	 * file_fr.properties, and finally file.properties.
	 */
	private static String[] buildVariants(String root) {
		if (nlSuffixes == null) {
			//build list of suffixes for loading resource bundles
			String nl = Locale.getDefault().toString();
			ArrayList<String> result = new ArrayList<String>(4);
			int lastSeparator;
			while (true) {
				result.add('_' + nl + EXTENSION);
				lastSeparator = nl.lastIndexOf('_');
				if (lastSeparator == -1)
					break;
				nl = nl.substring(0, lastSeparator);
			}
			//add the empty suffix last (most general)
			result.add(EXTENSION);
			nlSuffixes = result.toArray(new String[result.size()]);
		}
		root = root.replace('.', '/');
		String[] variants = new String[nlSuffixes.length];
		for (int i = 0; i < variants.length; i++)
			variants[i] = root + nlSuffixes[i];
		return variants;
	}

	private static void computeMissingMessages(String bundleName, Class clazz, Map<String, Object> fieldMap, Field[] fieldArray, boolean isAccessible) {
		// iterate over the fields in the class to make sure that there aren't any empty ones
		final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
		final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
		final int numFields = fieldArray.length;
		for (int i = 0; i < numFields; i++) {
			Field field = fieldArray[i];
			if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
				continue;
			//if the field has a a value assigned, there is nothing to do
			if (fieldMap.get(field.getName()) == ASSIGNED)
				continue;
			try {
				// Set a value for this empty field. We should never get an exception here because
				// we know we have a public static non-final field. If we do get an exception, silently
				// log it and continue. This means that the field will (most likely) be un-initialized and
				// will fail later in the code and if so then we will see both the NPE and this error.
				String value = "NLS missing message: " + field.getName() + " in: " + bundleName; //$NON-NLS-1$ //$NON-NLS-2$
				if (Debug.DEBUG_MESSAGE_BUNDLES)
					System.out.println(value);
				log(SEVERITY_WARNING, value, null);
				if (!isAccessible)
					field.setAccessible(true);
				field.set(null, value);
			} catch (Exception e) {
				log(SEVERITY_ERROR, "Error setting the missing message value for: " + field.getName(), e); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Load the given resource bundle using the specified class loader.
	 */
	static void load(final String bundleName, Class clazz, Bundle osgiBundle) {
		long start = System.currentTimeMillis();
		final Field[] fieldArray = clazz.getDeclaredFields();
		ClassLoader loader = clazz.getClassLoader();

		boolean isAccessible = (clazz.getModifiers() & Modifier.PUBLIC) != 0;

		//build a map of field names to Field objects
		final int len = fieldArray.length;
		Map<String, Object> fields = new HashMap<String, Object>(len * 2);
		for (int i = 0; i < len; i++)
			fields.put(fieldArray[i].getName(), fieldArray[i]);

		// search the variants from most specific to most general, since
		// the MessagesProperties.put method will mark assigned fields
		// to prevent them from being assigned twice
		final String[] variants = buildVariants(bundleName);
		
		// We are only interested in the keys at this time, and as keys cannot
		// be added in specific files, we read the keys from the most general
		// variant.
		String generalVariant = variants[variants.length-1];
		
//		for (int i = 0; i < variants.length; i++) {
//			final MessagesProperties properties = new MessagesProperties(fields, bundleName, isAccessible);
//			TranslatableResourceFile variantProperties = TranslatableResourceFile.get(osgiBundle, loader, variants[i]);
//			for (Object key: variantProperties.getKeys()) {
//				Object value = variantProperties.getValue((String)key);
//				properties.put(key, value);
//			}
//		}
		
		TranslatableResourceBundle resourceBundle = TranslatableResourceBundle.get(osgiBundle, loader, bundleName);
		
		final MessagesProperties properties = new MessagesProperties(fields, bundleName, isAccessible);
		for (Enumeration<String> enumerator = resourceBundle.getKeys(); enumerator.hasMoreElements(); ) {
			String key = enumerator.nextElement();
			ITranslatableText value = new TranslatableText(resourceBundle, key);
			properties.put(key, value);
		}

		computeMissingMessages(bundleName, clazz, fields, fieldArray, isAccessible);
		if (Debug.DEBUG_MESSAGE_BUNDLES)
			System.out.println("Time to load message bundle: " + bundleName + " was " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/*
	 * The method adds a log entry based on the error message and exception. 
	 * The output is written to the System.err.
	 * 
	 * This method is only expected to be called if there is a problem in
	 * the NLS mechanism. As a result, translation facility is not available 
	 * here and messages coming out of this log are generally not translated.
	 * 
	 * @param severity - severity of the message (SEVERITY_ERROR or SEVERITY_WARNING)
	 * @param message - message to log
	 * @param e - exception to log
	 */
	static void log(int severity, String message, Exception e) {
		if (frameworkLog != null) {
			frameworkLog.log(new FrameworkLogEntry("org.eclipse.osgi", severity, 1, message, 0, e, null)); //$NON-NLS-1$
			return;
		}
		String statusMsg;
		switch (severity) {
			case SEVERITY_ERROR :
				statusMsg = "Error: "; //$NON-NLS-1$
				break;
			case SEVERITY_WARNING :
			// intentionally fall through:
			default :
				statusMsg = "Warning: "; //$NON-NLS-1$
		}
		if (message != null)
			statusMsg += message;
		if (e != null)
			statusMsg += ": " + e.getMessage(); //$NON-NLS-1$
		System.err.println(statusMsg);
		if (e != null)
			e.printStackTrace();
	}

	/*
	 * Class which sub-classes java.util.Properties and uses the #put method
	 * to set field values rather than storing the values in the table.
	 */
	private static class MessagesProperties extends Properties {

		private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
		private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
		private static final long serialVersionUID = 1L;

		private final String bundleName;
		private final Map<String, Object> fields;
		private final boolean isAccessible;

		public MessagesProperties(Map<String, Object> fieldMap, String bundleName, boolean isAccessible) {
			super();
			this.fields = fieldMap;
			this.bundleName = bundleName;
			this.isAccessible = isAccessible;
		}

		/* (non-Javadoc)
		 * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
		 */
		public synchronized Object put(String key, Object value) {
			Object fieldObject = fields.put(key, ASSIGNED);
			// if already assigned, there is nothing to do
			if (fieldObject == ASSIGNED)
				return null;
			if (fieldObject == null) {
				final String msg = "NLS unused message: " + key + " in: " + bundleName;//$NON-NLS-1$ //$NON-NLS-2$
				if (Debug.DEBUG_MESSAGE_BUNDLES)
					System.out.println(msg); 
				log(SEVERITY_WARNING, msg, null);
				return null;
			}
			final Field field = (Field) fieldObject;
			//can only set value of public static non-final fields
			if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
				return null;
			try {
				// Check to see if we are allowed to modify the field. If we aren't (for instance 
				// if the class is not public) then change the accessible attribute of the field
				// before trying to set the value.
				if (!isAccessible)
					field.setAccessible(true);
				// Set the value into the field. We should never get an exception here because
				// we know we have a public static non-final field. If we do get an exception, silently
				// log it and continue. This means that the field will (most likely) be un-initialized and
				// will fail later in the code and if so then we will see both the NPE and this error.
				field.set(null, value);
			} catch (Exception e) {
				log(SEVERITY_ERROR, "Exception setting field value.", e); //$NON-NLS-1$
			}
			return null;
		}
	}
}