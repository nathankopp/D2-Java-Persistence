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
package org.d2.test.basic;

import java.io.File;
import java.util.List;

import org.d2.Bucket;
import org.d2.D2;
import org.d2.D2Dao;
import org.d2.D2Impl;
import org.d2.context.D2Context;
import org.d2.pluggable.IndexerFactory;
import org.d2.pluggable.StorageFactory;
import org.d2.plugins.localfile.LocalFileStorageFactory;
import org.d2.plugins.lucene.LuceneIndexerFactory;
import org.d2.plugins.xstream.XStreamSerializerFactory;
import org.d2.query.D2QueryBuilder;
import org.d2.query.Occurs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nkts.util.Util;


public class TestD2NumericRange extends Assert
{
    StorageFactory storage;
    IndexerFactory indexer;
    
    D2 d2;
    
    @Before
    public void setup()
    {
        storage = new LocalFileStorageFactory("testdb");
        indexer = new LuceneIndexerFactory("testdb");
        d2 = new D2Impl(storage, indexer, new XStreamSerializerFactory());
        d2.registerBucket(new Bucket(Location.class));
    }

    
    @Test
    public void testRangeSearch()
    {
        D2Context context = new D2Context();
        
        D2Dao<Location> dao = new D2Dao<Location>(d2, Location.class, context);
        
        Location loc = new Location();
        loc.setMinLat(10.0);
        loc.setMaxLat(10.0);

        dao.save(loc);
        assertEquals(loc.getId(),"1");
        
        double lat = 10.5;
        double rad = 1.0;

        D2QueryBuilder qB = new D2QueryBuilder();
        qB.range("minLat", null,(lat+rad), Occurs.MUST);
        qB.range("maxLat", (lat-rad),null, Occurs.MUST);
        
        List<Location> locs = dao.loadForQuery(qB.getQuery());
        Assert.assertEquals(1,locs.size());
        
    }
    

    @After
    public void cleanup()
    {
        try
        {
            d2.close();
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
        try
        {
            Util.deleteDirectory(new File("testdb"));
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

}
