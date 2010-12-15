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

import java.util.Date;

import org.d2.annotations.D2Ignore;
import org.d2.context.D2Context;


@D2Ignore
public class D2Metadata
{
    protected LoadStatus status = LoadStatus.NEW;
    protected String dataVersion = null;
    protected Date saveTimestamp = null;
    protected D2Context context = null;
    protected String loadedXml = null;
    

    public boolean isStandin()
    {
        return this.status == LoadStatus.STANDIN;
    }
    
    public boolean isNew()
    {
        return this.status == LoadStatus.NEW;
    }
    
    public LoadStatus getStatus()
    {
        return status;
    }
    public void setStatus(LoadStatus status)
    {
        this.status = status;
    }
    public String getDataVersion()
    {
        return dataVersion;
    }
    public void setDataVersion(String version)
    {
        this.dataVersion = version;
    }
    public Date getSaveTimestamp()
    {
        return saveTimestamp;
    }
    public void setSaveTimestamp(Date dttm)
    {
        this.saveTimestamp = dttm;
    }
    public D2Context getContext()
    {
        return context;
    }
    public void setContext(D2Context context)
    {
        this.context = context;
    }

    public String getLoadedXml()
    {
        return loadedXml;
    }

    public void setLoadedXml(String loadedXml)
    {
        this.loadedXml = loadedXml;
    }
}
