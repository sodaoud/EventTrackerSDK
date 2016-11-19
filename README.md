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
