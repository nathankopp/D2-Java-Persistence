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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.d2.D2Impl;
import org.d2.D2Metadata;
import org.d2.IdFinder;
import org.d2.Operation;
import org.d2.annotations.D2CascadeSave;
import org.d2.annotations.D2LazyLoad;
import org.d2.annotations.D2Versioned;
import org.d2.context.D2Context;
import org.nkts.util.Util;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class D2XmlEntityConverter implements Converter
{
    private static final String DEFAULT_VERSION = "1.0.0";
    private static final String D2DTTM_ATTRIB = "d2dttm";
    private static final String D2VERSION_ATTRIB = "d2version";
    private static final String D2CLASS_ATTRIB = "d2Class";
    private static final String D2ID_ATTRIB = "d2id";
    
    
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

    @SuppressWarnings({ "rawtypes" })
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
        
        writer.addAttribute(D2ID_ATTRIB, idStr);
        writer.addAttribute(D2CLASS_ATTRIB, value.getClass().getName());
        
        if(serializeFirst && isFirst)
        {
            writeMetadata(value, writer);
            
            isFirst = false;
            delegateConverter.marshal(value, writer, context);
        }
        else
        {
            Field field = (Field)context.get(D2EntityReflectionConverter.CURRENT_FIELD_KEY);
            if(field.isAnnotationPresent(D2CascadeSave.class))
            {
                operation.addCascadeSave(value);
            }
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        String idStr = reader.getAttribute(D2ID_ATTRIB);
        String id = idStr;
        
        // if the ID is null, then we should try to actually deserialize
        // directly.  Maybe it was NOT an individual entity before
        if((serializeFirst && isFirst) /*|| reader.hasMoreChildren()*/ || id==null)
        {
            isFirst = false;
            D2Metadata md = readMetadata(reader);
            Object obj = delegateConverter.unmarshal(reader, context);
            IdFinder.setMd(obj, md);
            return obj;
        }
        else
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
            
//            if(IdFinder.getMd(obj).isStandin())
//            {
                Field field = (Field)context.get(D2EntityReflectionConverter.CURRENT_FIELD_KEY);
                if(!field.isAnnotationPresent(D2LazyLoad.class))
                {
                     operation.addCascadeLoad(obj);
                }
//            }
            
            return obj;
        }
    }

    private void writeMetadata(Object value, HierarchicalStreamWriter writer)
    {
        writer.addAttribute(D2DTTM_ATTRIB, df.format(now));
        
        D2Versioned a = value.getClass().getAnnotation(D2Versioned.class);
        if(a!=null) writer.addAttribute(D2VERSION_ATTRIB, a.version());
    }

    private D2Metadata readMetadata(HierarchicalStreamReader reader)
    {
        D2Metadata md = new D2Metadata();

        String docVersion = reader.getAttribute(D2VERSION_ATTRIB);
        if(docVersion==null) docVersion = DEFAULT_VERSION;
        md.setDataVersion(docVersion);

        String dttmStr = reader.getAttribute(D2DTTM_ATTRIB);
        if(dttmStr!=null)
        {
            try
            {
                md.setSaveTimestamp(df.parse(dttmStr));
            }
            catch (ParseException e)
            {
                md.setSaveTimestamp(now);
            }
        }
        
        return md;
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
