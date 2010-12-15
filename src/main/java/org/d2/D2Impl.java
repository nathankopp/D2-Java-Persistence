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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.d2.annotations.D2Entity;
import org.d2.context.D2Context;
import org.d2.pluggable.IndexerFactory;
import org.d2.pluggable.StorageFactory;
import org.d2.serialize.D2AwareReflectionConverter;
import org.d2.serialize.D2XmlEntityConverter;
import org.d2.serialize.D2XmlIgnoreClassConverter;
import org.d2.serialize.D2XmlVersionedConverter;
import org.nkts.util.StringVisitor;
import org.nkts.util.Util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class D2Impl implements D2
{
    private List<Bucket> buckets = new ArrayList<Bucket>();
    
    private StorageFactory defaultStorageFactory;
    private IndexerFactory defaultIndexerFactory;
    
    public D2Impl(StorageFactory defaultStorageFactory, IndexerFactory defaultIndexerFactory)
    {
        this.defaultStorageFactory = defaultStorageFactory;
        this.defaultIndexerFactory = defaultIndexerFactory;
    }
    
    // ====================================================
    // logic
    // ====================================================
    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#registerBucket(com.pitcru.persistence.d2.D2Bucket)
     */
    public void registerBucket(Bucket bucket)
    {
        for(Bucket b : buckets)
        {
            if(b.getClazz()==bucket.getClazz()) return;
        }
        bucket.setD2(this);
        buckets.add(bucket);
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#save(com.pitcru.persistence.d2.Object)
     */
    public void save(Object obj, D2Context context)
    {
        Operation operation = new Operation();
        // FIXME - nondeterministic
        Date now = new Date();

        context = useObjectContextIfPossible(obj, context);
        saveInternal(obj, context, operation, now);
    }

    private void saveInternal(Object obj, D2Context context, Operation operation, Date now)
    {
        XStream xs = new XStream();
        Bucket bucket = prepareXStreamAndFindBucket(obj.getClass(), xs, new Date(), context, operation);
        assignId(obj, bucket.getClazz(), bucket);
        String xmlStr = xs.toXML(obj);

        String id = IdFinder.getId(obj);
        bucket.getStorage().acquireWriteLock(id);
        try
        {
            bucket.getStorage().saveDocument(id, xmlStr, now);
            setMetadata(obj, xmlStr, now, LoadStatus.LOADED, context);
            
            if(bucket.getIndexer()!=null)
            {
                bucket.getIndexer().indexObject(obj);
            }
        }
        finally
        {
            bucket.getStorage().releaseWriteLock(id);
        }
        
        context.registerInstanceToCache(bucket, obj);

        Object objectToSave;
        while((objectToSave = operation.pollCascadeSave())!=null)
        {
            saveInternal(objectToSave, context, operation, now);
        }
    }

    private void setMetadata(Object obj, String xml, Date now, LoadStatus status, D2Context context)
    {
        D2Metadata md = IdFinder.getMd(obj);
        if(md==null)
        {
            md = new D2Metadata();
            IdFinder.setMd(obj, md);
        }
        if(status!=null) md.setStatus(status);
        if(now!=null) md.setSaveTimestamp(now);
        if(xml!=null) md.setLoadedXml(xml);
        if(context!=null) md.setContext(context);
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#loadAll(java.lang.Class)
     */
    public <T> List<T> loadAll(final Class<T> clazz, final D2Context context)
    {
        final Operation operation = new Operation();
        final List<T> out = new ArrayList<T>();
        
        final Bucket bucket = prepareXStreamAndFindBucket(clazz, null, null, context, operation);
        
        bucket.getStorage().eachId(new StringVisitor(){
            @Override
            public void visit(String id)
            {
                // it's inefficient to prepare a new XS for each load... but right now we need to, because we must reset the
                // converter for this type... so that it will unmarshall the root-level object properly 
                XStream xs = new XStream();
                prepareXStreamAndFindBucket(clazz, xs, null, context, operation);
                T obj = loadOneObject(clazz, id, xs, bucket, context, true, operation);
                out.add(obj);
            }
        });
        
        return out;
    }
    
    public void reindexAll(final Class<? extends Object> clazz, final D2Context context)
    {
        final Operation operation = new Operation();
        final Bucket bucket = prepareXStreamAndFindBucket(clazz, null, null, context, operation);
        if(bucket.getIndexer()==null) throw new RuntimeException("Cannot index "+clazz.getSimpleName()+" because indexer is null");

        bucket.getStorage().eachId(new StringVisitor(){
            @Override
            public void visit(String id)
            {
                // it's inefficient to prepare a new XS for each load... but right now we need to, because we must reset the
                // converter for this type... so that it will unmarshall the root-level object properly 
                XStream xs = new XStream();
                prepareXStreamAndFindBucket(clazz, xs, null, context, operation);
                Object obj = loadOneObject(clazz, id, xs, bucket, context, true, operation);
                
                bucket.getIndexer().indexObject(obj);
            }
        });
        
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#deleteById(java.lang.Class, java.lang.Long)
     */
    public void deleteById(Class<? extends Object> clazz, String id, D2Context context)
    {
        Operation operation = new Operation();
        try
        {
            Bucket bucket = prepareXStreamAndFindBucket(clazz, null, null, context, operation);
    
            bucket.getStorage().deleteDocument(id);
            bucket.getIndexer().deleteDocument(id);
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#loadById(java.lang.Class, java.lang.Long)
     */
    public <T> T loadById(Class<T> clazz, String id, D2Context context)
    {
        Operation operation = new Operation();
        try
        {
            XStream xs = new XStream();
            Bucket bucket = prepareXStreamAndFindBucket(clazz, xs, null, context, operation);
    
            return loadOneObject(clazz, id, xs, bucket, context, true, operation);
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }

    public <T> T loadCachedOnlyById(Class<T> clazz, String id, D2Context context, boolean createStandin)
    {
        Operation operation = new Operation();
        try
        {
            XStream xs = new XStream();
            Bucket bucket = prepareXStreamAndFindBucket(clazz, xs, null, context, operation);
    
            return getCachedObjectOrStandin(clazz, id, bucket, context, createStandin);
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }

    private <T> T loadOneObject(Class<T> clazz, String id, XStream xs, Bucket bucket, D2Context context, boolean forceReload, Operation operation)
    {
        T object = getCachedObjectOrStandin(clazz, id, bucket, context, false);
        
        if(object==null || IdFinder.getMd(object).isStandin() || forceReload)
        {
            object = realizeObject(clazz, id, xs, bucket, context, object);
            if(object==null) return null;

//            TreeWalker.walk(new LoadStandins(this, context), object);
            T objectToLoad;
            while((objectToLoad = (T)operation.pollCascadeLoads())!=null)
            {
                realizeObject(objectToLoad, context, operation);
            }
            
        }
        return object;
    }
    
    public void realizeObject(Object obj, D2Context context)
    {
        realizeObject(obj, context, new Operation());
    }

    private void realizeObject(Object obj, D2Context context, Operation operation)
    {
        Class clazz = obj.getClass();
        String id = IdFinder.getId(obj);
        try
        {
            XStream xs = new XStream();
            Bucket bucket = prepareXStreamAndFindBucket(clazz, xs, null, context, operation);
    
            loadOneObject(clazz, id, xs, bucket, context, true, operation);
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }

    private <T> T realizeObject(Class<T> clazz, String id, XStream xs, Bucket bucket, D2Context context, T object)
    {
        String xmlStr = null;
        bucket.getStorage().acquireReadLock(id);
        try
        {
            xmlStr = bucket.getStorage().loadDocument(id);
        }
        finally
        {
            bucket.getStorage().releaseReadLock(id);
        }
        if(xmlStr==null) return null;
        
        if(object==null) object = clazz.cast(context.createAndRegisterStandin(bucket, clazz, id));
        xs.fromXML(xmlStr, object);
        
        setMetadata(object, xmlStr, null, LoadStatus.LOADED, context);
        return object;
    }

    private <T> T getCachedObjectOrStandin(Class<T> clazz, String id, Bucket bucket, D2Context context, boolean createStandin)
    {
        if(!bucket.getStorage().isIdValid(id)) return null;
        
        Object cached = context.lookInCache(bucket, id);
        if(cached!=null) return clazz.cast(cached);
        
        if(!createStandin) return null;
      
        T standin = clazz.cast(context.createAndRegisterStandin(bucket, clazz, id));
        
        return standin;
    }

    /**
     * TODO move this to LocalFileStorage
     * 
     * @param obj
     * @param bucketClass
     * @param bucket
     */
    private void assignId(Object obj, Class bucketClass, Bucket bucket)
    {
        if(bucket==null) throw new RuntimeException("bucket is null");
        if(bucket.getStorage()==null) throw new RuntimeException("bucket's storage system is null");
        if(IdFinder.getMd(obj).isNew())
        {
            String id = bucket.getStorage().getSeqNextVal(bucketClass.getSimpleName());
            IdFinder.setId(obj, id);
        }
        else
        {
            bucket.getStorage().setSequenceIfMore(obj.getClass().getSimpleName(), IdFinder.getId(obj));
        }
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2#prepareXStreamAndFindBucket(java.lang.Class, com.thoughtworks.xstream.XStream)
     */
    public Bucket prepareXStreamAndFindBucket(Class<?> clazz, XStream xs, Date now, D2Context context, Operation operation)
    {
        if(xs!=null)
        {
            Converter normalObjectConverter = xs.getConverterLookup().lookupConverterForType(Object.class);
            xs.registerConverter(new D2AwareReflectionConverter(xs.getMapper(), xs.getReflectionProvider()));
            xs.registerConverter(new D2XmlVersionedConverter(normalObjectConverter));
            xs.registerConverter(new D2XmlIgnoreClassConverter());
        }
        
        Bucket thisBucket = null;
        for(Bucket bucket : buckets)
        {
            if(bucket.getClazz().isAssignableFrom(clazz))
            {
                if(xs!=null)
                {
                    Converter delegateConverter = xs.getConverterLookup().lookupConverterForType(bucket.getClazz());
                    xs.registerConverter(new D2XmlEntityConverter(this, bucket.getClazz(), delegateConverter, now, context, operation));
                    xs.alias(getAlias(clazz), clazz);
                }
                thisBucket = bucket;
            }
            else
            {
                if(xs!=null)
                {
                    Converter delegateConverter = xs.getConverterLookup().lookupConverterForType(bucket.getClazz());
                    xs.registerConverter(new D2XmlEntityConverter(this, bucket.getClazz(), delegateConverter, context, operation));
//                    xs.registerConverter(bucket.getConverter());
                    xs.alias(getAlias(clazz), clazz);
                }
            }
        }
        return thisBucket;
    }
    

    private String getAlias(Class<?> clazz)
    {
        D2Entity entityAnnotation = clazz.getAnnotation(D2Entity.class);
        return entityAnnotation==null?clazz.getName():entityAnnotation.alias();
    }
    
    private static D2Context useObjectContextIfPossible(Object obj, D2Context context)
    {
        D2Metadata md = IdFinder.getMd(obj);
        if(md!=null && md.getContext()!=null)
        {
            context = md.getContext();
        }
        return context;
    }
    
    
    
    // ====================================================
    // getters/setters
    // ====================================================
    public List<Bucket> getBuckets()
    {
        return buckets;
    }

    public void setBuckets(List<Bucket> buckets)
    {
        this.buckets = buckets;
    }

    public StorageFactory getDefaultStorageFactory()
    {
        return defaultStorageFactory;
    }

    public IndexerFactory getDefaultIndexerFactory()
    {
        return defaultIndexerFactory;
    }

    @Override
    public void close()
    {
        for(Bucket b : buckets)
        {
            b.getIndexer().close();
        }
    }

}
