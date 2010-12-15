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
package org.d2.serialize;

import java.lang.reflect.Field;

import org.d2.annotations.D2Aware;
import org.d2.annotations.D2Entity;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

public class D2AwareReflectionConverter extends AbstractReflectionConverter
{
    public static final String CURRENT_FIELD_KEY = "currentField";

    public D2AwareReflectionConverter(Mapper mapper, ReflectionProvider reflectionProvider)
    {
        super(mapper, reflectionProvider);
    }
    
    @Override
    public boolean canConvert(Class type)
    {
        if(type.isAnnotationPresent(D2Aware.class) || type.isAnnotationPresent(D2Entity.class)) return true;
        return false;
    }
    
    @Override
    protected void marshallField(MarshallingContext context, Object newObj, Field field)
    {
        context.put(CURRENT_FIELD_KEY, field);
        super.marshallField(context, newObj, field);
    }
    
    @Override
    protected Object unmarshallField(UnmarshallingContext context, Object result, Class type, Field field)
    {
        context.put(CURRENT_FIELD_KEY, field);
        return super.unmarshallField(context, result, type, field);
    }

}
