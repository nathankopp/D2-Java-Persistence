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
package org.d2.plugins.xstream.converters;

import java.lang.reflect.Field;
import java.util.Date;

import org.d2.D2Impl;
import org.d2.Operation;
import org.d2.annotations.D2CascadeSave;
import org.d2.annotations.D2LazyLoad;
import org.d2.context.D2Context;
import org.nkts.util.Util;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class D2EntityReferenceConverter extends D2AbstractEntityConverter
{
    public D2EntityReferenceConverter(D2Impl d2, Class<? extends Object> clazz, Converter delegateConverter, Date now,
                                      D2Context d2context, Operation operation)
    {
        super(d2, clazz, delegateConverter, now, d2context, operation);
    }

    protected void serialize(Object value, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        Field field = (Field)context.get(D2FieldFinderConverter.CURRENT_FIELD_KEY);
        if(field!=null && field.isAnnotationPresent(D2CascadeSave.class))
        {
            operation.addCascadeSave(value);
        }
    }
    
    protected Object deserialize(HierarchicalStreamReader reader, UnmarshallingContext context, String id)
    {
        String className = reader.getAttribute(D2CLASS_ATTRIB);
        Class<?> clazz = null;
        try
        {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            Util.wrap(e);
        }
        
        Object obj = d2.loadCachedOnlyById(clazz, id, d2context, true);
        
        Field field = (Field)context.get(D2FieldFinderConverter.CURRENT_FIELD_KEY);
        if(field!=null && !field.isAnnotationPresent(D2LazyLoad.class))
        {
             operation.addCascadeLoad(obj);
        }
        
        return obj;
    }
}
