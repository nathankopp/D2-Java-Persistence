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
package org.d2.plugins.jdbc;

import java.util.Collection;
import java.util.List;

import org.d2.index.DocBuilderAbstract;
import org.d2.pluggable.Indexer;
import org.d2.query.D2Query;


public class AbstractJdbcIndexer implements Indexer
{
    private AbstractJdbcStorageSystem storage;
    
    public AbstractJdbcIndexer(AbstractJdbcStorageSystem storage)
    {
        super();
        this.storage = storage;

        // check table structure (based on indexed fields), alter table if necessary
    }

    public void close()
    {
        // TODO Auto-generated method stub

    }

    public List<String> findIdByQuery(D2Query query)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public DocBuilderAbstract getDocBuilder()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void indexObject(Object obj)
    {
        // TODO Auto-generated method stub

    }

    public void rebuildIndex(Collection<Object> objList)
    {
        // TODO Auto-generated method stub

    }

    public void deleteDocument(String id)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resetLocks()
    {
        // TODO Auto-generated method stub
        
    }

}
