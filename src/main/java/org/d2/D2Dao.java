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
import java.util.List;

import org.d2.context.D2Context;
import org.d2.query.D2Query;
import org.d2.query.D2QueryBuilder;
import org.d2.query.Occurs;


public class D2Dao<T> implements BasicDao<T>
{
    protected D2 d2;
    protected Class<T> clazz;
    protected D2Context context;
    
    public D2Dao(D2 d2, Class<T> clazz, D2Context context)
    {
        super();
        this.d2 = d2;
        this.clazz = clazz;
        this.context = context;
    }

    
	public List<T> loadAll()
    {
        return d2.loadAll(clazz, context);
    }

    public T loadById(String id)
    {
        return d2.loadById(clazz, id, context);
    }

    public Object loadById2(String id)
    {
        return loadById(id);
    }

    public void save(T obj)
    {
        d2.save(obj, context);
    }
    
    public void deleteById(String id)
    {
        d2.deleteById(clazz, id, context);
    }
    
    
    public T loadOneForQuery(D2Query query)
    {
        List<T> list = loadForQuery(query);
        if(list.size()==0) return null;
        if(list.size()>1) throw new RuntimeException("Expected to find one, but found "+list.size()+" for "+query.toString());
        return list.get(0);
    }

    
    public List<T> loadForQuery(D2Query query)
    {
        Operation operation = new Operation();
        Bucket bucket = d2.prepareSerializerAndFindBucket(clazz, null, null, context, operation);
        List<T> out = new ArrayList<T>();
        List<String> ids = bucket.getIndexer().findIdByQuery(query);
        for(String id : ids)
        {
            T obj = loadById(id);
            if(obj!=null)
            {
                boolean match = true;
//                for(Filter f : query.getFilters())
//                {
//                    if(!f.matches(obj))
//                    {
//                        match = false;
//                        break;
//                    }
//                }
                if(match) out.add(obj);
            }
        }
        return out;
    }

    
    
    
    public List<T> loadForQueryStr(String defaultField, String queryStr)
    {
        D2QueryBuilder qB = new D2QueryBuilder();
        qB.parse(defaultField, queryStr);
        return loadForQuery(qB.getQuery());
    }
    
    public List<T> loadForTerms(String... fields)
    {
        D2QueryBuilder qB = new D2QueryBuilder();
        for(int i=0; i<fields.length/2; i+=2)
        {
            qB.exact(fields[i],fields[i+1],Occurs.MUST);
        }
        return loadForQuery(qB.getQuery());
    }
    
    public List<T> findByFieldQueryStr(String fieldName, String fieldValue)
    {
        String queryStr = fieldName+":("+fieldValue+")";
        return (List<T>)loadForQueryStr(fieldName, queryStr);
    }
    
    public void reindexAll()
    {
        d2.reindexAll(clazz, context);
    }

}
