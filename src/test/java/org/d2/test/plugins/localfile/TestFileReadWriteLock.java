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
package org.d2.test.plugins.localfile;

import org.d2.plugins.localfile.FileReadWriteLock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * NOTE: This does NOT do an extensive job testing the FileReadWriteLock class.  :-(
 * Testing it fully would be tricky, especially since it is built for cross-JVM locking!
 * 
 * @author nkopp
 *
 */
@Test(groups={"fast"})
public class TestFileReadWriteLock extends Assert
{
    @BeforeMethod
    public void setup()
    {
        
    }

    @Test
    public void testLock1()
    {
        FileReadWriteLock lock = FileReadWriteLock.find("test");
        lock.acquireReadLock();
        lock.releaseReadLock();
    }

    @Test
    public void testLock2()
    {
        FileReadWriteLock lock = FileReadWriteLock.find("test");
        lock.acquireReadLock();
        lock.acquireReadLock();
        lock.releaseReadLock();
        lock.releaseReadLock();
    }

    @Test
    public void testLock2b()
    {
        FileReadWriteLock lock1 = FileReadWriteLock.find("test");
        FileReadWriteLock lock2 = FileReadWriteLock.find("test");
        lock1.acquireReadLock();
        lock2.acquireReadLock();
        lock2.releaseReadLock();
        lock1.releaseReadLock();
    }

    @Test
    public void testLock3()
    {
        FileReadWriteLock lock = FileReadWriteLock.find("test");
        lock.acquireReadLock();
        try
        {
            // should throw exception... would cause deadlock!
            lock.acquireWriteLock();
            fail();
        }
        catch(RuntimeException e)
        {
            assertTrue(e.getMessage().startsWith("Cannot"));
        }
        lock.releaseReadLock();
    }

    @Test
    public void testLock4()
    {
        FileReadWriteLock lock = FileReadWriteLock.find("test");
        lock.acquireWriteLock();
        try
        {
            // should throw exception... would cause deadlock!
            lock.acquireReadLock();
            fail();
        }
        catch(RuntimeException e)
        {
            assertTrue(e.getMessage().startsWith("Cannot"));
        }
        try
        {
            // should throw exception... would cause deadlock!
            lock.acquireWriteLock();
            fail();
        }
        catch(RuntimeException e)
        {
            assertTrue(e.getMessage().startsWith("Cannot"));
        }
        lock.releaseWriteLock();
    }
    
}
