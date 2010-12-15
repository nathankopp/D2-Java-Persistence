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
package org.d2.plugins.localfile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nkts.util.Util;


/**
 * NOTE: In this implementation, a writelock CANNOT be downgraded to a readlock!
 * 
 * @author nkopp
 *
 */
public class FileReadWriteLock
{
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private File file;
    private FileChannel channel;
    private FileLock fileLock;
    
    private static Map<String, FileReadWriteLock> allLocks = new HashMap<String, FileReadWriteLock>();
    
    public synchronized static FileReadWriteLock find(String name)
    {
        FileReadWriteLock lock = allLocks.get(name);
        if(lock==null)
        {
            lock = new FileReadWriteLock(name);
            allLocks.put(name, lock);
        }
        return lock;
    }
    
    private FileReadWriteLock(String fileName)
    {
        file = new File(fileName);
    }
    
    public synchronized void acquireReadLock()
    {
        if(rwl.getWriteHoldCount()>0) throw new RuntimeException("Cannot acquire read lock - write lock already held by this thread");
        rwl.readLock().lock();
        try
        {
            if(rwl.getReadLockCount()==1 && rwl.getReadHoldCount()==1 && channel==null)
            {
                channel = new RandomAccessFile(file, "rw").getChannel(); 
                fileLock = channel.lock(0L, Long.MAX_VALUE, true);
            }
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
    }

    public synchronized void acquireWriteLock()
    {
        if(rwl.getWriteHoldCount()>0) throw new RuntimeException("Cannot acquire write lock - write lock already held by this thread");
        if(rwl.getReadHoldCount()>0) throw new RuntimeException("Cannot acquire write lock - read lock already held by this thread");
        rwl.writeLock().lock();
        try
        {
            if(rwl.getWriteHoldCount()==1 && channel==null)
            {
                channel = new RandomAccessFile(file, "rw").getChannel(); 
                fileLock = channel.lock(0L, Long.MAX_VALUE, false);
            }
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
    }
    
    public synchronized void releaseReadLock()
    {
        rwl.readLock().unlock();
        if(rwl.getReadLockCount()==0 && rwl.getReadHoldCount()==0)
        {
            try
            {
                fileLock.release();
                channel.close();
                channel = null;
                fileLock = null;
            }
            catch (IOException e)
            {
                throw Util.wrap(e);
            }
        }
    }

    public synchronized void releaseWriteLock()
    {
        rwl.writeLock().unlock();
        if(rwl.getWriteHoldCount()==0 && rwl.getReadLockCount()==0 && rwl.getReadHoldCount()==0)
        {
            try
            {
                fileLock.release();
                channel.close();
                channel = null;
                fileLock = null;
            }
            catch (IOException e)
            {
                throw Util.wrap(e);
            }
        }
    }
}
