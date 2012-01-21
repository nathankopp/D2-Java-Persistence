package org.d2;

import org.d2.context.D2Context;

public interface UniqueConstraint
{
    public Object applyConstraint(Object o, D2Context context);
}
