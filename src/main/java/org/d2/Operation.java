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
package org.d2;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Operation
{
    private List<Object> allCascadeSaves = new ArrayList<Object>();
    private Queue<Object> cascadeSavesQueue = new ConcurrentLinkedQueue<Object>();
    
    private List<Object> allCascadeLoads = new ArrayList<Object>();
    private Queue<Object> cascadeLoadsQueue = new ConcurrentLinkedQueue<Object>();
    
    public synchronized void addCascadeSave(Object obj)
    {
        if(allCascadeSaves.contains(obj)) return;
        allCascadeSaves.add(obj);
        cascadeSavesQueue.add(obj);
    }
    
    public synchronized void addCascadeLoad(Object obj)
    {
        if(allCascadeLoads.contains(obj)) return;
        allCascadeLoads.add(obj);
        cascadeLoadsQueue.add(obj);
    }
    
    public Object pollCascadeSave()
    {
        return cascadeSavesQueue.poll();
    }
    public Object pollCascadeLoads()
    {
        return cascadeLoadsQueue.poll();
    }

    public void setCascadeSavesQueue(Queue<Object> cascadeSavesQueue)
    {
        this.cascadeSavesQueue = cascadeSavesQueue;
    }
    
}
