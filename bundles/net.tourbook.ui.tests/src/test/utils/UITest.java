package utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public abstract class UITest {

   protected static final String STATISTICS_VIEW_NAME = "Statistics";

   protected SWTWorkbenchBot     bot                  = new SWTWorkbenchBot();
}
