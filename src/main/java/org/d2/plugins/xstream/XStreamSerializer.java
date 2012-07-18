package org.d2.plugins.xstream;

import java.util.Date;

import org.d2.Bucket;
import org.d2.D2Impl;
import org.d2.Operation;
import org.d2.context.D2Context;
import org.d2.plugins.xstream.converters.D2EntityReflectionConverter;
import org.d2.plugins.xstream.converters.D2IgnoreClassConverter;
import org.d2.plugins.xstream.converters.D2XmlEntityConverter;
import org.d2.serialize.D2Serializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

public class XStreamSerializer implements D2Serializer
{
    public XStream xs = new XStream();
    
    public XStreamSerializer()
    {
        this.xs.registerConverter(new D2EntityReflectionConverter(this.xs.getMapper(), this.xs.getReflectionProvider()));
        this.xs.registerConverter(new D2IgnoreClassConverter());
    }

    public String serialize(Object obj)
    {
        return xs.toXML(obj);
    }
    
    public Object deserialize(String str, Object obj)
    {
        return xs.fromXML(str, obj);
    }

    public void prepareForRootBucket(Class<?> clazz, D2Impl d2Impl, Date now, D2Context context, Operation operation, Bucket bucket)
    {
        Converter delegateConverter = this.xs.getConverterLookup().lookupConverterForType(bucket.getClazz());
        this.xs.registerConverter(new D2XmlEntityConverter(d2Impl, bucket.getClazz(), delegateConverter, now, context, operation));
        this.xs.alias(d2Impl.getAlias(clazz), clazz);
        this.xs.alias(d2Impl.getAlias(bucket.getClazz()), bucket.getClazz());
    }

    public void prepareForNonRootBucket(D2Impl d2Impl, D2Context context, Operation operation, Bucket bucket)
    {
        Converter delegateConverter = this.xs.getConverterLookup().lookupConverterForType(bucket.getClazz());
        this.xs.registerConverter(new D2XmlEntityConverter(d2Impl, bucket.getClazz(), delegateConverter, context, operation));
//                    this.xs.registerConverter(bucket.getConverter());
        this.xs.alias(d2Impl.getAlias(bucket.getClazz()), bucket.getClazz());
    }
    
    
}
