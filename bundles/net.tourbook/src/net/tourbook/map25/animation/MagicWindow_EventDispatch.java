public class MagicWindow_EventDispatch implements MouseMotionListener, MouseListener, ChangeListener, ActionListener, MouseWheelListener, ListSelectionListener, KeyListener

{
   private List<MouseEvent> mouseEvents = new LinkedList<MouseEvent>();

   private List<ChangeEvent> changeEvents = new LinkedList<ChangeEvent>();

   private List<ActionEvent> actionEvents = new LinkedList<ActionEvent>();
   
   private List<MouseWheelEvent> mouseWheelEvents = new LinkedList<MouseWheelEvent>();
   
   private List<ListSelectionEvent> listEvents = new LinkedList<ListSelectionEvent>();

   private List<KeyEvent> keyEvents = new LinkedList<KeyEvent>();
   
   private boolean mouseDown;
   
   private synchronized void addMouseEvent(MouseEvent e)
   {
      mouseEvents.add(e);
   }

   public synchronized MouseEvent getMouseEvent()
   {
      if (!mouseEvents.isEmpty())
         return mouseEvents.remove(0);
      return null;
   }

   private synchronized void addChangeEvent(ChangeEvent e)
   {
      changeEvents.add(e);
   }

   public synchronized ChangeEvent getChangeEvent()
   {
      if (!changeEvents.isEmpty())
         return changeEvents.remove(0);
      return null;
   }
   ...
   
   public synchronized KeyEvent getKeyEvent()
   {
      if (!keyEvents.isEmpty())
         return keyEvents.remove(0);
      return null;
   }
   
   public void mouseMoved(MouseEvent e)
   {
      addMouseEvent(e);
   }

   public void mouseClicked(MouseEvent e)
   {
      addMouseEvent(e);
   }
   ...
}