package org.d2.serialize;

import java.util.Date;

import org.d2.Bucket;
import org.d2.D2Impl;
import org.d2.Operation;
import org.d2.context.D2Context;

public interface D2Serializer
{

    public abstract String serialize(Object obj);

    public abstract Object deserialize(String str, Object obj);

    public abstract void prepareForRootBucket(Class<?> clazz, D2Impl d2Impl, Date now, D2Context context,
                                              Operation operation, Bucket bucket);

    public abstract void prepareForNonRootBucket(D2Impl d2Impl, D2Context context, Operation operation, Bucket bucket);

}
