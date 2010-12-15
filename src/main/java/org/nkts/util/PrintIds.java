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
package org.nkts.util;

import org.d2.IdFinder;


public class PrintIds implements ReplacingVisitor
{
    public int depth;
    
    public PrintIds()
    {
        super();
    }

    public Object visit(Object o)
    {
        String id  = IdFinder.getId(o);
        if(id==null)
        {
            if(o.getClass().getName().startsWith("java.")) return o;
            for(int i=0; i<depth; i++) System.out.print(" ");
            System.out.println(o.getClass().getSimpleName());
            return o;
        }
        
        for(int i=0; i<depth; i++) System.out.print(" ");
        
        System.out.print(o.getClass().getSimpleName());
        System.out.println(": "+id);
        
        return o;
    }

}
