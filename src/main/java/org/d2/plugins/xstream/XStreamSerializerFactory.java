package org.d2.plugins.xstream;

import org.d2.serialize.D2Serializer;
import org.d2.serialize.SerializerFactory;

public class XStreamSerializerFactory implements SerializerFactory
{

    public D2Serializer createSerializer()
    {
        return new XStreamSerializer();
    }

}
