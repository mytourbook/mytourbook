import com.jme3.input.AWTKeyInput;
import com.jme3.input.AWTMouseInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MagicWindowMonkey extends JMECanvasImplementor implements Anchor {

   // Items for scene
   protected Node               rootNode;

   protected com.jme.util.Timer timer;

   protected float              tpf;

   protected Camera             cam;

   Vector3f                     loc           = new Vector3f(10.0f, 1.5f, 0.0f); // camera location at start

   protected DisplaySystem      display;

   protected int                width, height;

   protected InputHandler       input;

   private Root                 root          = null;

   private Waterfall            source        = null;

   private MagicWindow          magicWindow   = null;

   protected EventDispatch      eventDispatch = new EventDispatch();

   // list of awt components to be updated
   private List<Updateable> toUpdate = new LinkedList<Updateable>();

   // a flag from teh AWT to JME threads to say erase and rewind
   private boolean doRefresh = false;

   private Canvas  canvas;

   // if true monkey display has more speed, but is not selectable
   private final static boolean noSelect = true;

   /**
    * This class should be subclasses - not directly instantiated.
    *
    * @param width
    *           canvas width
    * @param height
    *           canvas height
    */
   protected MagicWindowMonkey(final int width, final int height, final Waterfall s, final Root root, final MagicWindow b, final Canvas c) {
      this.width = width;
      this.height = height;
      this.root = root;
      canvas = c;
      source = s;
      magicWindow = b;
      // eventDispatch = new EventDispatch();
      magicWindow.getCanvas().addMouseListener(eventDispatch);
      magicWindow.getCanvas().addMouseMotionListener(eventDispatch);
      magicWindow.getCanvas().addMouseWheelListener(eventDispatch);
      //magicWindow.getCanvas().addKeyListener(eventDispatch);
   }

   public void doSetup() {
      display = DisplaySystem.getDisplaySystem();//= Parameters.getDisplay();
      //renderer = Parameters.getRenderer();

      renderer = new LWJGLRenderer(800, 600);
      renderer.setHeadless(true);
      display.setRenderer(renderer);
      DisplaySystem.updateStates(renderer);
      // Create a camera specific to the DisplaySystem that works with the width and height

      cam = renderer.createCamera(width, height);

      // Set up how our camera sees.
      cam.setFrustumPerspective(45.0f, (float) width / (float) height, 1, 1000);

      final Vector3f left = new Vector3f(1.0f, 0.0f, 0.0f);
      final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
      final Vector3f dir = new Vector3f(0.0f, 0f, 1.0f);

      // Move our camera to a correct place and orientation.
      cam.setFrame(loc, left, up, dir);

      // Signal that we've changed our camera's location/frustum.
      // cameraPerspective();
      //cameraParallel();

      cam.update();

      // Assign the camera to this renderer.
      renderer.setCamera(cam);

      renderer.setBackgroundColor(new ColorRGBA(0.1f, 0.2f, 0.6f, 1.0f));

      // Get a high resolution timer for FPS updates.
      timer = com.jme.util.Timer.getTimer();

      // Create rootNode
      rootNode = new Node("rootNode");

      // Create a ZBuffer to display pixels closest to the camera above farther ones.

      final ZBufferState buf = renderer.createZBufferState();
      buf.setEnabled(true);
      buf.setFunction(ZBufferState.CF_LEQUAL);
      // cameraPerspective();
      rootNode.setRenderState(buf);

      input = new KeyboardLookHandler(cam, 50f, 3f);
      //input.
      ((JMECanvas) canvas).setUpdateInput(true);

      final KeyListener kl = (KeyListener) KeyInput.get();

      canvas.addKeyListener(kl);
      magicWindow.addKeyListener(kl);

      ((AWTMouseInput) MouseInput.get()).setEnabled(true);
      ((AWTMouseInput) MouseInput.get()).setDragOnly(false);
      ((AWTMouseInput) MouseInput.get()).setRelativeDelta(canvas);

      canvas.addMouseListener(eventDispatch);

      setFocusListener();
      input.setEnabled(true);

      lightState = renderer.createLightState();
      assert (lightState != null);

      simpleSetup();

      setup = true;
   }

   private void setFocusListener() {
      canvas.addFocusListener(new FocusListener() {
         public void focusGained(final FocusEvent arg0) {
            ((AWTKeyInput) KeyInput.get()).setEnabled(true);
            ((AWTMouseInput) MouseInput.get()).setEnabled(true);
            input.setEnabled(true);
            //mouseLook = true;
         }

         public void focusLost(final FocusEvent arg0) {
            ((AWTKeyInput) KeyInput.get()).setEnabled(false);
            ((AWTMouseInput) MouseInput.get()).setEnabled(false);
            input.update(0);
            input.setEnabled(false);
            //mouseLook = false;
         }
      });
   }

   // from baseSimpleGame
   protected void cameraPerspective() {
      cam.setFrustumPerspective(45.0f, (float) display.getWidth() / (float) display.getHeight(), 1, 1000);
      cam.setParallelProjection(false);
      cam.update();
   }

   protected void cameraParallel() {
      cam.setParallelProjection(true);
      final float aspect = (float) display.getWidth() / display.getHeight();
      cam.setFrustum(-100, 1000, -50 * aspect, 50 * aspect, -50, 50);
      cam.update();
   }

   public void doUpdate() {

      /** Update tpf to time per frame according to the Timer. */

      timer.update();
      tpf = timer.getTimePerFrame();
      cam.update();

      simpleUpdate();
      doMouseUpdate();
      rootNode.updateGeometricState(tpf, true);
      // go through all registered updaters...

      final Iterator<Updateable> it = toUpdate.iterator();
      final ArrayList<Updateable> toRemove = new ArrayList<Updateable>();
      while (it.hasNext()) {
         final Updateable u = it.next();
         if (u.isDisposed()) {
            toRemove.add(u);
         }
         // always do update - in case action on dispose()
         u.doUpdate();

      }
      for (final Updateable r : toRemove) {
         toUpdate.remove(r);
      }
   }

   public void doRender() {
      renderer.clearBuffers();
      renderer.draw(rootNode);
      simpleRender();
      renderer.displayBackBuffer();
   }

   private void createSity() {
      createSity(this, false);
   }

   /**
    * Evaluate our tree
    * @param resetRandom - reset the random of the found instance? will create
    * new building!
    *
    */
   private void createSity(final Anchor anchor, final boolean resetRandom)
   {
      ...
      // show it
      rootNode.updateGeometricState(0.0f, true);
      rootNode.updateRenderState();

      // put the anchor back the way it was
      Parameters.anchor = oldAnchor;
   }

   public void simpleSetup() {
      createSity();
      allDone();
   }

   public void simpleUpdate() {
      timer.update();
      tpf = timer.getTimePerFrame();
      // very important line....
      input.update(tpf);
   }

   public void simpleRender() {}

   public Camera getCamera() {
      return cam;
   }

   private void doMouseUpdate() {

      MouseEvent event;
      while ((event = eventDispatch.getMouseEvent()) != null) {
         switch (event.getID()) {
         case MouseEvent.MOUSE_DRAGGED:
            mouseDraggedUpdate(event);
            break;
         case MouseEvent.MOUSE_PRESSED:
            mousePressedUpdate(event);
            break;
         case MouseEvent.MOUSE_RELEASED:
            mouseReleasedUpdate(event);
            break;
         case MouseEvent.MOUSE_WHEEL:
            break;
         case MouseEvent.MOUSE_MOVED:
            mouseMovedUpdate(event);
            break;
         case MouseEvent.MOUSE_ENTERED:
            mouseEnteredUpdate(event);
         case MouseEvent.MOUSE_EXITED:
            mouseExitUpdate(event);
            break;
         }
      }
      MouseWheelEvent wheel;
      while ((wheel = eventDispatch.getMouseWheelEvent()) != null) {
         mouseWheelUpdate(wheel);
      }
      KeyEvent key;
      while ((key = eventDispatch.getKeyEvent()) != null) {
         switch (key.getID()) {
         case KeyEvent.KEY_RELEASED:
            keyRelease(key);
            break;
         }
      }

   }

   /**
    * Key release handler delegated through eventDispatch
    *
    * @param e
    */
   private void keyRelease(final KeyEvent e)
   {
      if (e.getKeyCode() == KeyEvent.VK_SPACE)
      {
         ...
      }
      else if (e.getKeyCode() == KeyEvent.VK_ENTER)
      {
         ...
      }
   }

   /**
    * Takes a mouse event and finds the object to select (or none!)
    *
    * @param e
    *            the mouse event with the locations
    */
   private void processClick(final MouseEvent e)
   {
      // jME voodoo to make it work (read: trial and error)
      display.setRenderer(renderer);
      DisplaySystem.updateStates(renderer);
      // Assign the camera to this renderer.
      renderer.setCamera(cam);

      ...
   }

   public void mouseDraggedUpdate(final MouseEvent e) {

   }

   public void mousePressedUpdate(final MouseEvent e) {

   }

   public void mouseReleasedUpdate(final MouseEvent e) {
      processClick(e);
   }

   private void mouseWheelUpdate(final MouseWheelEvent e) {

   }

}
