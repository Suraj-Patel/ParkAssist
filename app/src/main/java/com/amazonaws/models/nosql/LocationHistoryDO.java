package com.amazonaws.models.nosql;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBIndexRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@DynamoDBTable(tableName = "parkassist-mobilehub-119988226-locationHistory")

public class LocationHistoryDO {
    private String _userId;
    private String _time;
    private Double _lat;
    private Double _lon;

    @DynamoDBHashKey(attributeName = "userId")
    @DynamoDBIndexHashKey(attributeName = "userId", globalSecondaryIndexNames = {"userId-lon","userId-lat",})
    public String getUserId() {
        return _userId;
    }

    public void setUserId(final String _userId) {
        this._userId = _userId;
    }
    @DynamoDBRangeKey(attributeName = "time")
    @DynamoDBAttribute(attributeName = "time")
    public String getTime() {
        return _time;
    }

    public void setTime(final String _time) {
        this._time = _time;
    }
    @DynamoDBIndexRangeKey(attributeName = "lat", globalSecondaryIndexName = "userId-lat")
    public Double getLat() {
        return _lat;
    }

    public void setLat(final Double _lat) {
        this._lat = _lat;
    }
    @DynamoDBIndexRangeKey(attributeName = "lon", globalSecondaryIndexName = "userId-lon")
    public Double getLon() {
        return _lon;
    }

    public void setLon(final Double _lon) {
        this._lon = _lon;
    }

}
