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
package org.eclipse.babel.build.ant.test;

import junit.framework.TestCase;

import org.eclipse.babel.build.ant.BuildTask;

public class TestBuildTask extends TestCase{
	public void testBuildTask() throws Exception {
		BuildTask buildTask = new BuildTask();
		buildTask.setWorkingDirectory("/work/nls/w");
		buildTask.setEclipse("/work/nls/testing/eclipse-SDK-3.4M5-win32.zip");
		buildTask
				.setTranslations("/Users/aaron/Desktop/nls/IES_3.3/piiDocTransDir");
		buildTask.setLocales("gr1");
		buildTask.createCoverageReport().createIgnore().setPattern(
				"^META-INF/.*");
		buildTask.execute();
	}

	public void testBuildTask_missingEclipse() throws Exception {
		BuildTask buildTask = new BuildTask();
		buildTask.setWorkingDirectory("/work/nls/w");
		buildTask.setEclipse("/this/path/doesnt/exist");
		buildTask
				.setTranslations("/Users/aaron/Desktop/nls/IES_3.3/piiDocTransDir");
		buildTask.setLocales("gr1");
		buildTask.createCoverageReport().createIgnore().setPattern(
				"^META-INF/.*");
		buildTask.execute();
	}
}
