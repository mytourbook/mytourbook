// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Java reflection convenience class
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2007/03/06  Martin D. Flynn
//     -Added 'CONSTRUCTOR' constant
//  2007/11/28  Martin D. Flynn
//     -Added 'isDispatchThread()' wrapper for 'EventQueue.isDispatchThread()'
//  2008/05/20  Martin D. Flynn
//     -Updated to use Varargs on 'invoke' arguments.
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;

public class MethodAction
    implements ActionListener, Runnable
{

    // ------------------------------------------------------------------------

    public  static final String CONSTRUCTOR = "<init>";

    // ------------------------------------------------------------------------

    private Class               targetClass = null;
    private Object              target      = null;
    private AccessibleObject    method      = null;
    private Class               argClass[]  = null;
    private Object              args[]      = null;
    private Object              rtnValue    = null;
    private Throwable           error       = null;

    // ------------------------------------------------------------------------
    // static methods:
    //   target  : either a Class instance, or a class name String
    //   methName: static method name (null or "<init>" for constructor)
    //   argClass: static method arugment types
    //   args    : static method arguments
    // instance methods:
    //   target  : object instance
    //   methName: instance method name
    //   argClass: method arugment types (or null of no arguments)
    //   args    : method arguments (or null of no arguments)

    /**
    *** Constructor
    *** @param targ     The Object target (a class instance for static methods)
    *** @param methName A method name
    **/
    public MethodAction(Object targ, String methName)
        throws NoSuchMethodException, ClassNotFoundException
    {
        this(targ, methName, null, null);
    }

    /**
    *** Constructor
    *** @param targ     The Object target (a class instance for static methods)
    *** @param methName A method name
    *** @param argClass An array/list of argument class types
    **/
    public MethodAction(Object targ, String methName, Class... argClass)
        throws NoSuchMethodException, ClassNotFoundException
    {
        this(targ, methName, argClass, null);
    }

    /**
    *** Constructor
    *** @param targ     The Object target (a class instance for static methods)
    *** @param methName A method name
    *** @param argClass An array/list of argument class types
    *** @param args     An array of the actual method arguments
    **/
    public MethodAction(Object targ, String methName, Class argClass[], Object args[])
        throws NoSuchMethodException, ClassNotFoundException
    {
        this.target      = (targ instanceof String)? Class.forName((String)targ) : targ;
        this.targetClass = (this.target instanceof Class)? (Class)this.target : this.target.getClass();
        this.argClass    = (argClass != null)? argClass : new Class[0];
        if (methName == null) {
            this.method  = this.targetClass.getConstructor(this.argClass);
        } else
        if (methName.equals(CONSTRUCTOR)) {
            this.method  = this.targetClass.getConstructor(this.argClass);
        } else {
            this.method  = this.targetClass.getMethod(methName, this.argClass);
        }
        this.setArgs(args);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the bean method name for the specified field
    *** @param prefix    Either "get" or "set"
    *** @param fieldName The field name
    *** @return The 'getter'/'setter' method name
    **/
    protected static String _beanMethodName(String prefix, String fieldName)
    {
        if ((prefix != null) && (fieldName != null)) {
            StringBuffer sb = new StringBuffer(prefix);
            sb.append(fieldName.substring(0,1).toUpperCase());
            sb.append(fieldName.substring(1));
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
    *** Returns the bean 'getter' method name for the specified field
    *** @param fieldName The field name
    *** @return The 'getter' method name
    **/
    public static String getterMethodName(String fieldName)
    {
        return _beanMethodName("get", fieldName);
    }

    /**
    *** Returns the bean 'setter' method name for the specified field
    *** @param fieldName The field name
    *** @return The 'setter' method name
    **/
    public static String setterMethodName(String fieldName)
    {
        return _beanMethodName("set", fieldName);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a MethodAction instance which represents a bean field 'getter' method.
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @return The MethodAction instance
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static MethodAction GetterMethod(Object targ, String fieldName)
        throws NoSuchMethodException, ClassNotFoundException
    {
        String mn = _beanMethodName("get", fieldName);
        return new MethodAction(targ, mn, (Class[])null);
    }

    /**
    *** Invokes a 'getter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @return The returned Object for the 'getter' method
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static Object InvokeGetter(Object targ, String fieldName)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        return GetterMethod(targ, fieldName).invoke();
    }

    /**
    *** Returns a MethodAction instance which represents a bean field 'setter' method.
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @return The MethodAction instance
    *** @throws NoSuchMethodException if the 'setter' method does not exist
    **/
    public static MethodAction SetterMethod(Object targ, String fieldName, Class... argType)
        throws NoSuchMethodException, ClassNotFoundException
    {
        String mn = _beanMethodName("set", fieldName);
        if (argType == null) {
            argType = new Class[] { Object.class };
        }
        return new MethodAction(targ, mn, argType);
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, Class valClass, Object value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        Class clz = (valClass != null)? valClass : (value != null)? value.getClass() : Object.class;
        SetterMethod(targ, fieldName, clz).invoke(new Object[] { value });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, String value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, String.class).invoke(new Object[] { value });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, boolean value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, Boolean.TYPE).invoke(new Object[] { new Boolean(value) });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, int value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, Integer.TYPE).invoke(new Object[] { new Integer(value) });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, long value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, Long.TYPE).invoke(new Object[] { new Long(value) });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, float value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, Float.TYPE).invoke(new Object[] { new Float(value) });
    }

    /**
    *** Invokes a 'setter' method on the specified target for the specified field name
    *** @param targ      The Object target
    *** @param fieldName The field name for which the 'getter' is returned
    *** @param value     The value to 'set'.
    *** @throws NoSuchMethodException if the 'getter' method does not exist
    **/
    public static void InvokeSetter(Object targ, String fieldName, double value)
        throws NoSuchMethodException, ClassNotFoundException, Throwable
    {
        SetterMethod(targ, fieldName, Double.TYPE).invoke(new Object[] { new Double(value) });
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the MethodAction target (may be a Class or Object instance)
    *** @return A class or Object instance
    **/
    public Object getTarget()
    {
        return this.target;
    }

    /**
    *** Sets the atcual arguments to pass to the method at the time of invocation
    *** @param args  An array/list of method arguments
    **/
    public void setArgs(Object... args)
    {
        this.args = args;
    }

    /**
    *** Returns an array of currently defined arguments
    *** @return An array of currently defined arguments
    **/
    public Object[] getArgs()
    {
        return this.args;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Invokes this MethodAction with the specified array/list of arguments
    *** @param args An array/list of arguments
    *** @return The returned Object
    **/
    public Object invoke(Object... args)
        throws Throwable
    {
        this.setArgs(args);
        return this.invoke();
    }

    /**
    *** Invokes this MethodAction with the currently specified array/list of arguments
    *** @return The return Object
    **/
    public Object invoke()
        throws Throwable
    {
        this.error = null;
        this.rtnValue = null;
        try {
            if (this.method instanceof Constructor) {
                this.rtnValue = ((Constructor)this.method).newInstance(this.getArgs());
            } else
            if (this.method instanceof Method) {
                this.rtnValue = ((Method)this.method).invoke(this.getTarget(), this.getArgs());
            }
            return this.rtnValue;
        } catch (InvocationTargetException ite) {
            this.error = ite.getCause();
            if (this.error == null) { this.error = ite; }
            throw this.error;
        } catch (Throwable t) { // trap any remaining method invocation error
            this.error = t;
            throw this.error;
        }
    }

    /**
    *** Gets the returned value from the last MethodAction invocation
    *** @return The returned Object
    **/
    public Object getReturnValue()
    {
        return this.rtnValue;
    }

    // ------------------------------------------------------------------------

    /**
    *** Place the Runnable object in the current EventQueue execution stack to be executed after the
    *** specified initial delay.
    *** @param delayMillis  The number of milliseconds to wait befor executing the specified Runnable.
    ***                     This method will still return immediately.
    *** @param r            The Runnable instance to execute (ie. call the "run()" method)
    **/
    public static void invokeDelayed(int delayMillis, final Runnable r)
    {
        if (r != null) {
            if (delayMillis <= 0) {
                MethodAction.invokeLater(r);
            } else {
                javax.swing.Timer delay = new javax.swing.Timer(delayMillis, new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        r.run();
                    }
                });
                delay.setRepeats(false);
                delay.start();
            }
        }
    }

    /**
    *** Place this MethodAction object in the current EventQueue execution stack to be executed after the
    *** specified initial delay.
    *** @param delayMillis  The number of milliseconds to wait befor executing this MethodAction.
    ***                     This method will still return immediately.
    **/
    public void invokeDelayed(int delayMillis)
    {
        javax.swing.Timer delay = new javax.swing.Timer(delayMillis, this);
        delay.setRepeats(false);
        delay.start();
    }

    // ------------------------------------------------------------------------

    /**
    *** Queues the invocation of the specified ActionListener on the EventQueue
    *** @param al   The ActionListener to execute
    *** @param ae   The ActionEvent to pass to the ActionListener
    **/
    public static void invokeLater(final ActionListener al, final ActionEvent ae)
    {
        MethodAction.invokeLater(new Runnable() { public void run() {al.actionPerformed(ae);} });
    }

    /**
    *** Queues the invocation of the specified Runnable on the EventQueue
    *** @param r   The Runnable to execute
    **/
    public static void invokeLater(Runnable r)
    {
        if (r != null) {
            //Toolkit.getDefaultToolkit().getSystemEventQueue().invokeLater(r);
            EventQueue.invokeLater(r);
        }
    }

    /**
    *** Queues the invocation of the specified Runnable on the EventQueue, and waits for the EventQueue
    *** thread to complete execution of the specified Runnable.
    *** @param r   The Runnable to execute
    **/
    public static void invokeAndWait(Runnable r)
        throws InterruptedException, InvocationTargetException
    {
        if (r != null) {
            // call from a child thread only! (will block otherwise)
            //Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(r);
            EventQueue.invokeAndWait(r);
        }
    }

    /** 
    *** Queues the invocation of this MethodAction on the EventQueue
    **/
    public void invokeLater()
    {
        MethodAction.invokeLater(this);
    }

    /** 
    *** Queues the invocation of this MethodAction on the EventQueue, and waits for the EventQueue
    **/
    public void invokeAndWait()
        throws InterruptedException, InvocationTargetException
    {
        MethodAction.invokeAndWait(this);
    }

    /**
    *** Runs this MethodAction now
    **/
    public void run()
    {
        try {
            this.invoke();
        } catch (Throwable t) { // trap any method invocation error
            Print.logError("'invoke' error " + t);
        }
    }

    /**
    *** ActionListener interface execution
    *** @param ae The ActionEvent
    **/
    public void actionPerformed(ActionEvent ae)
    {
        try {
            this.invoke();
        } catch (Throwable t) { // trap any method invocation error
            Print.logError("'invoke' error " + t);
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the calling Thread is the event dispatch thread
    *** @return True if the calling Thread is the event dispatch thread
    **/
    public static boolean isDispatchThread()
    {
        return EventQueue.isDispatchThread();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Create a new instance of the specified class name
    *** @param className  The class name to instantiate
    *** @return The new instance of the specified class name
    **/
    public static Object newInstance(String className)
        throws NoSuchMethodException, ClassNotFoundException, 
               InstantiationException, IllegalAccessException
    {
        if (!StringTools.isBlank(className)) {
            //try {
                return Class.forName(className).newInstance();
            //} catch (InvocationTargetException ite) {
            //    Throwable th = ite.getCause();
            //    if (th == null) { th = ite; }
            //    throw th;
            //}
        } else {
            throw new ClassNotFoundException("Class name is null/blank");
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
