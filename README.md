# EventTrackerSDK

## Senior SDK Developer Challenge Introduction

In this test you will be developing an application for the mobile platform for which you are applying (iOS or Android) This document outlines the requirements and functionalities your application must provide. In addition to the core requirements, feel free to give suggestions of how the functionality of your app could be extended. This test has been designed with a particular focus on SDK development. We define an SDK as a self- contained module that is an optional, defensive part of a software an application. Therefore, please make sure there are as few dependencies as possible between your SDK and the software application into which it is integrated.

### Challenge Description

Build a mobile SDK that allows an app developer to track user events in his application. Those events are sent to a Events REST API which is described bellow. The SDK you build must cover the following user stories. User Stories

 * As an app developer, I want to initialise the SDK with my API key
 * As an app developer, I want to track events with a custom name
 * As an app developer, I want to optionally add a list of key/value parameters to each event
 * As an app developer, I want any events which were tracked while the app was offline to be delivered when again online
 * As an app developer, I want to have a sample app that can be used to demonstrate the SDK functionality
       
In this context, the App Developer is the person who will use and integrate the SDK you build. For the App Developer, the SDK should be a "black box" providing only the public functionalities described by the user stories.

### Constraints

From the above requirements we can derive the following quality criteria which we will use to assess your solution.
Functional design

 * Code best practices
 * Test design and coverage
 * SDK stability
 * The application must be easy to compile and deploy on a physical device
 * The challenge should not take more than 5 days
 * The source code should be available for us via Bitbucket or GitHub
 * Think of this project as something that would be continued with a team as an actual SDK
 
 ### Setup
 
Download the AAR file and copy it into the folder libs of your Android application project, add the dependecy into the build.gradle file.
 
Initialize the EventTracker using a context object and the Api Key:
     
    EventTracker.init(mContext, "your_api_key");
     
Track any event in the application using a name that matches the regex \'^ev_[A-Za-z0-9]+\':
    
    String name = "ev_test1";
    EventTracker.getInstance().sendEvent(name);

You can add a Map of custom params to the event:
    
    String name = "ev_test2";
    Map<String, String> params = new HashMap<>();
    params.put("key", "value");
    EventTracker.getInstance().sendEvent(name, params);

You can test if a name matches the regex using:

    EventTracker.getInstance().validateName(name);

All events will be delivered to the server, there is two configurations for the delivery, by number of events (Default is 1) and periodically, You can change the configuration any time:

    EventTracker.getInstance().setPostType(EventTracker.POST_TYPE_PERIODIC); // sets the configuration to periodic
    EventTracker.getInstance().setPeriodicTime(5000); // sets the period to 5s

You can also change the number of events tracked before delivery:

    EventTracker.getInstance().setPostType(EventTracker.POST_TYPE_NUMBER); // sets the configuration to number
    EventTracker.getInstance().setNumberOfEventsToPost(30); // sets the number to 30 events until delivery

The EventTracker uses HttpURLConnection to deliver events unless the application developer has included OkHttp3 in his project than the EventTracker will use OkHttp3, the developer can than sets a custom client:

    OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.SECONDS)
                        .build();
    EventTracker.getInstance().setClient(client);
 

