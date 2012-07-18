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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nkts.util.Util;


public class TestD2Basic extends Assert
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

        Person person2 = dao.loadById("1");
        assertNotNull(person2);
        assertEquals(person2.getFirstName(),"First");
        assertEquals(person2.getLastName(),"Last");
        assertEquals(person2.getId(),"1");

        Person person3 = dao.loadById("1");
        assertTrue(person==person2);
        assertTrue(person==person3);
        
        D2Context context2 = new D2Context();
        D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);
        Person person4 = dao2.loadById("1");
        assertTrue(person!=person4);
        
    }
    
    @Test
    public void testTestPersonWithRelationshipsMultipleContexts()
    {
        D2Context context = new D2Context();
        D2Dao<Person> dao = new D2Dao<Person>(d2, Person.class, context);
        
        Person john = new Person("John","Smith");
        dao.save(john);
        assertEquals(john.getId(),"1");

        Person jane = new Person("Jane","Smith");
        dao.save(jane);
        assertEquals(jane.getId(),"2");

        Person tim = new Person("Tim","Smith");
        dao.save(tim);
        assertEquals(tim.getId(),"3");

        Person layla = new Person("Layla","Smith");
        dao.save(layla);
        assertEquals(layla.getId(),"4");

        // ==========================================
        D2Context context2 = new D2Context();
        D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);

        Person john2 = dao2.loadById("1");
        assertNotNull(john2);
        assertEquals(john2.getFirstName(),"John");
        assertEquals(john2.getLastName(),"Smith");
        assertNull(john2.getChildren());
        assertNull(john2.getSpouse());
        
        // ==========================================

        john2.setSpouse(jane);
        dao2.save(john2);
        
        assertNull(john.getSpouse());
        
        // ==========================================
        D2Context context3 = new D2Context();
        D2Dao<Person> dao3 = new D2Dao<Person>(d2, Person.class, context3);
        
        Person john3 = dao3.loadById("1");
        assertNotNull(john3);
        
        assertEquals(john3.getSpouse().getFirstName(),"Jane");
        
        // ==========================================
        john3.addChild(tim);
        john3.addChild(layla);
        dao3.save(john3);
        
        // ==========================================
        D2Context context4 = new D2Context();
        D2Dao<Person> dao4 = new D2Dao<Person>(d2, Person.class, context4);
        
        Person john4 = dao4.loadById("1");
        assertNotNull(john4);
        
        assertEquals(john4.getChildren().size(),2);
        assertEquals(john4.getChildren().get(0).getFirstName(),"Tim");
        
        // ==========================================
        john4.getChildren().get(1).setLastName("Williams");
        dao4.save(john4);
        assertEquals(layla.getLastName(),"Smith");
        Person layla1b = dao.loadById("4");
        assertTrue(layla==layla1b);
        assertEquals(layla.getLastName(),"Smith");
        
        // ==========================================
        dao4.save(john4.getChildren().get(1));
        assertEquals(layla.getLastName(),"Smith");
        Person layla1c = dao.loadById("4");
        assertTrue(layla==layla1c);
        assertEquals(layla.getLastName(),"Williams");
        
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
