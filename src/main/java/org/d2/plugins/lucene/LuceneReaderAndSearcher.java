package org.d2.plugins.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class LuceneReaderAndSearcher
{
    private IndexReader reader;
    private IndexSearcher searcher;
    private int count;
    private boolean dirty;
    
    public LuceneReaderAndSearcher(IndexReader reader)
    {
        super();
        this.reader = reader;
        this.searcher = new IndexSearcher(reader);
        this.count = 0;
    }
    
    public void incCount()
    {
        count++;
    }
    public void decCount()
    {
        count--;
    }
    public void close() throws IOException
    {
        searcher.close();
        reader.close();
    }
    
    public IndexReader getReader()
    {
        return reader;
    }
    public void setReader(IndexReader reader)
    {
        this.reader = reader;
    }
    public IndexSearcher getSearcher()
    {
        return searcher;
    }
    public void setSearcher(IndexSearcher searcher)
    {
        this.searcher = searcher;
    }
    public int getCount()
    {
        return count;
    }
    public void setCount(int count)
    {
        this.count = count;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }

}
