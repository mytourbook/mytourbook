/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Class monitoring a {@link File} for changes.
 * @author Pascal Essiembre
 */
public class FileMonitor {

    private static final FileMonitor instance = new FileMonitor();

    private Timer timer;
    private Hashtable<String,FileMonitorTask> timerEntries;

    /**
     * Gets the file monitor instance.
     * @return file monitor instance
     */
    public static FileMonitor getInstance() {
        return instance;
    }

    /**
     * Constructor.
     */
    private FileMonitor() { 
        // Create timer, run timer thread as daemon.
    	timer = new Timer(true);
    	timerEntries = new Hashtable<String,FileMonitorTask>();
    }
    
    /**
     * Adds a monitored file with a {@link FileChangeListener}.
     * @param listener listener to notify when the file changed.
     * @param fileName name of the file to monitor.
     * @param period polling period in milliseconds.
     */
    public void addFileChangeListener(
    		FileChangeListener listener, String fileName, long period) 
			throws FileNotFoundException {
        addFileChangeListener(listener, new File(fileName), period);
    }

    /**
     * Adds a monitored file with a FileChangeListener.
     * @param listener listener to notify when the file changed.
     * @param fileName name of the file to monitor.
     * @param period polling period in milliseconds.
     */
    public void addFileChangeListener(
    		FileChangeListener listener, File file, long period) 
            throws FileNotFoundException {
	    removeFileChangeListener(listener, file);
	    FileMonitorTask task = new FileMonitorTask(listener, file);
	    timerEntries.put(file.toString() + listener.hashCode(), task);
	    timer.schedule(task, period, period);
    }
    
    /**
     * Remove the listener from the notification list.
     * @param listener the listener to be removed.
     */
    public void removeFileChangeListener(FileChangeListener listener, 
    					 String fileName) {
        removeFileChangeListener(listener, new File(fileName));
    }

    /**
     * Remove the listener from the notification list.
     * @param listener the listener to be removed.
     */
    public void removeFileChangeListener(
    		FileChangeListener listener, File file) {
        FileMonitorTask task = timerEntries.remove(
                file.toString() + listener.hashCode());
	    if (task != null) {
	        task.cancel();
	    }
    }

    /**
     * Fires notification that a file changed.
     * @param listener file change listener
     * @param file the file that changed
     */
    protected void fireFileChangeEvent(
    		FileChangeListener listener, File file) {
    	listener.fileChanged(file);
    }

    /**
     * File monitoring task.
     */
    class FileMonitorTask extends TimerTask {
        FileChangeListener listener;
        File monitoredFile;
        long lastModified;

        public FileMonitorTask(FileChangeListener listener, File file) 
				throws FileNotFoundException {
		    this.listener = listener;
		    this.lastModified = 0;
		    monitoredFile = file;
		    if (!monitoredFile.exists()) {  // but is it on CLASSPATH?
		        URL fileURL = 
			    listener.getClass().getClassLoader().getResource(
			    		file.toString());
				if (fileURL != null) {
				    monitoredFile = new File(fileURL.getFile());
				} else {
				    throw new FileNotFoundException("File Not Found: " + file);
				}
		    }
		    this.lastModified = monitoredFile.lastModified();
		}
	
        public void run() {
		    long lastModified = monitoredFile.lastModified();
		    if (lastModified != this.lastModified) {
		        this.lastModified = lastModified;
		        fireFileChangeEvent(this.listener, monitoredFile);
		    }
		}
    }
}
