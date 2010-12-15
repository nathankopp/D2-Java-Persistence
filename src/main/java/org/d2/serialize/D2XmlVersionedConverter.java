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

import org.d2.annotations.D2Versioned;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class D2XmlVersionedConverter implements Converter
{
    private Converter delegateConverter;

    public D2XmlVersionedConverter(Converter delegateConverter)
    {
        super();
        this.delegateConverter = delegateConverter;
    }

    public boolean canConvert(Class clazz)
    {
        if (clazz.isAnnotationPresent(D2Versioned.class)) return true;
        return false;
    }

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        D2Versioned a = value.getClass().getAnnotation(D2Versioned.class);
        if(a!=null) writer.addAttribute("d2version", a.version());
        delegateConverter.marshal(value, writer, context);
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
//        D2Versioned a = context.currentObject().getClass().getAnnotation(D2Versioned.class);
//        String classVersion = a.version();
        String docVersion = reader.getAttribute("d2version");
        if(docVersion==null) docVersion = "1.0.0";
        return delegateConverter.unmarshal(reader, context);
    }
}
