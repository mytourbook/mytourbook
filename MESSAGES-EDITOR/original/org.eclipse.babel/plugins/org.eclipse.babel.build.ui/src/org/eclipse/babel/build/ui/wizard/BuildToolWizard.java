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
package org.eclipse.babel.build.ui.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.babel.build.ui.Activator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;


public class BuildToolWizard extends Wizard 
	implements IExportWizard {

	private final static String SELECTED_LOCALES = "BuildToolWizardLocalePage#Locales";
	
	private BuildToolWizardPluginPage fPage1;
	private BuildToolWizardLocalePage fPage2;
	private BuildToolWizardConfigurationPage fPage3;
	
	public BuildToolWizard() {
		IDialogSettings master = Activator.getDefault().getDialogSettings();
		setDialogSettings(getSection(master));
		setWindowTitle(Messages.getString("Wizard_Title"));
	}
	
	private IDialogSettings getSection(IDialogSettings master){
		IDialogSettings settings = master.getSection("BuildToolWizard");
		if(settings == null){
			return master.addNewSection("BuildToolWizard");
		}
		return settings;
	}
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		BuildInitializationOperation runnable = new BuildInitializationOperation(selection, getDialogSettings().getArray(SELECTED_LOCALES));
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} 
		catch (InvocationTargetException e) {e.printStackTrace();} 
		catch (InterruptedException e) {e.printStackTrace();}
		finally {
			if (runnable.wasCanceled()) {
				return;
			}
			fPage1 = new BuildToolWizardPluginPage("BuildToolWizardPluginPage.Page1", runnable.getPluginsTable()); //$NON-NLS-1$
			fPage2 = new BuildToolWizardLocalePage("BuildToolWizardLocalePage.Page2", runnable.getLocalesTable()); //$NON-NLS-1$
			fPage3 = new BuildToolWizardConfigurationPage("BuildToolWizardConfigurationPage.Page3", runnable.getIgnoreTable(), runnable.getResourceExclusionTable()); //$NON-NLS-1$
		}
	}
	
	public void addPages() {
		addPage(fPage1);
		addPage(fPage2);
		addPage(fPage3);
	}
	
	private String getGroupName(String nameWithDescription) {
		if(nameWithDescription.startsWith("Group1")) {
			return "Group1";
		}
		else if(nameWithDescription.startsWith("Group2a")) {
			return "Group2a";
		}
		else if(nameWithDescription.startsWith("Group2")) {
			return "Group2";
		}
		else if(nameWithDescription.startsWith("GroupBidi")) {
			return "GroupBidi";
		}
		else {
			return "All";
		}
		
	}
	
    public boolean performFinish() { 
    	fPage1.storeSettings();
    	fPage2.storeSettings();
    	fPage3.storeSettings();
    	
        Object[] selectedPlugins;
        String eclipseArchivePath;
        
        String[] selectedLocales;
        
        String workingDirectoryLocation;
        String translationCataloguelocation;
        Object[] ignoreList;
        Object[] reportFilterPatterns;
        boolean includeXmlReport;
        boolean longReport;
        
        if(fPage1.isGeneratingFromPlugins()) {
            selectedPlugins = fPage1.getSelectedPlugins();
            eclipseArchivePath = "";
        }
        else {
            eclipseArchivePath = fPage1.getEclipseArchiveLocation();
            selectedPlugins = null;
        }
        
        if(fPage2.isGeneratingFromLocales()) {
        	if(fPage2.isGeneratingForAllLocales()) {
        		selectedLocales = new String[0];
        	}
        	else {
	            Object[] locales = fPage2.getSelectedLocales();
	            selectedLocales = new String[locales.length];
	            for(int i=0; i<locales.length; i++) {
	                selectedLocales[i] = locales[i].toString();
	            }
        	}
        }
        else {
            selectedLocales = new String[]{getGroupName(fPage2.getSelectedGroup())};
        }
        
        workingDirectoryLocation = fPage3.getWorkingDirectoryLocation();
        translationCataloguelocation = fPage3.getTranslationCatalogueLocation();
        ignoreList = fPage3.getIgnoreList();
        reportFilterPatterns = fPage3.getReportFilterPatterns();
        includeXmlReport = fPage3.getXmlReportSelection();
        longReport = fPage3.getLongReportSelection();
        
        final LanguagePackGenerationHandler handler;
        
        if(selectedPlugins == null) {
            handler = new LanguagePackGenerationHandler(eclipseArchivePath, 
                    selectedLocales,
                    workingDirectoryLocation,
                    translationCataloguelocation,
                    ignoreList,
                    reportFilterPatterns,
					includeXmlReport,
					longReport);
        }
        else {
            handler = new LanguagePackGenerationHandler(selectedPlugins, 
                    selectedLocales,
                    workingDirectoryLocation,
                    translationCataloguelocation,
                    ignoreList,
                    reportFilterPatterns,
					includeXmlReport,
					longReport);
        }
        
        Job languagePackGenerationJob = new Job("Generating Language Pack") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String errorMessage = handler.generateLanguagePack(monitor);
                if (errorMessage.equals("")) {
                    return new Status(Status.OK, "Generating Language Pack", "Language pack successfully generated");
                }
                return new Status(Status.ERROR, "Generating Language Pack", errorMessage);
            }
        };

        languagePackGenerationJob.setPriority(Job.BUILD);
        languagePackGenerationJob.schedule();
        
        PlatformUI.getWorkbench().getProgressService().showInDialog(new Shell(), languagePackGenerationJob);
        return true;
    }

	@Override
	public boolean canFinish() {
		return fPage1.canFlipToNextPage() && fPage2.canFlipToNextPage() && fPage3.isPageComplete();
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage currentPage) {
		if (currentPage.equals(fPage3)) {
			return fPage2;
		} else if (currentPage.equals(fPage2)) {
			return fPage1;
		}
		return null;
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
		if (currentPage.equals(fPage1)) {
			return fPage2;
		} else if (currentPage.equals(fPage2)) {
			return fPage3;
		} 
		return null;
	}
}
