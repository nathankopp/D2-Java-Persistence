/*******************************************************************************
 * Copyright Nathan Kopp
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


public class D2NumericRangeTerm extends D2QueryNode
{
    private String field;
    private Number value1;
    private Number value2;
    private boolean inclusive;
    
    public D2NumericRangeTerm(String field, Number value1, Number value2, boolean inclusive, Occurs occurs)
    {
        super();
        this.field = field;
        this.value1 = value1;
        this.value2 = value2;
        this.inclusive = inclusive;
        this.setOccurs(occurs);
    }
    
    public Class<?> getType()
    {
        if(value1!=null) return value1.getClass();
        if(value2!=null) return value2.getClass();
        throw new RuntimeException("one of the values must be non-null");
    }
    
    public String getField()
    {
        return field;
    }
    public void setField(String field)
    {
        this.field = field;
    }
    public Number getValue1()
    {
        return value1;
    }
    public void setValue1(Number value1)
    {
        this.value1 = value1;
    }
    public Number getValue2()
    {
        return value2;
    }
    public void setValue2(Number value2)
    {
        this.value2 = value2;
    }

    public boolean isInclusive()
    {
        return inclusive;
    }

    public void setInclusive(boolean inclusive)
    {
        this.inclusive = inclusive;
    }

}
