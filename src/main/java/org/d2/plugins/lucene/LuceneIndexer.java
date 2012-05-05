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
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.d2.Bucket;
import org.d2.IdFinder;
import org.d2.index.DocBuilderAbstract;
import org.d2.pluggable.Indexer;
import org.d2.query.D2NumericRangeTerm;
import org.d2.query.D2Query;
import org.d2.query.D2QueryNode;
import org.d2.query.D2RangeTerm;
import org.d2.query.D2Term;
import org.nkts.util.Util;


public class LuceneIndexer implements Indexer
{
    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
    private File indexDir;
    private Directory index = null;
    
    private IndexWriter writer;
    private Object writerLock = new Object();
    
    private IndexReader reader;
    private IndexSearcher searcher;
    private DocBuilderAbstract docBuilder;
    
    private Thread writerCommitThread;
    private Boolean stopThread = false;
    private long commitDelay = 5000;
    
    private boolean dirty = false;
    private Object searcherLock = new Object();
    
    public LuceneIndexer(String rootFolder, Bucket bucket) throws CorruptIndexException, LockObtainFailedException, IOException
    {
        indexDir = new File(rootFolder+"/"+bucket.getName()+"/index");
        indexDir.mkdirs();
        index = FSDirectory.open(indexDir);
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
            //writer.commit();
            closeWriter();
            closeReaderAndSearcher();
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
            
            //writer.commit();  // NOTE: may cause performance issues
            closeWriter();
            
//            // need to re-open the reader and searcher
            
            dirty = true;
            
            //closeReaderAndSearcher();
            //openReaderAndSearcher();
            
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
            if(dirty)
            {
                synchronized(searcherLock)
                {
                    reopenReaderAndSearcher();
                    dirty=false;
                }
            }
            
            openReaderAndSearcher();
            List<String> pList = new ArrayList<String>();
            
            TopScoreDocCollector  collector = TopScoreDocCollector.create(20, true);
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
            
            
            //writer = new IndexWriter(directory, analyzer, true, IndexWriter.MaxFieldLength.LIMITED);
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
            IndexWriter writer = new IndexWriter(index, config);
            for(Object obj : objList)
            {
                Document d = (Document)docBuilder.toDocument(obj);
                writer.addDocument(d);
            }
    
            //writer.optimize();
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
                        case WILDCARD:
                            lquery.add(new WildcardQuery(new Term(term.getField(), term.getValue())),  getLuceneOccur(node));
                            break;
                        case PARSED:
                            QueryParser parser = new QueryParser(Version.LUCENE_36, term.getField(), analyzer);
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
                    lquery.add(new TermRangeQuery(term.getField(), term.getValue1(), term.getValue2(), term.isInclusive(), term.isInclusive()),  getLuceneOccur(node));
                }
                else if (node instanceof D2NumericRangeTerm)
                {
                    D2NumericRangeTerm term = (D2NumericRangeTerm)node;
                    if(Double.class.isAssignableFrom(term.getType()))       lquery.add(NumericRangeQuery.newDoubleRange(term.getField(), (Double)term.getValue1(), (Double)term.getValue2(), term.isInclusive(), term.isInclusive()),  getLuceneOccur(node));
                    else if(Long.class.isAssignableFrom(term.getType()))    lquery.add(NumericRangeQuery.newLongRange(term.getField(), (Long)term.getValue1(), (Long)term.getValue2(), term.isInclusive(), term.isInclusive()),  getLuceneOccur(node));
                    else if(Float.class.isAssignableFrom(term.getType()))   lquery.add(NumericRangeQuery.newFloatRange(term.getField(), (Float)term.getValue1(), (Float)term.getValue2(), term.isInclusive(), term.isInclusive()),  getLuceneOccur(node));
                    else if(Integer.class.isAssignableFrom(term.getType())) lquery.add(NumericRangeQuery.newIntRange(term.getField(), (Integer)term.getValue1(), (Integer)term.getValue2(), term.isInclusive(), term.isInclusive()),  getLuceneOccur(node));
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
    
    public void closeReaderAndSearcher() throws IOException
    {
        synchronized(searcherLock)
        {
            if(searcher!=null) searcher.close();
            if(reader!=null) reader.close();
            searcher = null;
            reader = null;
        }
    }
    
    private void reopenReaderAndSearcher() throws IOException, CorruptIndexException
    {
        synchronized(searcherLock)
        {
            closeReaderAndSearcher();
            openReaderAndSearcher();
        }
    }

    private void reopenJustSearcher() throws IOException, CorruptIndexException
    {
        synchronized(searcherLock)
        {
            synchronized(searcherLock)
            {
                if(searcher!=null) searcher.close();
                searcher = new IndexSearcher(reader);
            }
        }
    }

    public void closeWriter() throws CorruptIndexException, IOException
    {
        synchronized(writerLock)
        {
            //if(writer!=null) writer.commit();
            if(writer!=null) writer.close();
            writer = null;
        }
    }
    
    private void openReaderAndSearcher() throws CorruptIndexException, IOException
    {
        synchronized(searcherLock)
        {
            if(searcher!=null) return;
            if(reader!=null) closeReaderAndSearcher();
            try
            {
                reader = IndexReader.open(index);
                searcher = new IndexSearcher(reader);
            }
            catch(FileNotFoundException e)
            {
                rebuildIndex(new ArrayList<Object>());
                reader = IndexReader.open(index);
                searcher = new IndexSearcher(reader);
            }
        }
    }

    private void openWriter() throws CorruptIndexException, LockObtainFailedException, IOException
    {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        try
        {
            writer = new IndexWriter(index, config);

        }
        catch(FileNotFoundException e)
        {
            writer = new IndexWriter(index, config);
        }
    }
    
    
    public IndexSearcher getSearcher()
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

    @Override
    public void resetLocks()
    {
        
        try
        {
            if ((new File(indexDir+"/write.lock").delete()))
            {
                System.out.println(indexDir+"/write.lock deleted");
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }

    
    

    
}
