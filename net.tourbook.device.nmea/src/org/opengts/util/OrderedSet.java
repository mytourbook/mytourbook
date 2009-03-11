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
//  This class provides an ordered Set
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added initial Java 5 'generics'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;

public class OrderedSet<K>
    implements Set<K>, java.util.List<K>, Cloneable
{

    // ------------------------------------------------------------------------

    protected static final int ENTRY_ADDED   = 1;
    protected static final int ENTRY_REMOVED = 2;

    /**
    *** ChangeListener interface
    **/
    public static interface ChangeListener
    {
        public void entryAdded(OrderedSet set, Object obj);
        public void entryRemoved(OrderedSet set, Object obj);
    }

    /**
    *** ChangeListener adapter
    **/
    public static class ChangeListenerAdapter
        implements ChangeListener
    {
        public void entryAdded(OrderedSet set, Object obj) {
            //Print.logDebug("Item added: " + obj);
        }
        public void entryRemoved(OrderedSet set, Object obj) {
            //Print.logDebug("Item removed: " + obj);
        }
    }

    // ------------------------------------------------------------------------

    private java.util.List<K>               elements = null;
    private boolean                         retainOriginalValue = false;
    private java.util.List<ChangeListener>  changeListeners = null;
    private int                             addChangeCount = 0;
    private int                             removeChangeCount = 0;

    /**
    *** Constructor
    **/
    public OrderedSet()
    {
        super();
    }

    /**
    *** Construtor 
    *** @param retainOriginalValue  True to ignore duplicate entries, false overwrite existing entries
    ***                             with any newly added duplicates.
    **/
    public OrderedSet(boolean retainOriginalValue)
    {
        this.setRetainOriginalValue(retainOriginalValue);
    }

    /**
    *** Construtor 
    *** @param c    Collection of Objects used to initialize this set.
    *** @param retainOriginalValue  True to ignore duplicate entries, false overwrite existing entries
    ***                             with any newly added duplicates.
    **/
    public OrderedSet(Collection<? extends K> c, boolean retainOriginalValue)
    {
        this(retainOriginalValue);
        this.addAll(c);
    }

    /**
    *** Construtor 
    *** @param c    Collection of Objects used to initialize this set.
    **/
    public OrderedSet(Collection<? extends K> c)
    {
        this(c, false);
    }

    /**
    *** Construtor 
    *** @param a    Array of Objects used to initialize this set.
    *** @param retainOriginalValue  True to ignore duplicate entries, false overwrite existing entries
    ***                             with any newly added duplicates.
    **/
    public OrderedSet(K a[], boolean retainOriginalValue)
    {
        this(retainOriginalValue);
        this.addAll(a);
    }

    /**
    *** Construtor 
    *** @param a    Array of Objects used to initialize this set.
    **/
    public OrderedSet(K a[])
    {
        this(a, false);
    }

    /**
    *** Copy Construtor 
    *** @param os   Other OrderedSet used to initialize this set
    **/
    public OrderedSet(OrderedSet<K> os)
    {
        super();
        this.setRetainOriginalValue(os.getRetainOriginalValue());
        this.getBackingList().addAll(os.getBackingList());
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a clone of this OrderedSet
    *** @return The cloned OrderedSet
    **/
    public Object clone()
    {
        return new OrderedSet<K>(this);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a list of change listeners
    *** @param create  True to create an empty list if no change listeners have been added
    *** @return The list of change listeners
    **/
    protected java.util.List<ChangeListener> getChangeListeners(boolean create)
    {
        if ((this.changeListeners == null) && create) { 
            this.changeListeners = new Vector<ChangeListener>(); 
        }
        return this.changeListeners;
    }

    /**
    *** Returns true if any change listeners have been registered
    *** @return True if any change listeners have been registered
    **/
    protected boolean hasChangeListeners()
    {
        return (this.getChangeListeners(false) != null);
    }

    /**
    *** Adds a change listener to this OrderedSet
    *** @param cl  The change listener to add
    **/
    public void addChangeListener(ChangeListener cl)
    {
        if (cl != null) {
            java.util.List<ChangeListener> listeners = this.getChangeListeners(true);
            if (!listeners.contains(cl)) {
                //Print.dprintln("Adding ChangeListener: " + StringTools.className(cl));
                listeners.add(cl);
            }
        }
    }

    /**
    *** Removes a change listener to this OrderedSet
    *** @param cl  The change listener to remove
    **/
    public void removeChangeListener(ChangeListener cl)
    {
        if (cl != null) {
            java.util.List<ChangeListener> listeners = this.getChangeListeners(false);
            if (listeners != null) {
                //Print.dprintln("Removing ChangeListener: " + StringTools.className(cl));
                listeners.remove(cl);
            }
        }
    }

    /**
    *** Notifies all change listeners of a change to this OrderedSet
    *** @param action   The change action
    *** @param obj      The Object changed
    **/
    protected void notifyChangeListeners(int action, Object obj)
    {
        java.util.List<ChangeListener> listeners = this.getChangeListeners(false);
        if (listeners != null) {
            for (Iterator i = listeners.iterator(); i.hasNext();) {
                ChangeListener cl = (ChangeListener)i.next();
                if (action == ENTRY_ADDED) {
                    cl.entryAdded(this, obj);
                    addChangeCount++;
                } else
                if (action == ENTRY_REMOVED) {
                    cl.entryRemoved(this, obj);
                    removeChangeCount++;
                } else {
                    Print.logError("Unrecognized action: " + action);
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the ordered backing list for this OrderedSet
    *** @return The backing list instance
    **/
    protected java.util.List<K> getBackingList()
    {
        if (this.elements == null) { this.elements = new Vector<K>(); }
        return this.elements;
    }

    /**
    *** Gets the ordered backing list for this OrderedSet
    *** @return The backing list instance
    **/
    public java.util.List<K> getList()
    {
        return this.getBackingList();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if original added entries should be retained if a duplicate entry
    *** is subsequently added.
    *** @return True if original added entries should be retained
    **/
    public boolean getRetainOriginalValue()
    {
        return this.retainOriginalValue;
    }

    /**
    *** Sets the retain-original state for this OrderedSet if duplicate entries are added.
    *** @param state  True to retain original added entries
    **/
    public void setRetainOriginalValue(boolean state)
    {
        this.retainOriginalValue = state;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Objects at the specified index
    *** @param ndx  The index
    *** @return The object at the specified index
    **/
    public K get(int ndx)
    {
        // java.util.List (mandatory)
        // allowed, since this is an ordered set (backed by a List)
        return this.getBackingList().get(ndx);
    }

    /**
    *** Throws an UnsupportedOperationException
    *** @param ndx  The index
    *** @param obj  The Object to set
    *** @return The previous Object
    *** @throws UnsupportedOperationException always
    **/
    public K set(int ndx, K obj)
    {
        // java.util.List (optional)
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the Object at the specified index
    *** @param ndx  The index
    *** @param obj  The Object to add
    **/
    protected void _add(int ndx, K obj)
    {
        if ((ndx < 0) || (ndx >= this.getBackingList().size())) {
            this.getBackingList().add(obj); // add to end
        } else {
            this.getBackingList().add(ndx, obj); // insert at index
        }
        this.notifyChangeListeners(ENTRY_ADDED, obj);
    }

    /**
    *** Adds the Object to the end of the list
    *** @param obj  The Object to add
    **/
    public boolean add(K obj)
    {
        if (this.getRetainOriginalValue()) {
            boolean contained = this.contains(obj);
            if (!contained) { // retain original
                this._add(-1, obj);
            }
            return !contained;
        } else {
            boolean contained = this.remove(obj);   // remove original
            this._add(-1, obj);
            return !contained;
        }
    }

    /**
    *** Adds all Objects in the specified Collection to this set
    *** @param c  The Collection
    *** @return True if any items were added to this list
    **/
    public boolean addAll(Collection<? extends K> c)
    {
        if ((c != null) && (c.size() > 0)) {
            for (Iterator<? extends K> i = c.iterator(); i.hasNext();) {
                this.add(i.next()); // TODO: should check retain-original state for actual changes
            }
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Adds all Objects in the specified array to this set
    *** @param a  The array
    *** @return True if any items were added to this list
    **/
    public boolean addAll(K a[])
    {
        if ((a != null) && (a.length > 0)) {
            for (int i = 0; i < a.length; i++) {
                this.add(a[i]); // TODO: should check retain-original state for actual changes
            }
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Adds the Object at the specified index
    *** @param ndx  The index
    *** @param obj  The Object to add
    **/
    public void add(int ndx, K obj)
    {
        if (this.getRetainOriginalValue()) {
            boolean contained = this.contains(obj);
            if (!contained) { // retain original
                this._add(ndx, obj);
            }
            //return !contained;
        } else {
            boolean contained = this.remove(obj);   // remove original
            this._add(ndx, obj);
            //return !contained;
        }
    }

    /**
    *** Throws an UnsupportedOperationException
    *** @param ndx  The index
    *** @param c    The Collection
    *** @return True if this set was changed
    *** @throws UnsupportedOperationException always
    **/
    public boolean addAll(int ndx, Collection<? extends K> c) 
    {
        // java.util.List (optional)
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this set contains the specified Object 
    *** @return True if this set contains the specified Object 
    **/
    public boolean contains(Object obj)
    {
        return this.getBackingList().contains(obj);
    }

    /**
    *** Returns true if this set contains all items specified in the Collection
    *** @return True if this set contains all items specified in the Collection
    **/
    public boolean containsAll(Collection<?> c)
    {
        return this.getBackingList().containsAll(c);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this set is equivalent to the specified Object
    *** @param other  The other Object
    *** @return True if this set is equivalent to the specified Object
    **/
    public boolean equals(Object other)
    {
        if (other instanceof OrderedSet) {
            OrderedSet os = (OrderedSet)other;
            java.util.List L1 = this.getBackingList();
            java.util.List L2 = os.getBackingList();
            //boolean eq = L1.containsAll(L2) && L2.containsAll(L1);
            boolean eq = L1.equals(L2); // same elements, same order
            return eq;
        } else {
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns the hashcode for this set
    *** @return The hashcode for this set
    **/
    public int hashCode()
    {
        return this.getBackingList().hashCode();
    }

    // ------------------------------------------------------------------------

    /**
    *** Removes the specified object from this set
    *** @param obj The Object to remove
    *** @return True if the object was remove, false if the object didn't exist in this set
    **/
    protected boolean _remove(Object obj)
    {
        //Print.dprintln("Removing: " + obj);
        if (this.getBackingList().remove(obj)) {
            this.notifyChangeListeners(ENTRY_REMOVED, obj);
            return true;
        } else {
            return false;
        }
    }

    /**
    *** Removes the specified object referenced by the specified iterator from this set
    *** @param obj The Object to remove
    *** @param i   The Iterator which references the Objet to remove
    *** @return True
    **/
    protected boolean _remove(Object obj, Iterator i)
    {
        //Print.dprintln("Removing: " + obj);
        i.remove();
        this.notifyChangeListeners(ENTRY_REMOVED, obj);
        return true;
    }

    /**
    *** Throws UnsupportedOperationException
    *** @param ndx The object index to remove
    *** @return The removed Object
    *** @throws UnsupportedOperationException always
    **/
    public K remove(int ndx)
    {
        // java.util.List (optional)
        throw new UnsupportedOperationException();
    }

    /**
    *** Removes the specified object from this set
    *** @param obj  THe Object to remove
    *** @return True if the object was removed
    **/
    public boolean remove(Object obj)
    {
        return this._remove(obj);
    }

    /**
    *** Remove all Objects contained in the specified Collection from this set
    *** @param c  The Collection containing the list of Objects to remove from this set
    *** @return True if elements were removed from this set
    **/
    public boolean removeAll(Collection<?> c)
    {
        if (!this.hasChangeListeners()) {
            return this.getBackingList().removeAll(c);
        } else
        if (c == this) {
            if (this.size() > 0) {
                this.clear();
                return true;
            } else {
                return false;
            }
        } else
        if ((c != null) && (c.size() > 0)) {
            boolean changed = false;
            for (Iterator i = c.iterator(); i.hasNext();) {
                if (this.remove(i.next())) { changed = true; }
            }
            return changed;
        } else {
            return false;
        }
    }

    /**
    *** Removes all Object from this set that are not reference in the specified Collection
    *** @param c  The Collection of Objects to keep
    *** @return True if any Objects were removed from this list
    **/
    public boolean retainAll(Collection<?> c)
    {
        if (!this.hasChangeListeners()) {
            return this.getBackingList().retainAll(c);
        } else
        if (c == this) {
            return false;
        } else
        if ((c != null) && (c.size() > 0)) {
            boolean changed = false;
            for (Iterator<?> i = this.getBackingList().iterator(); i.hasNext();) {
                Object obj = i.next();
                if (!c.contains(obj)) {
                    this._remove(obj, i);
                    changed = true;
                }
            }
            return changed;
        } else {
            return false;
        }
    }

    /**
    *** Clears all Objects from this set
    **/
    public void clear()
    {
        if (!this.hasChangeListeners()) {
            this.getBackingList().clear();
        } else {
            for (Iterator i = this.getBackingList().iterator(); i.hasNext();) {
                Object obj = i.next();
                this._remove(obj, i);
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the number of elements in this set
    *** @return The number of elements in this set
    **/
    public int size()
    {
        return this.getBackingList().size();
    }

    /**
    *** Returns true if this set is empty
    *** @return True if this set is empty
    **/
    public boolean isEmpty()
    {
        return (this.size() == 0);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the index of the specified Object in this set
    *** @param obj  The Object for which the index is returned
    *** @return The index of the specified Object, or -1 if the Object does not
    ***         exist in this set.
    **/
    public int indexOf(Object obj)
    {
        // java.util.List
        return this.getBackingList().indexOf(obj);
    }

    /**
    *** Returns the last index of the specified Object in this set.  Since this is a
    *** 'Set', any value exists at-most once, this is essentially the same as calling
    *** 'indexOf(obj)'.
    *** @param obj  The Object for which the index is returned
    *** @return The last index of the specified Object, or -1 if the Object does not
    ***         exist in this set.
    **/
    public int lastIndexOf(Object obj)
    {
        // java.util.List
        return this.getBackingList().lastIndexOf(obj);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an ordered Iterator over the elements in this set.
    *** @return An Iterator over the elements in this set.
    **/
    public Iterator<K> iterator()
    {
        if (!this.hasChangeListeners()) {
            // OK, since only 'remove' is allowed
            return this.getBackingList().iterator();
        } else {
            return new Iterator<K>() {
                private K thisObject = null;
                private Iterator<K> i = OrderedSet.this.getBackingList().iterator();
                public boolean hasNext() {
                    return i.hasNext();
                }
                public K next() {
                    this.thisObject = i.next();
                    return this.thisObject;
                }
                public void remove() {
                    OrderedSet.this._remove(this.thisObject, i);
                    this.thisObject = null;
                }
            };
        }
    }

    /**
    *** Returns a ListIterator over this set
    *** @return The ListIterator
    **/
    public ListIterator<K> listIterator()
    {
        // java.util.List (mandatory)
        return this.listIterator(-1);
    }

    /**
    *** Returns a ListIterator over this set.
    *** @param ndx  The starting index.
    *** @return The ListIterator
    **/
    public ListIterator<K> listIterator(final int ndx)
    {
        if (!this.hasChangeListeners()) {
            // OK, since only 'remove' is allowed
            return (ndx >= 0)?
                this.getBackingList().listIterator(ndx) :
                this.getBackingList().listIterator();
        } else {
            return new ListIterator<K>() {
                private K thisObject = null;
                private ListIterator<K> i = (ndx >= 0)?
                    OrderedSet.this.getBackingList().listIterator(ndx) :
                    OrderedSet.this.getBackingList().listIterator();
                public boolean hasNext() {
                    return i.hasNext();
                }
                public boolean hasPrevious() {
                    return i.hasPrevious();
                }
                public K next() {
                    this.thisObject = i.next();
                    return this.thisObject;
                }
                public int nextIndex() {
                    return i.nextIndex();
                }
                public K previous() {
                    this.thisObject = i.previous();
                    return this.thisObject;
                }
                public int previousIndex() {
                    return i.previousIndex();
                }
                public void remove() {
                    OrderedSet.this._remove(this.thisObject, i);
                    this.thisObject = null;
                }
                public void add(K obj) {
                    throw new UnsupportedOperationException();
                }
                public void set(K obj) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Thows UnsupportedOperationException
    *** @param fromIndex  The 'from' index.
    *** @param toIndex    The 'to' index.
    *** @throws UnsupportedOperationException always
    **/
    public List<K> subList(int fromIndex, int toIndex)
    {
        // java.util.List (mandatory?)
        // not currently worth the effort to implement this
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of Object elements in this set
    *** @return An array of Object elements in this set
    **/
    public Object[] toArray()
    {
        return this.getBackingList().toArray(new Object[this.size()]);
    }

    /**
    *** Returns an array of Object elements in this set
    *** @param a  The array into which the elements are copied
    *** @return An array of Object elements in this set
    **/
    public <K> K[] toArray(K a[])
    {
        return this.getBackingList().toArray(a);
    }

    // ------------------------------------------------------------------------

    /**
    *** Prints the contents of this set (for debug/testing purposes)
    **/
    public void printContents()
    {
        int n = 0;
        for (Iterator i = this.iterator(); i.hasNext();) {
            Print.logInfo("" + (n++) + "] " + i.next());
        }
    }

    // ------------------------------------------------------------------------

}
