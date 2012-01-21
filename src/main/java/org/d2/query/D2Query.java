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

import java.util.ArrayList;
import java.util.List;

public class D2Query extends D2QueryNode
{
    private List<D2QueryNode> nodes = new ArrayList<D2QueryNode>();
    private List<Filter> filters = new ArrayList<Filter>();
    
    public void add(D2QueryNode node)
    {
        nodes.add(node);
    }
    
    public void addFilter(Filter filter)
    {
        filters.add(filter);
    }
    
    public List<D2QueryNode> getNodes()
    {
        return nodes;
    }

    public List<Filter> getFilters()
    {
        return filters;
    }
    
    @Override
    public String toString()
    {
        String s = "";
        for(D2QueryNode node : nodes)
        {
            s += getOccurs()+":"+node.toString()+" ";
        }
        return s.trim();
    }

}
