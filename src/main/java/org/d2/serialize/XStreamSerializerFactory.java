package org.d2.serialize;

public class XStreamSerializerFactory implements SerializerFactory
{

    public D2Serializer createSerializer()
    {
        return new XStreamSerializer();
    }

}
