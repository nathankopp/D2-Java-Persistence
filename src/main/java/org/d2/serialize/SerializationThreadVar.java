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

import com.thoughtworks.xstream.converters.reflection.ReflectionProviderWrapper;

public class SerializationThreadVar
{
    static private ThreadLocal<ReflectionProviderWrapper> instance = new ThreadLocal<ReflectionProviderWrapper>();
    
    public static ReflectionProviderWrapper getInstance()
    {
        return instance.get();
    }
    
    private Long rootId;
    private boolean first;
    private String docVersion;
    private String classVersion;

    public Long getRootId()
    {
        return rootId;
    }
    public void setRootId(Long rootId)
    {
        this.rootId = rootId;
    }
    public boolean isFirst()
    {
        return first;
    }
    public void setFirst(boolean first)
    {
        this.first = first;
    }
    public String getDocVersion()
    {
        return docVersion;
    }
    public void setDocVersion(String docVersion)
    {
        this.docVersion = docVersion;
    }
    public String getClassVersion()
    {
        return classVersion;
    }
    public void setClassVersion(String classVersion)
    {
        this.classVersion = classVersion;
    }
}
