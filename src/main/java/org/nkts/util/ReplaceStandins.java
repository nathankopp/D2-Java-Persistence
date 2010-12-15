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

import org.d2.D2;
import org.d2.IdFinder;
import org.d2.context.D2Context;


public class ReplaceStandins implements ReplacingVisitor
{
    private D2 d2;
    private D2Context context;

    public ReplaceStandins(D2 d2, D2Context context)
    {
        super();
        this.d2 = d2;
        this.context = context;
    }

    public Object visit(Object o)
    {
        if(Util.isBlank(IdFinder.getId(o)) || !IdFinder.getId(o).startsWith("-")) return o;
        
        if(d2!=null)
        {
            return d2.loadById(o.getClass(), IdFinder.getId(o).substring(1), context);
        }
        
        return o;
    }

}
