/*******************************************************************************
 * Copyright 2010 Nathan Kopp
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.nkts.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * See http://archive.devx.com/java/free/articles/Kabutz01/Kabutz01-2.asp
 * 
 * @author Dr. Heinz Kabutz
 *
 * @param <K>
 * @param <V>
 */
public class SoftHashMap<K,V> extends AbstractMap<K,V>
{
    /** The internal HashMap that will hold the SoftReference. */
    private final Map<K,SoftValue> hash = new HashMap<K,SoftValue>();
    /** The number of "hard" references to hold internally. */
    private final int HARD_SIZE;
    /** The FIFO list of hard references, order of last access. */
    private final LinkedList hardCache = new LinkedList();
    /** Reference queue for cleared SoftReference objects. */
    private final ReferenceQueue queue = new ReferenceQueue();

    public SoftHashMap()
    {
        this(100);
    }

    public SoftHashMap(int hardSize)
    {
        HARD_SIZE = hardSize;
    }

    public V get(Object key)
    {
        V result = null;
        // We get the SoftReference represented by that key
        SoftReference<V> soft_ref = (SoftReference<V>) hash.get(key);
        if (soft_ref != null)
        {
            // From the SoftReference we get the value, which can be
            // null if it was not in the map, or it was removed in
            // the processQueue() method defined below
            result = soft_ref.get();
            if (result == null)
            {
                // If the value has been garbage collected, remove the
                // entry from the HashMap.
                hash.remove(key);
            }
            else
            {
                // We now add this object to the beginning of the hard
                // reference queue. One reference can occur more than
                // once, because lookups of the FIFO queue are slow, so
                // we don't want to search through it each time to remove
                // duplicates.
                hardCache.addFirst(result);
                if (hardCache.size() > HARD_SIZE)
                {
                    // Remove the last entry if list longer than HARD_SIZE
                    hardCache.removeLast();
                }
            }
        }
        return result;
    }

    /**
     * We define our own subclass of SoftReference which contains not only the
     * value but also the key to make it easier to find the entry in the HashMap
     * after it's been garbage collected.
     */
    private class SoftValue extends SoftReference<V>
    {
        private final K key; // always make data member final

        /**
         * Did you know that an outer class can access private data members and
         * methods of an inner class? I didn't know that! I thought it was only
         * the inner class who could access the outer class's private
         * information. An outer class can also access private members of an
         * inner class inside its inner class.
         */
        private SoftValue(V k, K key, ReferenceQueue<V> q)
        {
            super(k, q);
            this.key = key;
        }
    }

    /**
     * Here we go through the ReferenceQueue and remove garbage collected
     * SoftValue objects from the HashMap by looking them up using the
     * SoftValue.key data member.
     */
    private void processQueue()
    {
        SoftValue sv;
        while ((sv = (SoftValue) queue.poll()) != null)
        {
            hash.remove(sv.key); // we can access private data!
        }
    }

    /**
     * Here we put the key, value pair into the HashMap using a SoftValue
     * object.
     */
    public V put(Object key, Object value)
    {
        processQueue(); // throw out garbage collected values first
        SoftValue v = hash.put((K)key, new SoftValue((V)value, (K)key, queue));
        if(v!=null) return v.get();
        else return null;
    }

    public V remove(Object key)
    {
        processQueue(); // throw out garbage collected values first
        SoftValue v = hash.remove(key);
        if(v!=null) return v.get();
        else return null;
    }

    public void clear()
    {
        hardCache.clear();
        processQueue(); // throw out garbage collected values
        hash.clear();
    }

    public int size()
    {
        processQueue(); // throw out garbage collected values first
        return hash.size();
    }

    public Set entrySet()
    {
        // no, no, you may NOT do that!!! GRRR
        throw new UnsupportedOperationException();
    }
}
