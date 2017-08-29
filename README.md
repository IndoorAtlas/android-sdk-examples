# IndoorAtlas SDK Examples for Android

[IndoorAtlas](https://www.indooratlas.com/) provides a unique Platform-as-a-Service (PaaS) solution that runs a disruptive geomagnetic positioning in its full-stack hybrid technology for accurately pinpointing a location inside a building. The IndoorAtlas SDK enables app developers to use high-accuracy indoor positioning in venues that have been fingerprinted.

This example app showcases the IndoorAtlas SDK features and acts as a reference implementation for many of the basic SDK features. Getting started requires you to set up a free developer account and fingerprint your indoor venue using the IndoorAtlas MapCreator tool.

There are also similar examples for iOS in [Objective-C](https://github.com/IndoorAtlas/ios-sdk-examples) and [Swift](https://github.com/IndoorAtlas/ios-sdk-swift-examples).

* [Getting Started](#getting-started)
    * [Set up your account](#set-up-your-account)
    * [Set up your API keys](#set-up-your-api-keys)    
* [Features](#features)
* [Documentation](#documentation)
* [SDK Changelog](#sdk-changelog)
* [License](#license)


## Getting Started

### Set up your account

* Set up your [free developer account](https://app.indooratlas.com) in the IndoorAtlas developer portal. Help with getting started is available in the [Quick Start Guide](http://docs.indooratlas.com/quick-start-guide.html).
* To enable IndoorAtlas indoor positioning in a venue, the venue needs to be fingerprinted with the [IndoorAtlas MapCreator 2](https://play.google.com/store/apps/details?id=com.indooratlas.android.apps.jaywalker) tool.
* To start developing your own app, create an [API key](https://app.indooratlas.com/apps).

### Set up your API keys

To run the examples you need to configure your IndoorAtlas API keys. If you do not have keys yet, go to https://app.indooratlas.com and sign up.

Once you have API keys, edit them into `gradle.properties` in the project root level.

## Features

These examples are included in the app:

* Basic Application: This app contains a number of small examples:
* Simple Example: This is the hello world of IndoorAtlas SDK. Displays received location updates as log entries.
* ImageView: Automatically downloads the floor plan that user has entered and displays it using Dave Morrissey's 
https://github.com/davemorrissey/subsampling-scale-image-view. This is a great library for handling large images!
* Google Maps - Basic: This is the hello world of IndoorAtlas SDK + Google Map. Shows received locations on world map. Does not retrieve floor plans.
* Google Maps - Overlay: Just like *Google Maps - Basic* but demonstrates how to place floor plan on world map by coordinates.
* Location sharing: Demonstrates sharing location via 3rd party cloud service. Can be used as an example of an multidot application.
* Set credentials: Demonstrates how to set IndoorAtlas credentials from code in runtime.
* Regions: Demonstrates automatic region changes.
* Background mode: Demonstrates running IndoorAtlas positioning in the background.
* Overlay with Open Street Map: Similar to Google maps examples, but uses Open Street Maps instead
* Orientation: Demonstrates IndoorAtlas 3D Orientation API.
* Geofences: Demonstrates how to set geofences and receive the geofence events.

## Documentation

The IndoorAtlas SDK API documentation is available in the documentation portal: http://docs.indooratlas.com/android/

## SDK Changelog

http://docs.indooratlas.com/android/CHANGELOG.html

## License

Copyright 2015-2017 IndoorAtlas Ltd. The IndoorAtlas SDK Examples are released under the Apache License. See the [LICENSE.md](https://github.com/IndoorAtlas/android-sdk-examples/blob/master/LICENSE.md) file for details.


