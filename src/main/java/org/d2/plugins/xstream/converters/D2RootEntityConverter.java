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

import java.text.ParseException;
import java.util.Date;

import org.d2.D2Impl;
import org.d2.D2Metadata;
import org.d2.IdFinder;
import org.d2.Operation;
import org.d2.annotations.D2Versioned;
import org.d2.context.D2Context;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class D2RootEntityConverter extends D2EntityReferenceConverter
{
    protected boolean isFirst = true;

    public D2RootEntityConverter(D2Impl d2, Class<? extends Object> clazz, Converter delegateConverter,
                                        Date now, D2Context d2context, Operation operation)
    {
        super(d2, clazz, delegateConverter, now, d2context, operation);
    }

    protected void serialize(Object value, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        if(isFirst)
        {
            writeMetadata(value, writer);
            isFirst = false;
            delegateConverter.marshal(value, writer, context);
        }
        else
        {
            super.serialize(value, writer, context);
        }
    }
    
    protected Object deserialize(HierarchicalStreamReader reader, UnmarshallingContext context, String id)
    {
        // if the ID is null, then we should try to actually deserialize
        // directly.  Maybe it was NOT an individual entity before
        if((isFirst) /*|| reader.hasMoreChildren()*/ || id==null)
        {
            isFirst = false;
            D2Metadata md = readMetadata(reader);
            Object obj = delegateConverter.unmarshal(reader, context);
            IdFinder.setMd(obj, md);
            return obj;
        }
        else
        {
            return super.deserialize(reader, context, id);
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
}
