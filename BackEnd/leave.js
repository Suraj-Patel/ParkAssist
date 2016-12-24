/**
 * Created by karan on 12/22/16.
 */
'use strict';
console.log("Loading function");

var es = require('elasticsearch');

var AWS = require('aws-sdk');

var sns = new AWS.SNS({
    region: 'us-east-1'
});

exports.handler = function (event, context, callback) {

    context.callbackWaitsForEmptyEventLoop = false;

    var client = new es.Client({
        host: "AWS_ELASTICSEARCH_HOST",
        log: 'warning'
    });

    var responseCode = 200;
    var requestBody, pathParams, queryStringParams, headerParams, stage,
        stageVariables, cognitoIdentityId, httpMethod, sourceIp, userAgent,
        requestId, resourcePath;
    console.log("request: " + JSON.stringify(event));

    // Query String Parameters
    queryStringParams = event.queryStringParameters;

    if (event.requestContext !== null && event.requestContext !== undefined) {

        var requestContext = event.requestContext;

        var identity = requestContext.identity;

        // Amazon Cognito User Identity
        cognitoIdentityId = identity.cognitoIdentityId;

        // Source IP
        sourceIp = identity.sourceIp;

        // User-Agent
        userAgent = identity.userAgent;
    }

    // API Gateway Stage Variables
    stageVariables = event.stageVariables;

    // HTTP Method (e.g., POST, GET, HEAD)
    httpMethod = event.httpMethod;

    var lat = ((queryStringParams && 'lat' in queryStringParams) ? queryStringParams['lat'] : 40.7128);
    var lon = ((queryStringParams && 'lon' in queryStringParams) ? queryStringParams['lon'] : -74.0059);

    var endpoint = ((queryStringParams && 'endpoint' in queryStringParams) ? queryStringParams['endpoint'] : "");

    client.search({
        index: 'userloc',
        size: 1000,
        body: getQuery(lat,lon, endpoint)
    }).then(function (resp) {

        client.close();
        sendSNS(resp.hits.hits, lat, lon, endpoint, function(){
            callback(null, {
                statusCode: 200,
                body: "SUCCESS"
            });
        });
    }, function (err) {
        client.close();
        console.error(err.message);
        callback(err)
    });
};

function sendSNS(hits, lat, lon, endpoint, cb)  {
    console.log(hits);
    if(hits.length) {
        sendMessage(hits, lat, lon, endpoint, cb)
    }   else {
        cb()
    }
}

function getQuery(lat,lon, endpoint)  {
    return {
        query: {
            bool: {
                must_not: {
                    term: {
                        "endpoint": endpoint
                    }
                },
                must: [
                    {
                        "geo_distance": {
                            "distance": "2km",
                            "location": {
                                "lat": lat,
                                "lon": lon
                            }
                        }
                    }
                ]
            }
        }
    }
}

function sendMessage(hits, lat, lon, endpoint, cb)  {
    if(!hits.length) {
        return cb();
    }
    var hit = hits.pop();
    if(hit._source.cid != endpoint) {
        var params = {
            Message: JSON.stringify({
                'lat': lat,
                'lon': lon,
                'time': (new Date()).getTime()
            }),
            Subject: 'PARKING SPOT OPEN',
            TargetArn: hit._source.cid
        };
        sns.publish(params, function(err, result) {
            if (err) console.error(err, err.stack); // an error occurred
            else console.log(result);
            sendMessage(hits, lat, lon, endpoint, cb)
        });
    }   else    {
        sendMessage(hits, lat, lon, endpoint, cb)
    }
}