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
package org.d2.test.query;

import java.io.File;

import org.d2.Bucket;
import org.d2.D2;
import org.d2.D2Dao;
import org.d2.D2Impl;
import org.d2.context.D2Context;
import org.d2.pluggable.IndexerFactory;
import org.d2.pluggable.StorageFactory;
import org.d2.plugins.localfile.LocalFileStorageFactory;
import org.d2.plugins.lucene.LuceneIndexerFactory;
import org.d2.query.D2QueryBuilder;
import org.d2.serialize.XStreamSerializerFactory;
import org.d2.test.basic.Person;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nkts.util.Util;


public class TestD2Query extends Assert
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
        d2.registerBucket(new Bucket(Person.class));
    }

    
    @Test
    public void testTestOnePerson()
    {
        D2Context context = new D2Context();
        
        D2Dao<Person> dao = new D2Dao<Person>(d2, Person.class, context);
        
        Person person = new Person("First","Last");
        dao.save(person);
        assertEquals(person.getId(),"1");

        Person person2 = dao.loadOneForQuery(D2QueryBuilder.start().exact("firstName", person.getFirstName()).getQuery());
        assertNotNull(person2);
        assertEquals(person2.getFirstName(),"First");
        assertEquals(person2.getLastName(),"Last");
        assertEquals(person2.getId(),"1");

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
