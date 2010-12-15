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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import org.d2.Bucket;
import org.d2.pluggable.StorageSystem;
import org.nkts.util.StringVisitor;
import org.nkts.util.Util;

import com.thoughtworks.xstream.XStream;

public class LocalFileStorage implements StorageSystem
{
    protected static FilenameFilter textFileFilter = new FilenameFilter() { public boolean accept(File dir, String name) { return name.endsWith(".txt"); } };
    
    private Bucket bucket;
    private String rootFolder;
    private String dirStr;
    private Sequences sequences;
    
    public LocalFileStorage(String rootFolder, Bucket bucket)
    {
        super();
        this.rootFolder = rootFolder;
        this.bucket = bucket;
        (new File(rootFolder)).mkdirs();
        dirStr = rootFolder+"/"+bucket.getName();
        (new File(dirStr)).mkdirs();
        
        this.sequences = loadSequences();
        
    }

    /* (non-Javadoc)
     * @see com.pitcru.persistence.d2.D2Storage#saveDocument(java.lang.Long, java.lang.String, com.pitcru.persistence.d2.D2Bucket)
     */
    public void saveDocument(String id, String str, Date now)
    {
        BufferedReader in = null;
        try
        {
            try
            {
                String numAsString = numToString(Long.valueOf(id));

                String fileName = dirStr + "/" + numAsString + ".txt";
                
                PrintStream out = new PrintStream(new FileOutputStream(fileName));
                out.println(str);
                out.close();
            }
            finally
            {
                if(in!=null) in.close();
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }        
    }
    
    public void deleteDocument(String id)
    {
        String numAsString = numToString(Long.valueOf(id));
        String fileName = dirStr + "/" + numAsString + ".txt";
        File file = new File(fileName);
        file.delete();
    }
    
    
    public String loadDocument(String id)
    {
        BufferedReader in = null;
        try
        {
            try
            {
                if(Util.isBlank(id)) throw new RuntimeException("id is blank");
                String numAsString = numToString(Long.valueOf(id));
                
                String fileName = dirStr + "/" + numAsString + ".txt";
                File file = new File(fileName);
//                System.out.println("Loading from "+bucket.getName()+": "+id);
                
                in = new BufferedReader(new FileReader(file));
                String xmlStr = readBlockAsSingleString(in);
                return xmlStr;
            }
            finally
            {
                if(in!=null) in.close();
            }
        }
        catch(FileNotFoundException e)
        {
            return null;
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    
    @Override
    public void eachId(StringVisitor visitor)
    {
        try
        {
            File dir = new File(dirStr);
            File[] list = dir.listFiles(textFileFilter);
            if(list!=null)
            {
                for(File file : list)
                {
                    String idStr = file.getName().substring(0,file.getName().lastIndexOf('.'));
                    if(!idStr.equals("info"))
                    {
                        String id  = Long.toString(Long.valueOf(idStr));
                        visitor.visit(id);
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw Util.wrap(e);
        }
    }
    
    public void eachDocument(final StringVisitor docVisitor)
    {
        eachId(new StringVisitor(){
            @Override
            public void visit(String id)
            {
                String doc = loadDocument(id);
                docVisitor.visit(doc);
            }
        });
    }
    
    public void saveSequences(Sequences sequences)
    {
        try
        {
            XStream xs = new XStream();
            String xml = xs.toXML(sequences);
            
            String fileName = dirStr+"/sequences.txt";
            PrintStream out = new PrintStream(new FileOutputStream(fileName));
            out.println(xml);
            out.println();
            out.close();
        }
        catch (FileNotFoundException e)
        {
            throw Util.wrap(e);
        }
    }
    
    /* (non-Javadoc)
     * @see com.pitcru.persistence.document.DocDaoManager#loadSequences()
     */
    public Sequences loadSequences()
    {
        Sequences sequences = null;
        BufferedReader in = null;
        try
        {
            String fileName = dirStr+"/sequences.txt";

            File file = new File(fileName);
            in = new BufferedReader(new FileReader(file));
            String xmlStr = readBlockAsSingleString(in);

            XStream xs = new XStream();
            sequences = (Sequences)xs.fromXML(xmlStr);
        }
        catch(FileNotFoundException e)
        {
            sequences = new Sequences();
        }
        catch (Exception e)
        {
            throw Util.wrap(e);
        }
        finally
        {
            if(in!=null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    throw Util.wrap(e);
                }
            }
        }
        return sequences;
    }
    
    protected String readBlockAsSingleString(BufferedReader in) throws IOException
    {
        String s = null;
        while(Util.isBlank(s)) s = in.readLine();
        String xmlStr = "";
        while(!Util.isBlank(s))
        {
            xmlStr += s + "\n";
            s = in.readLine();
        }
        return xmlStr;
    }
    
    private String numToString(Long id)
    {
        String numAsString = ""+id;
        if(id<10) numAsString = "0"+numAsString;
        if(id<100) numAsString = "0"+numAsString;
        if(id<1000) numAsString = "0"+numAsString;
        if(id<10000) numAsString = "0"+numAsString;
        return numAsString;
    }

    public String makeReal(String id)
    {
        if(!id.startsWith("-")) throw new RuntimeException("id already is real");
        return id.substring(1);
    }

    public String makeStandin(String id)
    {
        if(id.startsWith("-")) throw new RuntimeException();
        return "-"+id;
    }

    public String getSeqNextVal(String entityName)
    {
        String seq = sequences.getNext(entityName);
        saveSequences(sequences);
        return seq;
    }
    
    public boolean isIdStandin(String id)
    {
        return sequences.isStandin(id);
    }
    
    public void setSequenceIfMore(String name, String value)
    {
        sequences.setIfMore(name, value);
        saveSequences(sequences);
    }

    @Override
    public boolean isIdValid(String id)
    {
        if (Util.isBlank(id) || id.equals("0") || id.equals("-0")) return false;
        return true;
    }

    public Bucket getBucket()
    {
        return bucket;
    }

    public String getRootFolder()
    {
        return rootFolder;
    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void acquireReadLock(String id)
    {
        FileReadWriteLock.find(dirStr+"/"+id+".lock").acquireReadLock();
    }

    @Override
    public void acquireWriteLock(String id)
    {
        FileReadWriteLock.find(dirStr+"/"+id+".lock").acquireWriteLock();
    }

    @Override
    public void releaseReadLock(String id)
    {
        FileReadWriteLock.find(dirStr+"/"+id+".lock").releaseReadLock();
    }

    @Override
    public void releaseWriteLock(String id)
    {
        FileReadWriteLock.find(dirStr+"/"+id+".lock").releaseWriteLock();
    }
}
