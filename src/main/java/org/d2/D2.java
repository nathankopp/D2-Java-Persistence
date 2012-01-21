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
package org.d2;

import java.util.Date;
import java.util.List;

import org.d2.context.D2Context;
import org.d2.pluggable.IndexerFactory;
import org.d2.pluggable.StorageFactory;

import com.thoughtworks.xstream.XStream;

public interface D2
{

    // ====================================================
    // logic
    // ====================================================
    void registerBucket(Bucket bucket);

    void save(Object obj, D2Context context);

    <T> List<T> loadAll(Class<T> clazz, D2Context context);

    void deleteById(Class<?> clazz, String id, D2Context context);

    <T> T loadById(Class<T> clazz, String id, D2Context context);
    
    public void realizeObject(Object obj, D2Context context);

    void reindexAll(Class<? extends Object> clazz, D2Context context);

    Bucket prepareXStreamAndFindBucket(Class<?> clazz, XStream xs, Date now, D2Context context, Operation operation);
    
    void close();
    
    void resetAllIndexLocks();
    
    // ====================================================
    // getters/setters
    // ====================================================
    List<Bucket> getBuckets();

    void setBuckets(List<Bucket> buckets);

    StorageFactory getDefaultStorageFactory();
    IndexerFactory getDefaultIndexerFactory();

}
