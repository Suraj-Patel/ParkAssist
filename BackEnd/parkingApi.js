'use strict';
console.log("Loading function");

var es = require('elasticsearch');

var full_days = {
    "SUNDAY": "SUN",
    "MONDAY": "MON",
    "TUESDAY": "TUES",
    "WEDNESDAY": "WED",
    "THURSDAY": "THURS",
    "FRIDAY": "FRI",
    "SATURDAY": "SAT"
};

var all_days = ["SUN","MON","TUES","WED","THURS","FRI","SAT"];

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

    client.index({
        index: 'userloc',
        type: 'userloc',
        id: cognitoIdentityId,
        body: {
            location: {
                lat: lat,
                lon: lon
            },
            time: (new Date).getTime(),
            cid: endpoint
        }
    }, function (error) {
        if(error)   {
            client.close();
            console.error(error.message);
            callback(error)
        }   else    {
            client.search({
                index: 'segments',
                size: 5000,
                body: getQuery(lat,lon)
            }).then(function (resp) {

                client.close();
                callback(null, {
                    statusCode: 200,
                    body: JSON.stringify(getPoints(resp.hits.hits))
                });
            }, function (err) {
                client.close();
                console.error(err.message);
                callback(err)
            });
        }
    });
};

function getPoints(hits)    {
    "use strict";
    var points = [];
    var now = new Date();
    for(var h of hits)  {
        var prob = -1;
        if(h._source.rules.common.length)  {
            var r = parse(h._source.rules.common[0].desc);
            if(r)   {
                if(r.days.indexOf(all_days[now.getDay()]))  {
                    var time = r.from.match(/(\d+)(?::(\d\d))?\s*(P?)/);
                    var from = new Date();
                    from.setHours( parseInt(time[1]) + (time[3] ? 12 : 0) );
                    from.setMinutes( parseInt(time[2]) || 0 );
                    time = r.to.match(/(\d+)(?::(\d\d))?\s*(P?)/);
                    var to = new Date();
                    to.setHours( parseInt(time[1]) + (time[3] ? 12 : 0) );
                    to.setMinutes( parseInt(time[2]) || 0 );
                    if(from < now < to)    {
                        prob = 0
                    }   else    {
                        prob = 0.4;
                    }
                }   else    {
                    prob = 0.4;
                }
            }
        }

        points.push({
            points: [h._source.p1, h._source.p2],
            prob: prob
        });
    }
    return points
}


function parse(desc){
    "use strict";

    if(desc.indexOf("EXCEPT") != -1) {
        return;
    }

    desc = desc.replace("STANDING", "PARKING");
    desc = desc.replace("STOPPING", "PARKING");

    if(desc == "NO PARKING ANYTIME")   {
        return {
            'from': '12AM',
            'to': '11:59PM',
            'days': all_days
        };
    }

    if(desc.indexOf("NO PARKING") == -1) {
        return;
    }

    desc = desc.replace("MOON & STARS (SYMBOLS)", "");
    desc = desc.replace("(SANITATION BROOM SYMBOL)", "");
    desc = desc.replace("(DON'T LITTER)", "");
    desc = desc.replace("NO PARKING", "");
    desc = desc.replace("MIDNIGHT", "12AM");
    desc = desc.replace(" TO ", '-');
    desc = desc.trim();

    for(var long_day in full_days)   {
        desc = desc.replace(long_day, full_days[long_day])
    }

    var splitBySpace = desc.split(" ");
    var times, days, t1, t2;

    if(splitBySpace.length == 1)    {
        return;
    }

    if(splitBySpace[0].indexOf("-")!=-1)    {
        times = splitBySpace[0].split("-");
        if(times.length != 2)   {
            return;
        }
        t1 = times[0];
        t2 = times[1];
        days = splitBySpace.slice(1,splitBySpace.length);
    }   else if(splitBySpace[splitBySpace.length-1].indexOf("-")!=-1)   {
        times = splitBySpace[splitBySpace.length-1].split("-");
        if(times.length != 2)   {
            return;
        }
        t1 = times[0];
        t2 = times[1];
        days = splitBySpace.slice(0,splitBySpace.length-1);
    }   else    {
        return;
    }


    if(days.length == 3)    {

        var fromDay = all_days.indexOf(days[0]);
        var toDay =  all_days.indexOf(days[2]);
        if(days[1] == "&" && fromDay != -1 && toDay!=-1)  {
            return {
                'from': t1,
                'to': t2,
                'days': [days[0], days[2]]
            };
        }
        if(days[1] == "THRU" && fromDay != -1 && toDay!=-1)  {
            var ds = [];
            for(var d=fromDay; d<=toDay;d++)   {
                ds.push(d);
            }
            return {
                'from': t1,
                'to': t2,
                'days': ds
            };
        }
    }

    if(days.length == 2 && all_days.indexOf(days[0]) != -1 && all_days.indexOf(days[1]) != -1)    {
        return  {
            'from': t1,
            'to': t2,
            "days": days
        }
    }

    if(days.length == 1 && all_days.indexOf(days[0]) != -1){
        return {
            'from': t1,
            'to': t2,
            'days': days
        }
    }
}

function getQuery(lat,lon)  {
    return {
        query: {
            bool: {
                minimum_should_match: 1,
                should: [
                    {
                        "geo_distance": {
                            "distance": "1km",
                            "p1": {
                                "lat": lat,
                                "lon": lon
                            }
                        }
                    },
                    {
                        "geo_distance": {
                            "distance": "1km",
                            "p2": {
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