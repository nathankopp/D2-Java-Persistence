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
package org.d2.test.performance;

import java.io.File;
import java.util.Random;

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
import org.d2.test.basic.Person;
import org.nkts.util.Util;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test(groups={"slow"})
public class TestD2Performance extends Assert
{
    StorageFactory storage;
    IndexerFactory indexer;
    
    D2 d2;
    
    int count = 20;
    
    @BeforeMethod
    public void setup()
    {
        storage = new LocalFileStorageFactory("testdb");
        indexer = new LuceneIndexerFactory("testdb");
        d2 = new D2Impl(storage, indexer, new XStreamSerializerFactory());
        d2.registerBucket(new Bucket(Person.class));
    }

    @Test
    public void testReadPeformance()
    {
        D2Context context1 = new D2Context();
        D2Dao<Person> dao1 = new D2Dao<Person>(d2, Person.class, context1);

        // ==========================================
        Person john = new Person("John","Smith");
        dao1.save(john);
        assertEquals(john.getId(),"1");

        Person jane = new Person("Jane","Johnson");
        dao1.save(jane);
        assertEquals(jane.getId(),"2");

        // ==========================================
        long start = System.currentTimeMillis();
        for(int i=0; i<count; i++)
        {
            D2Context context2 = new D2Context();
            D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);
            
            Person john2 = dao2.loadById("1");
            Person jane2 = dao2.loadById("2");
            assertEquals(john2.getFirstName(),"John");
            assertEquals(jane2.getFirstName(),"Jane");
        }
        long end = System.currentTimeMillis();
        
        System.out.println("read total time: "+(end-start)+"ms ("+((end-start)/(float)count)+"ms/loadById)");
        
        assertTrue((end-start)<150000);
    }

    @Test
    public void testWritePeformance()
    {
        D2Context context1 = new D2Context();
        D2Dao<Person> dao1 = new D2Dao<Person>(d2, Person.class, context1);

        // ==========================================
        Person john = new Person("John","Smith");
        dao1.save(john);
        assertEquals(john.getId(),"1");

        Person jane = new Person("Jane","Johnson");
        dao1.save(jane);
        assertEquals(jane.getId(),"2");

        // ==========================================
        long start = System.currentTimeMillis();
        for(int i=0; i<count; i++)
        {
            dao1.save(john);
            
            D2Context context2 = new D2Context();
            D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);
            Person person2 = dao2.loadOneForQuery(D2QueryBuilder.start().exact("firstName", john.getFirstName()).getQuery());
            assertEquals(person2.getLastName(),john.getLastName());
        }
        long end = System.currentTimeMillis();
        
        System.out.println("write total time: "+(end-start)+"ms ("+((end-start)/(float)count)+"ms/save)");
        
        long storageTime = ((D2Impl)d2).getStorageTime();
        long indexTime = ((D2Impl)d2).getIndexTime();
        System.out.println("storage time: "+(storageTime)+"ms ("+((storageTime)/(float)count)+"ms/save)");
        System.out.println("index time: "+(indexTime)+"ms ("+((indexTime)/(float)count)+"ms/save)");
        
        assertTrue((end-start)<200000);
    }

    @Test
    public void testWritePeformance2()
    {
        D2Context context1 = new D2Context();
        D2Dao<Person> dao1 = new D2Dao<Person>(d2, Person.class, context1);

        D2Context context2 = new D2Context();
        D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);

        Random r = new Random();
        // ==========================================
        long start = System.currentTimeMillis();
        for(int i=0; i<count; i++)
        {
            Person person = new Person(String.valueOf(r.nextInt()),String.valueOf(r.nextInt()));
            dao1.save(person);
            
            Person person2 = dao2.loadOneForQuery(D2QueryBuilder.start().exact("firstName", person.getFirstName()).getQuery());
            assertEquals(person2.getLastName(),person.getLastName());
        }
        long end = System.currentTimeMillis();
        
        System.out.println("write2 total time: "+(end-start)+"ms ("+((end-start)/(float)count)+"ms/save)");
        
        assertTrue((end-start)<800000);
    }
    
    
    public class WriteThreadProcess implements Runnable
    {
        public boolean done = false;
        public long time = 0;
        public Throwable t;
        @Override
        public void run()
        {
            try
            {
                D2Context context1 = new D2Context();
                D2Dao<Person> dao1 = new D2Dao<Person>(d2, Person.class, context1);
    
                D2Context context2 = new D2Context();
                D2Dao<Person> dao2 = new D2Dao<Person>(d2, Person.class, context2);
    
                Random r = new Random();
                // ==========================================
                long start = System.currentTimeMillis();
                for(int i=0; i<count/2; i++)
                {
                    Person person = new Person(String.valueOf(r.nextInt()),String.valueOf(r.nextInt()));
                    dao1.save(person);
                    
                    Person person2 = dao2.loadOneForQuery(D2QueryBuilder.start().exact("firstName", person.getFirstName()).getQuery());
                    Assert.assertNotNull(person2);
                    assertEquals(person2.getLastName(),person.getLastName());
                }
                long end = System.currentTimeMillis();
                time = end-start;
                done = true;
            }
            catch(Throwable t)
            {
                done = true;
                this.t = t;
                t.printStackTrace();
            }
        }
    }
    
    @Test
    public void testWritePeformanceMultiThread() throws Throwable
    {
        WriteThreadProcess p1 = new WriteThreadProcess();
        WriteThreadProcess p2 = new WriteThreadProcess();
        Thread t1 = new Thread(p1);
        Thread t2 = new Thread(p2);
        
        // ==========================================
        long start = System.currentTimeMillis();

        t1.start();
        t2.start();
        
        synchronized(this)
        {
            while(!(p1.done && p2.done))
            {
                try
                {
                    this.wait(10);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        if(p1.t!=null) throw p1.t;
        if(p2.t!=null) throw p2.t;
        

        long end = System.currentTimeMillis();
        
        System.out.println("writeMultiThread total time: "+(end-start)+"ms ("+((end-start)/(float)count)+"ms/save)");
        
        assertTrue((end-start)<800000);
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
