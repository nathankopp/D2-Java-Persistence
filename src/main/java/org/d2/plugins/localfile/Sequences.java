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
package org.d2.plugins.localfile;

import java.util.HashMap;
import java.util.Map;

public class Sequences
{
    Map<String, String> seq = new HashMap<String, String>();

    public synchronized String getNext(String name)
    {
        String value = seq.get(name);
        if(value==null) value = "0";
        value = Long.toString(Long.parseLong(value)+1);
        seq.put(name, value);
        return value;
    }
    
    public synchronized void setIfMore(String name, String value)
    {
        String value2 = seq.get(name);
        if(value2==null || Long.parseLong(value2)<Long.parseLong(value))
            set(name, value);
    }
    
    public synchronized void set(String name, String value)
    {
        seq.put(name, value);
    }
    
    public synchronized void clear()
    {
        seq.clear();
    }

    public boolean isStandin(String id)
    {
        if(id==null) return true;
        if(id.startsWith("-") || id.equals("0")) return true;
        return false;
    }
}
