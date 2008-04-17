//
// $Id$

package com.threerings.tudey.util;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Iterator;

import com.threerings.io.Streamable;
import com.threerings.export.Exportable;

/**
 * Maps pairs of coordinates (in the range [-32767, +32767]) to lists of
 * objects using a 2D hash table that employs chaining to resolve collisions.
 */
public class CoordMultiMap<T> extends CoordMap<T>
    implements Streamable, Exportable
{
    /**
     * Creates a new map with the default capacity and load factor.
     */
    public CoordMultiMap ()
    {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the default load factor and the specified capacity.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordMultiMap (int capacity)
    {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new map with the specified capacity and load factor.
     *
     * @param capacity the number of entries in each dimension, expressed as a power of two.
     */
    public CoordMultiMap (int capacity, float loadFactor)
    {
        setCapacity(capacity);
        _loadFactor = loadFactor;
    }

    @Override // documentation inherited
    public T get (int x, int y)
    {
        throw new UnsupportedOperationException("CoordMultiMap.get(int,int) is unsupported, use getAll(int,int) instead.");
    }

    /**
     * Gets all the elements at the specified coordinates.
     */
    public Iterator<T> getAll (final int x, final int y)
    {
        return new Iterator<T>() {
            public boolean hasNext () {
                checkConcurrentModification();
                for (; _entry != null; _entry = _entry.next) {
                    if (_entry.key == _key) {
                        return true;
                    }
                }
                return false;
            }
            public T next () {
                checkConcurrentModification();
                if (_entry == null) {
                    throw new NoSuchElementException();
                }
                T val = _entry.value;
                _entry = _entry.next;
                return val;
            }
            public void remove () {
                checkConcurrentModification();
                if (_entry == null) {
                    throw new IllegalStateException();
                }
                _entry.next = _entry.next.next;
                _size--;
                _omodcount = ++_modcount;
            }
            protected void checkConcurrentModification () {
                if (_modcount != _omodcount) {
                    throw new ConcurrentModificationException();
                }
            }
            protected Entry<T> _entry = _entries[getHashIndex(x, y)];
            protected int _omodcount = _modcount;
            protected int _key = CoordUtil.getCoord(x, y);
        };
    }

    /**
     * Maps the given value at the specified coordinates.
     *
     * @return the value inserted.
     */
    public T put (int x, int y, T value)
    {
        // make sure we have space for the new value
        ensureCapacity(_size + 1);

        // see if there's an existing chain
        int key = CoordUtil.getCoord(x, y);
        int idx = getHashIndex(x, y);
        Entry<T> entry = _entries[idx];
        if (entry == null) {
            _entries[idx] = new Entry<T>(key, value);
            _size++;
            _modcount++;
            return value;
        }

        // the chain has started, so update/append
        for (;; entry = entry.next) {
             if (entry.next == null) {
                entry.next = new Entry<T>(key, value);
                _size++;
                _modcount++;
                return value;
            }
        }
    }

    @Override // documentation inherited
    public T remove (int x, int y)
    {
        throw new UnsupportedOperationException("CoordMultiMap.remove(int,int) is unsupported, use removeAll(int,int) instead.");
    }

    /**
     * Removes the value at the specified coordinates.
     *
     * @return the value removed, or <code>null</code> if nothing was removed
     */
    public T remove (int x, int y, T value)
    {
        // see if there's a chain
        int key = CoordUtil.getCoord(x, y);
        int idx = getHashIndex(x, y);
        Entry<T> entry = _entries[idx];
        if (entry == null) {
            return null;
        } else if (entry.key == key && value.equals(entry.value)) {
            _entries[idx] = entry.next;
            _size--;
            _modcount++;
            return value;
        }

        // follow the chain
        for (; entry.next != null; entry = entry.next) {
            if (entry.next.key == key && value.equals(entry.next.value)) {
                entry.next = entry.next.next;
                _size--;
                _modcount++;
                return value;
            }
        }
        return null;
    }

    /**
     * Removes all the elements at the specified coordinates.
     */
    public void removeAll (int x, int y)
    {
        // see if there's a chain
        int key = CoordUtil.getCoord(x, y);
        int idx = getHashIndex(x, y);
        Entry<T> entry = _entries[idx];
        if (entry == null) {
            return;
        } 

        // remove all the initial elements in the chain with matching keys
        for (; entry != null && entry.key == key; entry = entry.next) {
            _entries[idx] = entry.next;
            _size--;
            _modcount++;
        }

        // then remove any remaining elements in the chain
        while (entry != null && entry.next != null) {
            if (entry.next.key == key) {
                entry.next = entry.next.next;
                _size--;
                _modcount++;
            } else {
                entry = entry.next;
            }
        }
    }
}
