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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.d2.annotations.D2Id;
import org.nkts.util.Util;


public class IdFinder
{
    public static String getId(Object obj)
    {
        if(obj instanceof D2Object) return toString(((D2Object)obj).getId());
        
        return getIdFromAnnotatedField(obj);
    }
    
    public static D2Metadata getMd(Object obj)
    {
        synchronized(obj)
        {
            D2Metadata md = getMetadataFromAnnotatedField(obj, true);
            if(md==null)
            {
                md = new D2Metadata();
                setMd(obj, md);
            }
            return md;
        }
    }
    
    public static D2Metadata getOptionalMd(Object obj)
    {
        D2Metadata md = getMetadataFromAnnotatedField(obj, false);
        return md;
    }
    
    private static String getIdFromAnnotatedField(Object obj)
    {
        try
        {
            Field f = Util.getFirstAnnotatedField(obj.getClass(), D2Id.class);
            if(f!=null) return toString(f.get(obj));
            else
            {
                Method m = Util.getFirstAnnotatedMethod(obj.getClass(), D2Id.class);
                if(m!=null) return toString(m.invoke(obj));
                else throw new RuntimeException("could not find ID field for type "+obj.getClass().toString());
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    private static D2Metadata getMetadataFromAnnotatedField(Object obj, boolean exception)
    {
        try
        {
            Field f = Util.getFirstFieldOfType(obj.getClass(), D2Metadata.class);
            if(f!=null) return (D2Metadata)f.get(obj);
            else
            {
                if(exception)
                    throw new RuntimeException("could not find D2Metadata field");
                else
                    return null;
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }

    private static String toString(Object object)
    {
        if(object==null) return null;
        else return object.toString();
    }

    public static void setId(Object obj, String id)
    {
        if(obj instanceof D2Object)
        {
            ((D2Object)obj).setId(id);
            return;
        }
        
        setIdToAnnotatedField(obj, id);
    }
    
    private static void setIdToAnnotatedField(Object obj, String id)
    {
        try
        {
            Field f = Util.getFirstAnnotatedField(obj.getClass(), D2Id.class);
            if(f!=null) f.set(obj, id);
            else throw new RuntimeException("could not find ID field");
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    public static void setMd(Object obj, D2Metadata md)
    {
        setMdToTypedField(obj, md);
    }
    private static void setMdToTypedField(Object obj, D2Metadata id)
    {
        try
        {
            Field f = Util.getFirstFieldOfType(obj.getClass(), D2Metadata.class);
            if(f!=null) f.set(obj, id);
            else throw new RuntimeException("could not find D2Metadata field");
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    
    

}
