package org.d2.plugins.lucene;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Version;
import org.d2.IdFinder;
import org.nkts.util.Util;

public class LuceneManager
{
    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
    private Directory index;
    private IndexWriter writer;
    private List<LuceneReaderAndSearcher> searchers = new ArrayList<LuceneReaderAndSearcher>();
    private LuceneReaderAndSearcher lastReader;
    
    public LuceneManager(Directory index) throws CorruptIndexException, LockObtainFailedException, IOException
    {
        this.index = index;
        openWriter();
        lastReader = new LuceneReaderAndSearcher(IndexReader.open(writer,true));
        searchers.add(lastReader);
    }


    public void updateDocument(Object obj, Document d) throws CorruptIndexException, IOException
    {
        getWriter().updateDocument(new Term("id",IdFinder.getId(obj).toString()), d);
    }

    public LuceneReaderAndSearcher getSearcher() throws IOException
    {
        clearAllDirtyUnused();
        synchronized(searchers)
        {
            IndexReader newReader = IndexReader.openIfChanged(lastReader.getReader());
            if(newReader==null && searchers.size()>0)
            {
                lastReader.incCount();
                return lastReader;
            }
            else
            {
                for(LuceneReaderAndSearcher searcher : searchers)
                {
                    searcher.setDirty(true);
                }
                lastReader = new LuceneReaderAndSearcher(newReader);
                searchers.add(lastReader);
                return lastReader;
            }
        }
    }


    private void clearAllDirtyUnused() throws IOException
    {
        synchronized(searchers)
        {
            List<LuceneReaderAndSearcher> remove = new ArrayList<LuceneReaderAndSearcher>();
            for(LuceneReaderAndSearcher searcher : searchers)
            {
                if(searcher.isDirty() && searcher.getCount()==0)
                    remove.add(searcher);
            }
            for(LuceneReaderAndSearcher searcher : remove)
            {
                searcher.close();
                searchers.remove(searcher);
            }
        }
    }


    public void releaseSearcher(LuceneReaderAndSearcher searcher)
    {
        if(searcher==null) return;
        try
        {
            synchronized(searchers)
            {
                searcher.decCount();
                if(searcher.getCount()==0 && searcher.isDirty())
                {
                    searcher.close();
                    searchers.remove(searcher);
                }
            }
        }
        catch (IOException e)
        {
            throw Util.wrap(e);
        }
    }


    private void openWriter() throws CorruptIndexException, LockObtainFailedException, IOException
    {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        if(index instanceof NRTCachingDirectory) config.setMergeScheduler(((NRTCachingDirectory)index).getMergeScheduler());
        try
        {
            writer = new IndexWriter(index, config);
        }
        catch(FileNotFoundException e)
        {
            writer = new IndexWriter(index, config);
        }
    }
    
    public void close() throws CorruptIndexException, IOException
    {
        writer.close();
        for(LuceneReaderAndSearcher searcher : searchers)
        {
            searcher.close();
        }
    }
    
    
    public Directory getIndex()
    {
        return index;
    }
    public void setIndex(Directory index)
    {
        this.index = index;
    }
    public IndexWriter getWriter()
    {
        return writer;
    }
    public void setWriter(IndexWriter writer)
    {
        this.writer = writer;
    }





    
    
    
}
