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
package org.d2.example;

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
import org.nkts.util.Util;


public class Example1
{
    public static void main(String[] args)
    {
        StorageFactory storage = new LocalFileStorageFactory("testdb");
        IndexerFactory indexer = new LuceneIndexerFactory("testdb");
        D2 d2 = new D2Impl(storage, indexer, new XStreamSerializerFactory());
        d2.registerBucket(new Bucket(Person.class));
        
        D2Context context1 = new D2Context();
        D2Dao<Person> dao = new D2Dao<Person>(d2, Person.class, context1);
        
        dao.save(new Person("Nathan","Kopp"));
        dao.save(new Person("Lael","Watkins"));
        dao.save(new Person("John","Smith"));
        dao.save(new Person("Jane","Smith"));
        
        D2QueryBuilder qB = new D2QueryBuilder();
        qB.exact("firstName", "Nathan", Occurs.MUST);
        Person person = dao.loadOneForQuery(qB.getQuery());
        
        System.out.println("Found person: " + person.firstName + " " + person.lastName);
        
        List<Person> theSmiths = dao.loadForQuery(D2QueryBuilder.start().exact("lastName", "Smith").getQuery());
        
        for(Person p : theSmiths)
        {
            System.out.println("Found person: " + p.firstName + " " + p.lastName);
        }
        
        d2.close();
        
        Util.deleteDirectory(new File("testdb"));
    }
}
