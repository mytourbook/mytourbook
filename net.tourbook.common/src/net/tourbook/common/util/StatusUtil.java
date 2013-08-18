/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package net.tourbook.common.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.tourbook.common.CommonActivator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

/*
 * this class is copied from org.eclipse.ui.internal.misc.StatusUtil and modified
 */

/**
 * Utility class to create status objects.
 * 
 * @private - This class is an internal implementation class and should not be referenced or
 *          subclassed outside of the workbench
 */
public class StatusUtil {

	public static Throwable getCause(final Throwable exception) {

		// Figure out which exception should actually be logged -- if the given exception is
		// a wrapper, unwrap it
		Throwable cause = null;
		if (exception != null) {
			if (exception instanceof CoreException) {
				// Workaround: CoreException contains a cause, but does not actually implement getCause().
				// If we get a CoreException, we need to manually unpack the cause. Otherwise, use
				// the general-purpose mechanism. Remove this branch if CoreException ever implements
				// a correct getCause() method.
				final CoreException ce = (CoreException) exception;
				cause = ce.getStatus().getException();

			} else {

				// use reflect instead of a direct call to getCause(), to allow compilation against JCL Foundation (bug 80053)
				try {
					final Method causeMethod = exception.getClass().getMethod("getCause", new Class[0]); //$NON-NLS-1$
					final Object o = causeMethod.invoke(exception, new Object[0]);
					if (o instanceof Throwable) {
						cause = (Throwable) o;
					}
				} catch (final NoSuchMethodException e) {
					// ignore
				} catch (final IllegalArgumentException e) {
					// ignore
				} catch (final IllegalAccessException e) {
					// ignore
				} catch (final InvocationTargetException e) {
					// ignore
				}
			}

			if (cause == null) {
				cause = exception;
			}
		}

		return cause;
	}

	/**
	 * Utility method for handling status.
	 */
	public static void handleStatus(final String message, final int style) {
		handleStatus(message, null, style, IStatus.ERROR);
	}

	/**
	 * Utility method for handling status.
	 * 
	 * @param severity
	 */
	public static void handleStatus(final String message, final Throwable e, final int style, final int severity) {
		StatusManager.getManager().handle(
				newStatus(CommonActivator.PLUGIN_ID, message, e, severity),
				style | StatusManager.LOG);
	}

	/**
	 * Log message into the status log
	 * 
	 * @param message
	 * @param exception
	 */
	public static void log(final String message) {
		handleStatus(message, null, StatusManager.LOG, IStatus.ERROR);
	}

	/**
	 * Log exception into the status log
	 * 
	 * @param message
	 * @param exception
	 */
	public static void log(final String message, Throwable exception) {

		/*
		 * create an exception to see in the log the location where the logging occured
		 */
		if (exception == null) {
			exception = new Exception();
		}

		handleStatus(message, exception, StatusManager.LOG, IStatus.ERROR);
	}

	/**
	 * Log error into the log
	 * 
	 * @param exception
	 */
	public static void log(final Throwable exception) {
		handleStatus(exception.getMessage(), exception, StatusManager.LOG, IStatus.ERROR);
	}

	public static void logError(final String message) {
		handleStatus(message, null, StatusManager.LOG, IStatus.ERROR);
	}

	public static void logInfo(final String message) {
		handleStatus(message, null, StatusManager.LOG, IStatus.INFO);
	}

	public static void logInfo(final String message, Exception exception) {

		/*
		 * create an exception to see in the log the location where the logging occured
		 */
		if (exception == null) {
			exception = new Exception();
		}

		handleStatus(message, exception, StatusManager.LOG, IStatus.INFO);
	}

	/**
	 * Utility method for creating status.
	 */
	public static IStatus newStatus(final int severity, final String message, final Throwable exception) {

		String statusMessage = message;
		if (message == null || message.trim().length() == 0) {
			if (exception.getMessage() == null) {
				statusMessage = exception.toString();
			} else {
				statusMessage = exception.getMessage();
			}
		}

		return new Status(severity, CommonActivator.PLUGIN_ID, severity, statusMessage, getCause(exception));
	}

	private static IStatus newStatus(	final String pluginId,
										final String message,
										final Throwable exception,
										final int severity) {
		return new Status(severity, pluginId, IStatus.OK, message, getCause(exception));
	}

	public static void showStatus(final String message) {
		handleStatus(message, null, StatusManager.SHOW, IStatus.ERROR);
	}

	public static void showStatus(final String message, final Throwable exception) {
		handleStatus(message, exception, StatusManager.SHOW, IStatus.ERROR);
	}

	public static void showStatus(final Throwable exception) {
		handleStatus(exception.getMessage(), exception, StatusManager.SHOW, IStatus.ERROR);
	}

}
