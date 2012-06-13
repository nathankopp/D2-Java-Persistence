package org.d2.test.basic;

import org.d2.D2Metadata;
import org.d2.annotations.D2Entity;
import org.d2.annotations.D2Id;
import org.d2.annotations.D2Indexed;

@D2Entity( alias="Location", bucketName="locations")
public class Location
{
    protected D2Metadata md;
    
    @D2Id private String id;
    private Double minLat;
    private Double maxLat;
    
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    @D2Indexed(analyzed=true) public Double getMinLat()
    {
        return minLat;
    }
    public void setMinLat(Double lat)
    {
        this.minLat = lat;
    }
    @D2Indexed(analyzed=true) public Double getMaxLat()
    {
        return maxLat;
    }
    public void setMaxLat(Double lon)
    {
        this.maxLat = lon;
    }

}
