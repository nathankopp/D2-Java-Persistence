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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.d2.index.DocBuilderAbstract;
import org.nkts.util.Util;


public class LuceneDocBuilder extends DocBuilderAbstract
{
    public static final String MAGIC_BLANK_VALUE = "_blank_";

    @Override
    public void addField(String fieldName, String value, Object doc, boolean store, boolean analyze)
    {
        Document d = (Document)doc;
        if(!Util.isBlank(value)) d.add(new Field(fieldName, value, store?Field.Store.YES:Field.Store.NO, analyze?Field.Index.ANALYZED:Field.Index.NOT_ANALYZED_NO_NORMS));
    }

    @Override
    public void addField(String fieldName, Number value, Object doc, boolean store, boolean analyze)
    {
        if(value==null) return;
        Document d = (Document)doc;
        NumericField field = new NumericField(fieldName, store?Field.Store.YES:Field.Store.NO, true);
        if(Double.class.isAssignableFrom(value.getClass()))       field.setDoubleValue((Double)value);
        else if(Long.class.isAssignableFrom(value.getClass()))    field.setLongValue((Long)value);
        else if(Float.class.isAssignableFrom(value.getClass()))   field.setFloatValue((Float)value);
        else if(Integer.class.isAssignableFrom(value.getClass())) field.setIntValue((Integer)value);
        d.add(field);
    }

    @Override
    public void addIdField(String field, Object doc)
    {
        Document d = (Document)doc;
        d.add(new Field("id", field, Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    @Override
    public void addNonstoredAnalyzedField(String fieldName, String field, Object doc)
    {
        Document d = (Document)doc;
        if(!Util.isBlank(field)) d.add(new Field(fieldName, field, Field.Store.NO, Field.Index.ANALYZED));
    }

    @Override
    public void addStoredAnalyzedField(String fieldName, String field, Object doc)
    {
        Document d = (Document)doc;
        if(!Util.isBlank(field)) d.add(new Field(fieldName, field, Field.Store.YES, Field.Index.ANALYZED));
    }

    @Override
    public void addStoredUnanalyzedField(String fieldName, String field, Object doc)
    {
        Document d = (Document)doc;
        if(!Util.isBlank(field)) d.add(new Field(fieldName, field, Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    @Override
    public Object createDoc()
    {
        return new Document();
    }

    @Override
    public String getMagicBlankValue()
    {
        return MAGIC_BLANK_VALUE;
    }

}
