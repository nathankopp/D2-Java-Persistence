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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.d2.D2Impl;
import org.d2.IdFinder;
import org.d2.Operation;
import org.d2.context.D2Context;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class D2AbstractEntityConverter implements Converter
{
    protected static final String DEFAULT_VERSION = "1.0.0";
    protected static final String D2DTTM_ATTRIB = "d2dttm";
    protected static final String D2VERSION_ATTRIB = "d2version";
    protected static final String D2CLASS_ATTRIB = "d2Class";
    protected static final String D2ID_ATTRIB = "d2id";
    
    protected Class<? extends Object> convertingClass;
    protected D2Impl d2;
    protected Converter delegateConverter;
    protected Date now;
    protected D2Context d2context;
    protected Operation operation;
    
    // note SimpleDateFormat is not threadsafe - boo.
    protected DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS Z");

    protected abstract void serialize(Object value, HierarchicalStreamWriter writer, MarshallingContext context);
    protected abstract Object deserialize(HierarchicalStreamReader reader, UnmarshallingContext context, String id);

    public D2AbstractEntityConverter(D2Impl d2, Class<? extends Object> clazz, Converter delegateConverter, Date now, D2Context d2context, Operation operation)
    {
        super();
        this.d2 = d2;
        this.convertingClass = clazz;
        this.delegateConverter = delegateConverter;
        this.now = now;
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
        alwaysSaveNowIfNew(value);

        String id = IdFinder.getId(value);
        String idStr = id==null?"0":id.toString();
        
        writer.addAttribute(D2ID_ATTRIB, idStr);
        writer.addAttribute(D2CLASS_ATTRIB, value.getClass().getName());
        
        serialize(value, writer, context);
    }
    
    private void alwaysSaveNowIfNew(Object value)
    {
        // FIXME:  I don't like the id==null check to prevent infinite recursion
        if(IdFinder.getMd(value).isNew() && IdFinder.getId(value)==null)
        {
            // FIXME:  saveInternal needs to be exposed (with a different name), or this needs to know about the implementation
            ((D2Impl)d2).saveInternal(value, d2context, operation, now);
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        String idStr = reader.getAttribute(D2ID_ATTRIB);
        String id = idStr;
        
        return deserialize(reader, context, id);
    }
        
}
