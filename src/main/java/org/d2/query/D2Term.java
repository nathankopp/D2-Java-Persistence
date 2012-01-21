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
package org.d2.query;


public class D2Term extends D2QueryNode
{
    private String field;
    private String value;
    private TermType type;
    
    
    public D2Term(String field, String value, TermType type, Occurs occurs)
    {
        super();
        this.field = field;
        this.value = value;
        this.type = type;
        this.setOccurs(occurs);
    }
    
    public String getField()
    {
        return field;
    }
    public void setField(String field)
    {
        this.field = field;
    }
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }
    public TermType getType()
    {
        return type;
    }
    public void setType(TermType type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "D2Term [field=" + field + ", type=" + type + ", value=" + value + "]";
    }
    
    
}
