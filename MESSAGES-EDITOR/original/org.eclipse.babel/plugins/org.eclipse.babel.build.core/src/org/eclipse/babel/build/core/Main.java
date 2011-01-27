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

import org.eclipse.babel.build.core.Options.MissingArgument;
import org.eclipse.babel.build.core.Options.UnsetMandatoryOption;
import org.eclipse.babel.build.core.coverage.LanguagePackCoverageReport;
import org.eclipse.babel.build.core.exceptions.FailedDeletionException;
import org.eclipse.babel.build.core.exceptions.InvalidFilenameException;
import org.eclipse.babel.build.core.languagepack.LanguagePack;


public class Main {

	public static void main (String[] args) {
		try {
			CommandLineConfiguration config = new CommandLineConfiguration(args);
			
			long startLanguagePackGeneration = System.currentTimeMillis();
			LanguagePack languagePack = new LanguagePack(config);
			System.out.println();
			System.out.println(Messages.getString("Messages_generating_language_pack"));	//$NON-NLS-1$		
			
			LanguagePackCoverageReport coverage;
			coverage = languagePack.generate();
			Configuration.helper.printLanguagePackResult(config, coverage);
			
			config.times.languagePackGeneration = System.currentTimeMillis() - startLanguagePackGeneration;
			
			System.out.println("\nEclipse Target parsed in: " + config.times.eclipseInstallPopulation);	//$NON-NLS-1$
			System.out.println("Language Pack generated in: " + config.times.languagePackGeneration);	//$NON-NLS-1$
		
		} catch (MissingArgument e) {
			System.out.println(Messages.getString("Error_missing_argument") + e.getMessage());	//$NON-NLS-1$
			System.exit(-1);
		} catch (UnsetMandatoryOption e) {
			System.out.println(Messages.getString("Error_unset_mandatory_exception") + e.getMessage());	//$NON-NLS-1$
			System.exit(-1);
		} catch (InvalidFilenameException i) {
			System.out.println( Messages.getString("Error_invalid_working_directory_name") );	//$NON-NLS-1$
			System.exit(0);
		} catch (FailedDeletionException f) {
			System.out.println( Messages.getString("Error_deleting_working_directory") );	//$NON-NLS-1$
			System.exit(0);
		} catch (Exception e) {
			System.out.println( Messages.getString("Error_language_pack") + ": " + e.getMessage());	//$NON-NLS-1$	$NON-NLS-2$
			System.exit(0);
		}
	}
}
