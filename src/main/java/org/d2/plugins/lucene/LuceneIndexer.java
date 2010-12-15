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
package org.d2.plugins.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.LockObtainFailedException;
import org.d2.Bucket;
import org.d2.IdFinder;
import org.d2.index.DocBuilderAbstract;
import org.d2.pluggable.Indexer;
import org.d2.query.D2Query;
import org.d2.query.D2QueryNode;
import org.d2.query.D2RangeTerm;
import org.d2.query.D2Term;
import org.nkts.util.Util;


public class LuceneIndexer implements Indexer
{
    private Analyzer analyzer = new StandardAnalyzer();
    private File indexDir;
    
    private IndexWriter writer;
    private Object writerLock = new Object();
    
    private IndexReader reader;
    private Searcher searcher;
    private DocBuilderAbstract docBuilder;
    
    private Thread writerCommitThread;
    private Boolean stopThread = false;
    private long commitDelay = 5000;
    
    public LuceneIndexer(String rootFolder, Bucket bucket) throws CorruptIndexException, LockObtainFailedException, IOException
    {
        indexDir = new File(rootFolder+"/"+bucket.getName()+"/index");
        indexDir.mkdirs();
        openReaderAndSearcher();
        this.docBuilder = createDocBuilder();
        
//        writerCommitThread = createWriterCommitThread();
//        writerCommitThread.start();
    }

    private DocBuilderAbstract createDocBuilder()
    {
        return new LuceneDocBuilder();
    }

    public void flushAndReopenWriter()
    {
        try
        {
            writer.commit();
            closeWriter();
            if(searcher!=null) searcher.close();
            if(reader!=null) reader.close();
            openReaderAndSearcher();
        }
        catch(Throwable t)
        {
            throw Util.wrap(t);
        }
        
    }


    public synchronized void indexObject(Object obj)
    {
        try
        {
            if(writer==null) openWriter();
            Document d = (Document)docBuilder.toDocument(obj);
            writer.updateDocument(new Term("id",IdFinder.getId(obj).toString()), d);
            
            writer.commit();  // NOTE: may cause performance issues
            
//            // need to re-open the reader and searcher
            if(true)
            {
                if(searcher!=null) searcher.close();
                if(reader!=null) reader.close();
                openReaderAndSearcher();
            }
            else
            {
                searcher.close();
                IndexReader reader2 = reader.reopen();
                if(reader2!=reader)
                {
                    reader.close();
                    reader = reader2;
                }
                searcher = new IndexSearcher(reader);
            }
            
        }
        catch (CorruptIndexException e)
        {
            throw Util.wrap(e);
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
        finally
        {
            try
            {
//                closeWriter();
            }
            catch(Exception e)
            {
                throw Util.wrap(e);
            }
        }
    }
    
    public List<String> findIdByQuery(D2Query query)
    {
        try
        {
            if(searcher==null) openReaderAndSearcher();
            List<String> pList = new ArrayList<String>();
            
            TopDocCollector collector = new TopDocCollector(20);
            searcher.search(makeLuceneQuery(query), collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
            for (int i = 0; i < hits.length; i++)
            {
                Document doc = searcher.doc(hits[i].doc);
                pList.add(doc.get("id"));
            }
            
            return pList;
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
    }
    
    
    
    // ======================================================================
    // ======================================================================
    
    /**
     * TODO stop the background thread and lock everything (including all reads)
     */
    public void rebuildIndex(Collection<Object> objList)
    {
        try
        {
            if(writer!=null) writer.close();
            if(searcher!=null) searcher.close();
            if(reader!=null) reader.close();
            
            writer = new IndexWriter(indexDir, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
            for(Object obj : objList)
            {
                Document d = (Document)docBuilder.toDocument(obj);
                writer.addDocument(d);
            }
    
            writer.optimize();
            writer.commit();
            
            writer.close();
            writer = null;
            
            openReaderAndSearcher();
        }
        catch (CorruptIndexException e)
        {
            throw Util.wrap(e);
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
    }
    
    // ======================================================================
    // ======================================================================
    
    protected Query makeLuceneQuery(D2Query query)
    {
        try
        {
            BooleanQuery lquery = new BooleanQuery();
            for(D2QueryNode node : query.getNodes())
            {
                if(node instanceof D2Term)
                {
                    D2Term term = (D2Term)node;
                    switch(term.getType())
                    {
                        case EXACT:
                            lquery.add(new TermQuery(new Term(term.getField(), handleBlanks(term.getValue()))),  getLuceneOccur(node));
                            break;
                        case FUZZY:
                            lquery.add(new FuzzyQuery(new Term(term.getField(), term.getValue())),  getLuceneOccur(node));
                            break;
                        case PREFIX:
                            lquery.add(new PrefixQuery(new Term(term.getField(), term.getValue())),  getLuceneOccur(node));
                            break;
                        case REGEX:
                            throw new RuntimeException("regex queries require upgrade to lucene 2.9.0");
                            // requires lucene 2.9.0
                            //lquery.add(new RegexQuery(new Term(term.getField(), term.getValue())),  BooleanClause.Occur.MUST);
                            //break;
                        case WILDCARD:
                            lquery.add(new WildcardQuery(new Term(term.getField(), term.getValue())),  getLuceneOccur(node));
                            break;
                        case PARSED:
                            QueryParser parser = new QueryParser(term.getField(), analyzer);
                            Query luceneChild = parser.parse(term.getValue());
                            lquery.add(luceneChild,  getLuceneOccur(node));
                            break;
                        default:
                            throw new RuntimeException("query type not found: "+term.getType());
                    }
                }
                else if (node instanceof D2RangeTerm)
                {
                    D2RangeTerm term = (D2RangeTerm)node;
                    lquery.add(new RangeQuery(new Term(term.getField(), term.getValue1()), new Term(term.getField(), term.getValue2()), true),  getLuceneOccur(node));
                }
                else if (node instanceof D2Query)
                {
                    D2Query child = (D2Query)node;
                    Query luceneChild = makeLuceneQuery(child);
                    lquery.add(luceneChild, getLuceneOccur(child));
                }
            }
            
//            System.out.println("LUCENE QUERY: "+lquery.toString());
            
            return lquery;
        }
        catch (ParseException e)
        {
            throw Util.wrap(e);
        }
    }
    
    private static String handleBlanks(String input)
    {
        if(Util.isBlank(input)) return LuceneDocBuilder.MAGIC_BLANK_VALUE;
        else return input;
    }

    private static Occur getLuceneOccur(D2QueryNode node)
    {
        switch(node.getOccurs())
        {
            case MUST: return BooleanClause.Occur.MUST;
            case SHOULD: return BooleanClause.Occur.SHOULD;
            case MUST_NOT: return BooleanClause.Occur.MUST_NOT;
        }
        throw new RuntimeException("bad occurs: "+node.getOccurs());
    }

    // ======================================================================
    // ======================================================================
    
    
    
    public void close()
    {
        try
        {
            stopThread = true;
            closeReaderAndSearcher();
            closeWriter();
            synchronized(stopThread) { stopThread.notifyAll(); }
        }
        catch(Exception e)
        {
            Util.wrap(e);
        }
    }
    
    public void closeReaderAndSearcher() throws Exception
    {
        if(searcher!=null) searcher.close();
        if(reader!=null) reader.close();
        searcher = null;
        reader = null;
    }
    
    public void closeWriter() throws CorruptIndexException, IOException
    {
        synchronized(writerLock)
        {
            if(writer!=null) writer.commit();
            if(writer!=null) writer.close();
            writer = null;
        }
    }
    
    private void openReaderAndSearcher() throws CorruptIndexException, IOException
    {
        try
        {
            reader = IndexReader.open(indexDir);
            searcher = new IndexSearcher(reader);
        }
        catch(FileNotFoundException e)
        {
            rebuildIndex(new ArrayList<Object>());
            reader = IndexReader.open(indexDir);
            searcher = new IndexSearcher(reader);
        }
    }

    private void openWriter() throws CorruptIndexException, LockObtainFailedException, IOException
    {
        try
        {
            writer = new IndexWriter(indexDir, analyzer, false, IndexWriter.MaxFieldLength.LIMITED);
        }
        catch(FileNotFoundException e)
        {
            writer = new IndexWriter(indexDir, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
        }
    }
    
    
    public Searcher getSearcher()
    {
        try
        {
            if(searcher==null) openReaderAndSearcher();
            return searcher;
        }
        catch(Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    private Thread createWriterCommitThread()
    {
        return new Thread(new Runnable(){
            @Override
            public void run()
            {
                while(!stopThread)
                {
                    synchronized(stopThread)
                    {
                        synchronized(writerLock)
                        {
                            try
                            {
                                if(writer!=null) writer.commit();
                            }
                            catch(Exception e)
                            {
                                throw Util.wrap(e);
                            }
                        }
                        try
                        {
                            stopThread.wait(commitDelay);
                        }
                        catch (InterruptedException e)
                        {
                            // do nothing... this is OK
                        }
                    }
                }
            }
        });
    }


    public DocBuilderAbstract getDocBuilder()
    {
        return docBuilder;
    }

    @Override
    public void deleteDocument(String id)
    {
        try
        {
            writer.deleteDocuments(new Term("id", id));
            writer.commit();
        }
        catch(Throwable t)
        {
            throw Util.wrap(t);
        }

    }

    
    

    
}
