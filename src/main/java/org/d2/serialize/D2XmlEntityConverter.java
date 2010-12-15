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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.d2.D2Impl;
import org.d2.IdFinder;
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

public class D2XmlEntityConverter implements Converter
{
    private Class<? extends Object> convertingClass;
    private D2Impl d2;
    private boolean isFirst = true;
    private Converter delegateConverter;
    private Date now;
    private boolean serializeFirst;
    private D2Context d2context;
    private Operation operation;
    
    // note SimpleDateFormat is not threadsafe - boo.
    private DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS Z");

    public D2XmlEntityConverter(D2Impl d2, Class<? extends Object> clazz, Converter delegateConverter, D2Context d2context, Operation operation)
    {
        super();
        this.d2 = d2;
        this.convertingClass = clazz;
        this.delegateConverter = delegateConverter;
        serializeFirst = false;
        this.d2context = d2context;
        this.operation = operation;
    }

    public D2XmlEntityConverter(D2Impl d2, Class<? extends Object> clazz, Converter delegateConverter, Date now, D2Context d2context, Operation operation)
    {
        super();
        this.d2 = d2;
        this.convertingClass = clazz;
        this.delegateConverter = delegateConverter;
        this.now = now;
        serializeFirst = true;
        this.d2context = d2context;
        this.operation = operation;
    }

    public boolean canConvert(Class clazz)
    {
        if (this.convertingClass.isAssignableFrom(clazz)) return true;
        return false;
    }
    
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        // fixme... I don't like the id==null check to prevent infinite recursion
        if(IdFinder.getMd(value).isNew() && IdFinder.getId(value)==null)
        {
            // immediate cascade save!
            d2.save(value, d2context);
        }

        String id = IdFinder.getId(value);
        String idStr = id==null?"0":id.toString();
        
        writer.addAttribute("d2id", idStr);
        writer.addAttribute("d2Class", value.getClass().getName());
        
        if(serializeFirst && isFirst)
        {
            writer.addAttribute("d2dttm", df.format(now));
            
            isFirst = false;
            delegateConverter.marshal(value, writer, context);
        }
        else
        {
            Field field = (Field)context.get(D2AwareReflectionConverter.CURRENT_FIELD_KEY);
            if(field.isAnnotationPresent(D2CascadeSave.class))
            {
                operation.addCascadeSave(value);
            }
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        String idStr = reader.getAttribute("d2id");
        String id = idStr;
        
        // if the ID is null, then we should try to actually deserialize
        // directly.  Maybe it was NOT an individual entity before
        if((serializeFirst && isFirst) /*|| reader.hasMoreChildren()*/ || id==null)
        {
            String dttmStr = reader.getAttribute("d2dttm");
            
            isFirst = false;
            return delegateConverter.unmarshal(reader, context);
        }
        else
        {
            String className = reader.getAttribute("d2Class");
            Class clazz = null;
            try
            {
                clazz = Class.forName(className);
            }
            catch (ClassNotFoundException e)
            {
                Util.wrap(e);
            }
            
            Object obj = d2.loadCachedOnlyById(clazz, id, d2context, true);
            
//            if(IdFinder.getMd(obj).isStandin())
//            {
                Field field = (Field)context.get(D2AwareReflectionConverter.CURRENT_FIELD_KEY);
                if(!field.isAnnotationPresent(D2LazyLoad.class))
                {
                     operation.addCascadeLoad(obj);
                }
//            }
            
            return obj;
        }
    }

    private void assertNodeName(String actual, String expected)
    {
        if(!actual.equals(expected)) throw new RuntimeException("expected "+expected+" but found "+actual);
    }

    protected Object createStandin()
    {
        try
        {
            return convertingClass.newInstance();
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    public String makeReal(String id)
    {
        if(!id.startsWith("-")) throw new RuntimeException("id already is real: "+id);
        return id.substring(1);
    }

    public String makeStandin(String id)
    {
        if(id.startsWith("-")) throw new RuntimeException("id is already a standin: "+id);
        return "-"+id;
    }
}
