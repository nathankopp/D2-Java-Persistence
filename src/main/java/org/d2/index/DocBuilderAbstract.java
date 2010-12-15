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
package org.d2.index;

import java.lang.reflect.Method;
import java.util.Collection;

import org.d2.IdFinder;
import org.d2.annotations.D2Id;
import org.d2.annotations.D2Indexed;
import org.nkts.util.Util;



public abstract class DocBuilderAbstract implements DocBuilder
{
    public abstract void addIdField(String field, Object doc);
    public abstract void addField(String fieldName, String field, Object doc, boolean store, boolean analyze);
    public abstract void addStoredAnalyzedField(String fieldName, String field, Object doc);
    public abstract void addStoredUnanalyzedField(String fieldName, String field, Object doc);
    public abstract void addNonstoredAnalyzedField(String fieldName, String field, Object doc);
    
    public abstract Object createDoc();
    public abstract String getMagicBlankValue();
    
    
    public Object toDocument(Object obj)
    {
        try
        {
            Object doc = createDoc();
    
            addIdField(IdFinder.getId(obj).toString(), doc);
    
            Class<?> clazz = obj.getClass();
            while(!clazz.getSimpleName().equals("Object"))
            {
                for(java.lang.reflect.Field f : clazz.getDeclaredFields())
                {
                    if(f.isAnnotationPresent(D2Indexed.class))
                    {
                        f.setAccessible(true);
                        Object val = f.get(obj);
                        
                        D2Indexed a = f.getAnnotation(D2Indexed.class);

                        String fieldName = determineFieldName(f, a);
                        String valStr = getValAsString(val, a);
                        
                        addField(fieldName, valStr, doc, a.store(), a.analyzed());
                    }
                    else if (f.isAnnotationPresent(D2Id.class))
                    {
                        f.setAccessible(true);
                        Object val = f.get(obj);
                        
                        String fieldName = f.getName();
                        String valStr = convertToSingleToken(val, null);
                        
                        addField(fieldName, valStr, doc, true, false);
                    }
                }
                for(java.lang.reflect.Method m : clazz.getDeclaredMethods())
                {
                    if(m.isAnnotationPresent(D2Indexed.class))
                    {
                        m.setAccessible(true);
                        Object val = m.invoke(obj);

                        D2Indexed a = m.getAnnotation(D2Indexed.class);

                        String fieldName = determineFieldName(m, a);
                        String valStr = getValAsString(val, a);
                        
                        addField(fieldName, valStr, doc, a.store(), a.analyzed());
                    }
                }
                clazz = clazz.getSuperclass();
            }
            
            return doc;
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }

    private String determineFieldName(java.lang.reflect.Field f, D2Indexed a)
    {
        String fieldName = a.name();
        if(Util.isBlank(fieldName)) fieldName=f.getName();
        return fieldName;
    }
    
    private String determineFieldName(java.lang.reflect.Method m, D2Indexed a)
    {
        String fieldName = a.name();
        if(Util.isBlank(fieldName)) fieldName=m.getName();
        if(fieldName.startsWith("get")) fieldName = fieldName.substring(3,4).toLowerCase()+fieldName.subSequence(4, fieldName.length());
        else if(fieldName.startsWith("is")) fieldName = fieldName.substring(2,3).toLowerCase()+fieldName.subSequence(3, fieldName.length()); 
        return fieldName;
    }

    private String getValAsString(Object val, D2Indexed a)
    {
        if(val instanceof Collection<?>)
        {
            String valStr = "";
            for(Object val2 : (Collection<?>)val)
            {
                valStr += getValAsString(val2, a) + " ";
            }
            return valStr.trim();
        }
        else
        {
            return convertToSingleToken(getSingleValueAsString(val, a), a);
        }
    }

    public String convertToSingleToken(Object val, D2Indexed a)
    {
        if(Util.isBlank(val))
        {
            if(a!=null && a.analyzed())
            {
                return "";
            }
            else
            {
                return getMagicBlankValue();
            }
        }
        else
        {
            return val.toString();
        }
    }

    private String getSingleValueAsString(Object val, D2Indexed a)
    {
        if(val==null) return null;
        if(a.findId())
        {
            return IdFinder.getId(val);
        }
        else if(!Util.isBlank(a.indexValueGetter()))
        {
            try
            {
                Method m = val.getClass().getMethod(a.indexValueGetter());
                Object val2 = m.invoke(val);
                if(val2!=null) return val2.toString();
                return null;
            }
            catch(Exception e)
            {
                throw Util.wrap(e);
            }
        }
        else
        {
            return convertToSingleToken(val, a);
        }
    }
}
