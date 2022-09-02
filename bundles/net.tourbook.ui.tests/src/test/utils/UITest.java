package utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

public abstract class UITest {

   protected SWTWorkbenchBot bot                                   = new SWTWorkbenchBot();

   protected int             tourBookView_Distance_Column_Index    = 10;
   protected int             tourBookView_Temperature_Column_Index = 3;
}
