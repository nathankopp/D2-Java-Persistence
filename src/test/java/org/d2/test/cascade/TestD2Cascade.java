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
package org.d2.test.cascade;

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
import org.nkts.util.Util;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test(groups={"slow"})
public class TestD2Cascade extends Assert
{
    StorageFactory storage;
    IndexerFactory indexer;
    
    D2 d2;
    
    @BeforeMethod
    public void setup()
    {
        storage = new LocalFileStorageFactory("testdb");
        indexer = new LuceneIndexerFactory("testdb");
        d2 = new D2Impl(storage, indexer, new XStreamSerializerFactory());
        d2.registerBucket(new Bucket(Person2.class));
    }

    @Test
    public void testCascadeSave()
    {
        D2Context context1 = new D2Context();
        D2Dao<Person2> dao1 = new D2Dao<Person2>(d2, Person2.class, context1);

        D2Context context2 = new D2Context();
        D2Dao<Person2> dao2 = new D2Dao<Person2>(d2, Person2.class, context2);

        // ==========================================
        Person2 john = new Person2("John","Smith");
        dao1.save(john);
        assertEquals(john.getId(),"1");

        Person2 jane = new Person2("Jane","Johnson");
        dao1.save(jane);
        assertEquals(jane.getId(),"2");

        // ==========================================
        Person2 john2 = dao2.loadById("1");
        assertNotNull(john2);
        assertEquals(john2.getFirstName(),"John");
        assertEquals(john2.getLastName(),"Smith");
        assertNull(john2.getChildren());
        assertNull(john2.getSpouse());
        
        // ==========================================
        Person2 jane2 = dao2.loadById("2");
        john2.setSpouse(jane2);
        john2.getSpouse().setLastName(john2.getLastName());
        dao2.save(john2);
        
        // ==========================================
        D2Context context3 = new D2Context();
        D2Dao<Person2> dao3 = new D2Dao<Person2>(d2, Person2.class, context3);
        
        Person2 john3 = dao3.loadById("1");
        assertNotNull(john3);
        
        assertEquals(john3.getSpouse().getFirstName(),"Jane");
        assertEquals(john3.getSpouse().getLastName(),"Smith");
        
        // ---------------------------
        // reload in context1... should show last name
        // first: it jane's last name shoudl still be "Johnson" in memory
        assertEquals(jane.getLastName(),"Johnson");
        
        // reload john... it shoudl also reload jane
        john = dao1.loadById(john.getId());
        
        // ensure that jane got reloaded properly, and attached to john properly
        assertEquals(john.getSpouse().getLastName(),"Smith");
        assertEquals(jane.getLastName(),"Smith");
    }

    @Test
    public void testLazyLoadCollection()
    {
        D2Context context1 = new D2Context();
        D2Dao<Person2> dao1 = new D2Dao<Person2>(d2, Person2.class, context1);

        D2Context context2 = new D2Context();
        D2Dao<Person2> dao2 = new D2Dao<Person2>(d2, Person2.class, context2);

        // ==========================================
        Person2 john = new Person2("John","Smith");
        dao1.save(john);
        assertEquals(john.getId(),"1");
        
        Person2 tim = new Person2("Tim","Smith");
        Person2 layla = new Person2("Layla","Smith");
        john.addChild(tim);
        john.addChild(layla);
        
        // NOTE: ALWAYS forces cascade save because children aren't saved yet
        dao1.save(john);
        
        // ==========================================
        Person2 john2 = dao2.loadById("1");
        assertEquals(john2.getChildren().size(),2);

        assertNull(john2.getChildren().get(0).getFirstName());
        assertNull(john2.getChildren().get(1).getFirstName());
        
        // -----------
        d2.realizeObject(john2.getChildren().get(0), context2);
        assertEquals(john2.getChildren().get(0).getFirstName(),"Tim");
        assertNull(john2.getChildren().get(1).getFirstName());
        
        // -----------
        dao2.loadById(layla.getId());
        assertEquals(john2.getChildren().get(1).getFirstName(),"Layla");
        
        // assert continued separation of the contexts
        assertTrue(john!=john2);
        assertTrue(tim!=john2.getChildren().get(0));
        assertTrue(layla!=john2.getChildren().get(1));
    }

    
    @AfterMethod
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
