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
package org.d2.plugins.h2;

import java.sql.Connection;

import org.d2.Bucket;
import org.d2.pluggable.StorageSystem;
import org.d2.plugins.jdbc.AbstractJdbcStorageSystem;


public class H2StorageSystem extends AbstractJdbcStorageSystem implements StorageSystem
{
    public H2StorageSystem(Bucket bucket, Connection conn)
    {
        super(bucket, conn);
    }

    @Override
    public String getSeqNextVal(String entityName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSequenceIfMore(String name, String value)
    {
        // TODO Auto-generated method stub

    }
    
    @Override
    public boolean isIdValid(String id)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected String getCreateTableStatement()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void acquireReadLock(String id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void acquireWriteLock(String id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void releaseReadLock(String id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void releaseWriteLock(String id)
    {
        // TODO Auto-generated method stub
        
    }

}
