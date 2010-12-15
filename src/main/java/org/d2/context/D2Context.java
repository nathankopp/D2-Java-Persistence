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
package org.d2.context;

import java.util.Map;

import org.d2.Bucket;
import org.d2.D2Metadata;
import org.d2.IdFinder;
import org.d2.LoadStatus;
import org.nkts.util.SoftHashMap;
import org.nkts.util.Util;


public class D2Context
{
    protected Map<String, Object> cache = new SoftHashMap<String, Object>();

    public void registerInstanceToCache(Bucket bucket, Object obj)
    {
        getCache().put(bucket.getName()+":"+IdFinder.getId(obj), obj);
    }

    /**
     * Some notes:
     * 
     *  this isn't portable:
     *       Object standin = SilentObjectCreator.create(clazz);
     *  but this requires a no-arg constructor:
     *       Object standin = clazz.newInstance();
     *  so we use this instead: http://code.google.com/p/objenesis/
     *       Object standin = bucket.getInstantiator(clazz).newInstance();
     * 
     * @param bucket
     * @param clazz
     * @param id
     * @return
     */
    public Object createAndRegisterStandin(Bucket bucket, Class<?> clazz, String id)
    {
        try
        {
            D2Metadata md = new D2Metadata();
            md.setContext(this);
            md.setStatus(LoadStatus.STANDIN);
            
            Object standin = bucket.getInstantiator(clazz).newInstance();
            
            IdFinder.setMd(standin, md);
            IdFinder.setId(standin, id);
            
            getCache().put(bucket.getName()+":"+id, standin);

//            System.out.println("registered "+IdFinder.getId(standin)+" in cache");
            return standin;
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public Object lookInCache(Bucket bucket, String id)
    {
//        System.out.println("looking for "+id+" in cache");
        Object cached = getCache().get(bucket.getName()+":"+id);
        if(cached!=null) return cached;
//        System.out.println("NOT found");
        return null;
    }
    
    public void removeFromCache(Bucket bucket, String id)
    {
        getCache().remove(bucket.getName()+":"+id);
    }

    private Map<String, Object> getCache()
    {
        return cache;
    }
}
