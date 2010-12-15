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
package org.d2.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.nkts.util.Util;


public class FieldFilter implements Filter
{
    private String field;
    private Object value;
    
    public FieldFilter(String field, Object value)
    {
        super();
        this.field = field;
        this.value = value;
    }

    public boolean matches(Object obj)
    {
        try
        {
            if(obj==null) return false;
            
            Field f = obj.getClass().getField(field);
            f.setAccessible(true);
            Object fieldVal = f.get(obj);
            
            return Util.objEquals(value, fieldVal);
        }
        catch(NoSuchFieldException e)
        {
            try
            {
                Method[] methods = obj.getClass().getMethods();
                for(Method m : methods)
                {
                    if(m.getName().equalsIgnoreCase("get"+field) && m.getParameterTypes().length==0)
                    {
                        Object fieldVal = m.invoke(obj);
                        return Util.objEquals(value, fieldVal);
                    }
                }
            }
            catch(Throwable t)
            {
                throw Util.wrap(t);
            }
            throw new RuntimeException("No such field or method found");
        }
        catch(Throwable t)
        {
            throw Util.wrap(t);
        }
    }

}
