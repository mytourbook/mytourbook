package net.tourbook.map25.animation;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MagicWindow extends JFrame
{


   private static final long serialVersionUID = 1L;

   ...

   JPanel contentPane;

   JPanel mainPanel = new JPanel();

   public Canvas comp = null;

   JPanel spPanel = new JPanel();

   MagicWindowMonkey impl;

   public MagicWindow(final Waterfall toMake, final Root root)
   {
      // center the frame
      setLocation(450,400);
      // show frame
      setVisible(true);
      setResizable(false);
      this.toMake = toMake;
      this.root = root;
      addKeyListener(Sity.self);
      Parameters.setIcon(this);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(final WindowEvent e)
         {
            dispose();
         }
      });

      init();
      pack();

      thread = new MonkeyThread(comp);
      thread.start();
   }

   /**
    * Should never resize, too complicated to reconcile JME and AWT TLAs!
    *
    */
   protected void doResize()   {

      impl.resizeCanvas(comp.getWidth(), comp.getHeight());
      //impl.resizeCanvas(width, height);
   }

   // Component initialization
   public void end()
   {
      Parameters.magicWindow = null;
      Sity.self.update();
      thread.stopMonkeyingAround();
      dispose();
   }

   protected Canvas getCanvas()
   {
      return comp;
   }

   private void init()
   {
      contentPane = (JPanel) getContentPane();
      contentPane.setLayout(new BorderLayout());

      //mainPanel.setLayout(new GridBagLayout());

      setTitle("Sity - preview view");

      // GL STUFF

      // make the canvas:
      comp = DisplaySystem.getDisplaySystem("lwjgl").createCanvas(width,            height);

      // add a listener... if window is resized, we can do something about it.
      comp.addComponentListener(new ComponentAdapter()
      {
         @Override
         public void componentResized(final ComponentEvent ce)
         {
            doResize();
         }
      });

      // Important! Here is where we add the guts to the panel:
      impl = new MagicWindowMonkey(width, height, toMake, root, this, comp);

      comp.addKeyListener(Sity.self);
      comp.addKeyListener(impl.eventDispatch);
      ((JMECanvas) comp).setImplementor(impl);
      comp.requestFocus();

      // END OF GL STUFF
      //mainPanel.add(spPanel, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0,
      //      GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
      //            5, 5, 0, 5), 0, 0));
      comp.setBounds(0,0,width, height);
      //comp.setSize(width,height);
      contentPane.add(comp, BorderLayout.CENTER);
}

   // Overridden so we can exit when window is closed
   @Override
   protected void processWindowEvent(final WindowEvent e)
   {
      super.processWindowEvent(e);
      if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
         pop(); // just close the window, nothing else!
      }
   }

   protected void setSide(final Component comp)
   {
      spPanel.removeAll();
      spPanel.add(comp);
   }
}