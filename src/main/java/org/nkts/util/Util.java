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
package org.nkts.util;


import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.d2.IdFinder;
import org.d2.annotations.D2Indexed;

import com.thoughtworks.xstream.XStream;

public class Util
{
    public static RuntimeException wrap(Throwable e)
    {
        if(e instanceof RuntimeException) return (RuntimeException)e;
        else throw new RuntimeException(e);
    }
    
    public static boolean isBlank(Object o)
    {
        if(o==null) return true;
        if(o.toString().trim().length()==0) return true;
        return false;
    }

    public static boolean oneNotBlank(String a, String b)
    {
        if(!isBlank(a)) return true;
        if(!isBlank(b)) return true;
        return false;
    }

    public static boolean equalsNotNull(Object id, Object id2)
    {
        if(id==null || id2==null) return false;
        return (id.equals(id2));
    }

    public static boolean objEquals(Object id, Object id2)
    {
        if(id==null && id2==null) return true;
        if(id==null || id2==null) return false;
        if(id==id2) return true;
        return (id.equals(id2));
    }

    public static boolean strEqualsIgnoreCase(String id, String id2)
    {
        if(id==null && id2==null) return true;
        if(id==null || id2==null) return false;
        if(id==id2) return true;
        return (id.equalsIgnoreCase(id2));
    }

    public static boolean strEqualsIgnoreCaseNiceBlank(String id, String id2)
    {
        if(isBlank(id) && isBlank(id2)) return true;
        if(isBlank(id) || isBlank(id2)) return false;
        if(id==id2) return true;
        return (id.equalsIgnoreCase(id2));
    }

    public static boolean standardObjEquals(Object obj1, Object obj2)
    {
        if(obj1==null && obj2==null) return true;
        if(obj1==null) return false;
        if(obj2==null) return false;
        if(obj1==obj2) return true;
        if(!(obj1.getClass().getName().equals(obj2.getClass().getName()))) return false;
        return equalsNotNull(IdFinder.getId(obj1),IdFinder.getId(obj2)); // TODO: shoudl this be plain objEquals?  see objWithIdEquals below
    }

    public static <T> T deepClone(T phrase)
    {
        XStream xs = new XStream();
        return (T)xs.fromXML(xs.toXML(phrase));
    }
    
    public static boolean objWithIdEquals(Object obj1, Object obj2)
    {
        if(obj1==null && obj2==null) return true;
        if(obj1==null || obj2==null) return false;
        if(obj1==obj2) return true;
        return objEquals(IdFinder.getId(obj1),IdFinder.getId(obj2));
    }

    public static Field getFirstAnnotatedField(Class clazz, Class annotation)
    {
        while(!clazz.getSimpleName().equals("Object"))
        {
            for(Field f : clazz.getDeclaredFields())
            {
                f.setAccessible(true);
                if(f.isAnnotationPresent(annotation))
                {
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
    
    public static Field getFirstFieldOfType(Class clazz, Class type)
    {
        while(!clazz.getSimpleName().equals("Object"))
        {
            for(Field f : clazz.getDeclaredFields())
            {
                f.setAccessible(true);
                if(f.getType().isAssignableFrom(type))
                {
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public static Method getFirstAnnotatedMethod(Class clazz, Class annotation)
    {
        while(!clazz.getSimpleName().equals("Object"))
        {
            for(Method m : clazz.getDeclaredMethods())
            {
                m.setAccessible(true);
                if(m.isAnnotationPresent(annotation))
                {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               if(!files[i].delete()) throw new RuntimeException("could not delete "+files[i].getName());
             }
          }
        }
        return( path.delete() );
      }
    
    public static boolean anyFieldHasIndexedAnnotation(Class<? extends Object> clazz)
    {
        if(Util.getFirstAnnotatedField(clazz, D2Indexed.class)!=null) return true;
        if(Util.getFirstAnnotatedMethod(clazz, D2Indexed.class)!=null) return true;
        return false;
    }
}
