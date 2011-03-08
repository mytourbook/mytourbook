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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.babel.runtime.actions.LocalizableMenuSet;
import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.babel.runtime.external.ITranslatableText;
import org.eclipse.babel.runtime.external.NonTranslatableText;
import org.eclipse.babel.runtime.pluginXmlParsing.LocalizableContribution;
import org.eclipse.babel.runtime.pluginXmlParsing.PluginXmlRegistry;
import org.eclipse.core.internal.registry.ConfigurationElementHandle;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.action.SubMenuManager;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IIdentifier;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.internal.ShowViewMenu;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.intro.IIntroConstants;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

public class MenuAnalyzer {

	private LocalizableMenuSet textSet = new LocalizableMenuSet();

	public MenuAnalyzer() {
		
	}
	
	public TranslatableMenuItem createTranslatableMenu() {
		WorkbenchWindow window = (WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		MenuManager menuManager = window.getMenuBarManager();
		
		
		return createTranslatableMenu(menuManager, null);
	}

	private TranslatableMenuItem createTranslatableMenu(IContributionItem item, ITranslatableText parentLocalizableText) {
		TranslatableMenuItem translatableMenuItem = new TranslatableMenuItem(parentLocalizableText); 

		if (item == null) {
			return translatableMenuItem;
		}
		
		if (item instanceof MenuManager) {
			MenuManager menuManager = (MenuManager)item;

//			System.out.print("\n children of " + item.toString() + "are:\n\n");
//			for (IContributionItem contributionItem: menuManager.getItems()) {
//				System.out.println(contributionItem.toString());
//			}
			
			for (IContributionItem contributionItem: menuManager.getItems()) {

				
				if (!contributionItem.isVisible()) {
					continue;
				}

				if (contributionItem.isSeparator()) {
					continue;
				}

				if (contributionItem instanceof SubContributionItem) {
					contributionItem = ((SubContributionItem) contributionItem).getInnerItem();
				}

				if (contributionItem instanceof ShowViewMenu) {
					addShowViewChildren(translatableMenuItem);
				} else {
					/*
					 * Fetch the original localization text for this item.
					 */
					ITranslatableText localizableText = null;

					if (contributionItem instanceof MenuManager) {
						MenuManager subMenuManager = (MenuManager)contributionItem;
						String id = subMenuManager.getId();

						localizableText = extractFromPluginXml("org.eclipse.ui.commands", "category", id, "name", true);
						if (localizableText == null) {
							localizableText = extractFromPluginXml("org.eclipse.ui.commands", "command", id, "name", true);
						}
						if (localizableText == null) {
							localizableText = extractFromPluginXml("org.eclipse.ui.menus", "menuContribution", "menu", id, "label", false);
						}
						if (localizableText == null) {
							localizableText = extractFromPluginXml("org.eclipse.ui.actionSets", "actionSet", "menu", id, "label", false);
						}
						if (localizableText == null) {
							localizableText = new NonTranslatableText(subMenuManager.getMenuText());
						}
					} else if (contributionItem instanceof ActionContributionItem) {
						ActionContributionItem pluginAction = (ActionContributionItem)contributionItem;
						String localId = pluginAction.getId();
						
						// Something weird happens here.
						// A null localId occurs here when running the plug-in in normal deployed
						// mode but is never null when running the plug-in by starting a new JVM
						// from within the IDE.
						if (localId != null) { // needed???
						// Does this line find anything????
						localizableText = extractFromPluginXml("org.eclipse.ui.actionSets", "actionSet", "action", localId, "label", false);
						if (localizableText != null) {
							textSet.associate(pluginAction, localizableText);
						} else {
							String fullId = pluginAction.getAction().getActionDefinitionId();
							localizableText = extractFromPluginXml("org.eclipse.ui.commands", "command", fullId, "name", false);
							if (localizableText != null) {
								textSet.associate(pluginAction, localizableText);
							} else {
								localizableText = new NonTranslatableText(pluginAction.getAction().getText());
							}
						}
						} // needed???
					} else if (contributionItem instanceof CommandContributionItem) {
						CommandContributionItem pluginAction = (CommandContributionItem)contributionItem;
						String id = pluginAction.getId();
//						for (IConfigurationElement element: RegistryFactory.getRegistry().getConfigurationElementsFor("org.eclipse.ui.commands")) {
//							if (element instanceof ConfigurationElementHandle) {
//								System.out.println(element.toString());
//							}
//						}
						
						Bundle bundle = null;
						String commandId = null;

						outer1: for (IConfigurationElement element: RegistryFactory.getRegistry().getConfigurationElementsFor("org.eclipse.ui.menus")) {
							if (element instanceof ConfigurationElementHandle) {
//								System.out.println(element.toString());
								
								for (IConfigurationElement element2: element.getChildren()) {
									if (element2 instanceof ConfigurationElementHandle) {
										if (element2.toString().equals(id)) {
											commandId = element2.getAttribute("commandId");
											String contributorName = element2.getDeclaringExtension().getContributor().getName();
											bundle = InternalPlatform.getDefault().getBundle(contributorName);
											break outer1;
										}
									}
								}
							}
						}
						
						if (commandId == null) {
						
						outer: for (IConfigurationElement element: Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.menus")) {
							if (element.getName().equals("menuContribution")) {
								for (IConfigurationElement subElement1: element.getChildren("menu")) {
									for (IConfigurationElement subElement: subElement1.getChildren("command")) {
										if (subElement.getAttribute("id").equals(id)) {
											String contributorName = element.getDeclaringExtension().getContributor().getName();
											bundle = InternalPlatform.getDefault().getBundle(contributorName);

											try {
												LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
												commandId = contribution.getValue("org.eclipse.ui.menus", "menuContribution", "menu", "command", id, "commandId");
											} catch (ParserConfigurationException e1) {
												e1.printStackTrace();
												commandId = null;
											} catch (SAXException e1) {
												e1.printStackTrace();
												commandId = null;
											} catch (IOException e1) {
												e1.printStackTrace();
												commandId = null;
											}

											break outer;
										}
									}
								}
							}
						}
						}
						
						if (commandId != null) {

							try {

								// repeat whole process with the command id
								LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
								localizableText = contribution.getLocalizableText("org.eclipse.ui.commands", "command", commandId, "name");

								textSet.associate(pluginAction, localizableText);
							} catch (ParserConfigurationException e1) {
								e1.printStackTrace();
								localizableText = new NonTranslatableText(pluginAction.getId());
							} catch (SAXException e1) {
								e1.printStackTrace();
								localizableText = new NonTranslatableText(pluginAction.getId());
							} catch (IOException e1) {
								e1.printStackTrace();
								localizableText = new NonTranslatableText(pluginAction.getId());
							}
						} else {
							localizableText = new NonTranslatableText(pluginAction.getId());
						}

//					} else if (contributionItem instanceof NewWizardMenu) {
					} else if (contributionItem instanceof SubMenuManager) {
						
						SubMenuManager pluginAction = (SubMenuManager)contributionItem;
// TODO: Sort this out.  Problem occurs because of bad use of action sets in
// the copier plug-in.  Generally these should be completely ignored, so set
// the localized text object to null which will result in nothing being added.						
						localizableText = null;	
/*
						
						//						String id = pluginAction.getId();

						for (IContributionItem subItem: pluginAction.getItems()) {
							if (subItem instanceof SubContributionItem) {
								subItem = ((SubContributionItem) subItem).getInnerItem();
							}

							if (!(subItem instanceof GroupMarker)
									&& !(subItem instanceof Separator)) {
							String id = subItem.getId();
							localizableText = extractFromPluginXml("org.eclipse.ui.actionSets", "actionSet", "action", id, "label", true);
							if (localizableText != null) {
//								textSet.associate(subItem, localizableText);
							} else {
								localizableText = extractFromPluginXml("org.eclipse.ui.commands", "command", id, "name", true);
								if (localizableText != null) {
//									textSet.associate(pluginAction, localizableText);
								} else {
									localizableText = new NonTranslatableText(subItem.toString());
								}
							}

							TranslatableMenuItem childItem = createTranslatableMenu(subItem, localizableText);
							translatableMenuItem.add(childItem);
							}
						}
*/							
						
					} else {
						localizableText = new NonTranslatableText(contributionItem.toString());
					}
				
					if (localizableText != null) {
//						System.out.print("\n " + contributionItem.toString() + " resulted in " + localizableText.getLocalizedText(Locale.getDefault()) + "\n");
						TranslatableMenuItem childItem = createTranslatableMenu(contributionItem, localizableText);
						translatableMenuItem.add(childItem);
					}
				}
			}
		}
		
		return translatableMenuItem;
	}
	
	/**
	 * @param children
	 */
	private void addShowViewChildren(TranslatableMenuItem parentItem) {
		// If no page disable all.
		IWorkbenchPage page = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return;
		}

		// If no active perspective disable all
		if (page.getPerspective() == null) {
			return;
		}

		// Get visible actions.
		List<String> viewIds = Arrays.asList(page.getShowViewShortcuts());

		// add all open views
		viewIds = addOpenedViews(page, viewIds);

		IViewRegistry reg = WorkbenchPlugin.getDefault().getViewRegistry();

		List<IViewDescriptor> actions = new ArrayList<IViewDescriptor>(viewIds.size());
		for (String id : viewIds) {
			if (id.equals(IIntroConstants.INTRO_VIEW_ID)) {
				continue;
			}

			// TODO: check activity

			IViewDescriptor desc = reg.find(id);
			if (desc != null) {
				IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI
				.getWorkbench().getActivitySupport();

				String pluginId =
					desc instanceof IPluginContribution 
					? ((IPluginContribution) desc).getPluginId()
							: null;

					String localId = desc.getId();
					String unifiedId =  (pluginId != null) 
					? pluginId + '/' + localId 
							: localId;

					IIdentifier identifier = workbenchActivitySupport
					.getActivityManager().getIdentifier(
							unifiedId);
					if (!identifier.isEnabled()) {
						continue;
					}

					actions.add(desc);
			}

		}

		Comparator<IViewDescriptor> actionComparator = new Comparator<IViewDescriptor>() {
			public int compare(IViewDescriptor view1, IViewDescriptor view2) {
				// Ideally we should be using the ICU collator for this comparison.
				// However, this is really not important enough to require everyone
				// to include such a large plug-in.
				return view1.getLabel().compareToIgnoreCase(view2.getLabel());
			}
		};


		Collections.sort(actions, actionComparator);

		for (IViewDescriptor action: actions) {
			ITranslatableText localizableText = extractFromPluginXml("org.eclipse.ui.views", "view", action.getId(), "name", false);
			parentItem.add(new TranslatableMenuItem(localizableText));
		}

//						// Add Other ..

//						// Build resource bundle for WorkbenchMessages.
//						TranslatableResourceBundle resourceBundle = (TranslatableResourceBundle)ResourceBundle.getBundle(
//						"org.eclipse.ui.internal.messages", // WorkbenchMessages.BUNDLE_NAME //$NON-NLS-1$
//						Locale.getDefault(),
//						org.eclipse.ui.internal.WorkbenchPlugin.class.getClassLoader(),
//						new UpdatableResourceControl(org.eclipse.ui.internal.WorkbenchPlugin.getDefault().getStateLocation()));
//						TranslatableResourceBundle.register(resourceBundle, org.eclipse.ui.internal.WorkbenchPlugin.getDefault().getBundle());


//						ITranslatableText localizableText = new TranslatableText(resourceBundle, "ShowView_title");

//						TranslatableTextInput textInput = new TranslatableTextInput(localizableText) {
//						@Override
//						public void updateControl(String text) {
//						WorkbenchMessages.ShowView_title = text;
//						// The menu title should update????
//						}
//						};
	}

	// Copied from ShowViewMenu
	private List<String> addOpenedViews(IWorkbenchPage page, List<String> actions) {

		
		
//		ArrayList views = getParts(page);
		ArrayList<String> views = new ArrayList<String>();  // Temp?

		ArrayList<String> result = new ArrayList<String>(views.size() + actions.size());

		for (int i = 0; i < actions.size(); i++) {
			String element = actions.get(i);
			if (result.indexOf(element) < 0) {
				result.add(element);
			}
		}
		for (int i = 0; i < views.size(); i++) {
			String element = views.get(i);
			if (result.indexOf(element) < 0) {
				result.add(element);
			}
		}
		return result;
	}

	private ITranslatableText extractFromPluginXml(String extensionPointId, String elementName, String subElementName, String id, String labelAttributeName, boolean localMatchOnly) {
		for (IConfigurationElement element: Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if (element.getName().equals(elementName)) {
				for (IConfigurationElement subElement: element.getChildren(subElementName)) {
					String thisId = subElement.getAttribute("id");
					// FIXME: If there is no id then we need to do something more sophisticated to find the element
					// from which this menu item came.  See bug 226380.  As an interim solution, we ignore if there
					// is no id which means the menu item will not be translatable.
					boolean idMatches = (thisId != null) && (localMatchOnly ? (thisId.endsWith("." + id)) : thisId.equals(id));
					if (idMatches) {
						String contributorName = element.getDeclaringExtension().getContributor().getName();
						Bundle bundle = InternalPlatform.getDefault().getBundle(contributorName);
						String fullId = subElement.getAttribute("id");

						try {
							LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
							return contribution.getLocalizableText(extensionPointId, elementName, subElementName, fullId, labelAttributeName);
						} catch (Exception e1) {
							/*
							 * If there were any problems reading the plugin.xml, return the text from
							 * the extension registry.  This is the localized text, without any information
							 * as to the source property file and key, so it must be returned as non-localizable
							 * text.
							 */
							e1.printStackTrace();
							return new NonTranslatableText(element.getAttribute(labelAttributeName));
						}
					}
				}
			}
		}
		return null;
	}

	private ITranslatableText extractFromPluginXml(String extensionPointId, String elementName, String id, String labelAttributeName, boolean localMatchOnly) {

		for (IConfigurationElement element: Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if ((element.getName().equals(elementName))) {
				String thisId = element.getAttribute("id");
				// FIXME: If there is no id then we need to do something more sophisticated to find the element
				// from which this menu item came.  See bug 226380.  As an interim solution, we ignore if there
				// is no id which means the menu item will not be translatable.
				boolean idMatches = (thisId != null) && (localMatchOnly ? (thisId.endsWith("." + id)) : thisId.equals(id));
				if (idMatches) {
					String contributorName = element.getDeclaringExtension().getContributor().getName();
					Bundle bundle = InternalPlatform.getDefault().getBundle(contributorName);
					String fullId = element.getAttribute("id");

					try {
						LocalizableContribution contribution = PluginXmlRegistry.getInstance().getLocalizableContribution(bundle);
						return contribution.getLocalizableText(extensionPointId, elementName, fullId, labelAttributeName);
					} catch (Exception e1) {
						/*
						 * If there were any problems reading the plugin.xml, return the text from
						 * the extension registry.  This is the localized text, without any information
						 * as to the source property file and key, so it must be returned as non-localizable
						 * text.
						 */
						e1.printStackTrace();
						return new NonTranslatableText(element.getAttribute(labelAttributeName));
					}
				}
			}
		}

		return null;
	}

	/**
	 * 
	 * @return
	 */
	public ITranslatableSet getTextSet() {
		return textSet;
	}

}
