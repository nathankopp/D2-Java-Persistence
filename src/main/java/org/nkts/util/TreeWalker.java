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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class TreeWalker
{
    public static Object walk(ReplacingVisitor job, Object o)
    {
        Collection<Object> alreadyCovered = new HashSet<Object>();
        return walk(job, o, alreadyCovered);
    }
    
    public static Object walk(ReplacingVisitor job, Object o, Collection<Object> alreadyCovered)
    {
        if(job instanceof PrintIds) ((PrintIds)job).depth++;
        
        try
        {
            Object retVal = o;
            
            retVal = job.visit(o);
            o = retVal;

            if(alreadyCovered.contains(o)) return retVal;
            alreadyCovered.add(o);

            
            Class c = o.getClass();
            
            while(!c.isAssignableFrom(Object.class) && !c.getName().startsWith("java."))
            {
//                System.out.println("Examining "+c.getName());
                for(Field f : c.getDeclaredFields())
                {
                    if(Modifier.isFinal(f.getModifiers())) continue;
                    if(Modifier.isStatic(f.getModifiers())) continue;
                    
//                    System.out.println("Field "+f.getName());
                    f.setAccessible(true);
                    Object val = f.get(o);
                    if(val!=null)
                    {
//                        System.out.println("Field not null "+f.getName());
                        Class classOfVal = val.getClass();

                        if(List.class.isAssignableFrom(classOfVal))
                        {
                            if(job instanceof PrintIds) ((PrintIds)job).depth++;
                            if(job instanceof PrintIds) System.out.println(f.getName());
                            
                            List list = (List)val;
                            for(int i=0; i<list.size(); i++)
                            {
                                Object o2 = list.get(i);
                                Object newVal = walk(job, o2, alreadyCovered);
                                list.set(i, newVal);
                            }
                            if(job instanceof PrintIds) ((PrintIds)job).depth--;
                        }
                        else if(Iterable.class.isAssignableFrom(classOfVal))
                        {
                            for(Object o2 : (Iterable)val)
                            {
                                Object newVal = walk(job, o2, alreadyCovered);
                                if(newVal!=o2)
                                {
                                    throw new RuntimeException("unable to replace object with its new value in collection of type "+classOfVal.getName());
                                }
                            }
                        }
                        else if(!f.getType().getName().startsWith("java."))
                        {
                            Object newVal = walk(job, val, alreadyCovered);
                            f.set(o, newVal);
                        }
                    }
                }
                c = c.getSuperclass();
            }
            
            return retVal;
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
        finally
        {
            if(job instanceof PrintIds) ((PrintIds)job).depth--;
        }
    }

}
