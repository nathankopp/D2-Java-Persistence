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

import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProviderWrapper;

public class VersionedReflectionProvider extends ReflectionProviderWrapper
{

    public VersionedReflectionProvider(ReflectionProvider wrapper)
    {
        super(wrapper);
    }
    
    @Override
    public boolean fieldDefinedInClass(String fieldName, Class type)
    {
        // TODO Auto-generated method stub
        return super.fieldDefinedInClass(fieldName, type);
    }
    
    @Override
    public Field getField(Class definedIn, String fieldName)
    {
        // TODO Auto-generated method stub
        return super.getField(definedIn, fieldName);
    }
    
    @Override
    public Class getFieldType(Object object, String fieldName, Class definedIn)
    {
        // TODO Auto-generated method stub
        return super.getFieldType(object, fieldName, definedIn);
    }
    
    @Override
    public void writeField(Object object, String fieldName, Object value, Class definedIn)
    {
        // TODO Auto-generated method stub
        super.writeField(object, fieldName, value, definedIn);
    }

}
