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

To run the examples you need to configure your IndoorAtlas API keys. If you do not have keys yet, go to <https://app.indooratlas.com> and sign up.

Once you have API keys, edit them into `gradle.properties` in the project root level.

## Examples


### Simple Example

* [Simple Example](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/simple): This is the hello world of IndoorAtlas SDK. Displays received location updates as log entries.

![](/example-screenshots/simple_01.jpg)


### Imageview Example

* [ImageView](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/imageview): Automatically downloads the floor plan that user has entered and displays it using Dave Morrissey's 
<https://github.com/davemorrissey/subsampling-scale-image-view>. This is a great library for handling large images! The example also demonstrates smoothly animating the blue dot and how to set up OrientationListener for obtaining device heading
 information.

![](/example-screenshots/imageview_02.jpg)


### Google Maps - Overlay Example

* [Google Maps](https://github.com/IndoorAtlas/android-sdk-examples/blob/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/mapsoverlay) - Overlay: Just like *Google Maps - Basic* but demonstrates how to place floor plan on world map by coordinates.

![](/example-screenshots/googlemaps&#32;-&#32;overlay_04.jpg)


### Open Street Map Overlay Example

* [Overlay with Open Street Map](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/osmdroid): Similar to Google maps examples, but uses Open Street Maps instead

![](/example-screenshots/open-street-map_08.jpg)


### Automatic Venue and Floor Detection Example

* [Automatic Venue and Floor Detection](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/regions): Demonstrates automatic region changes i.e. automatic venue detection and floor detection.

![](/example-screenshots/regions_07.jpg)


### Wayfinding Example

* [Wayfinding Example](https://github.com/IndoorAtlas/android-sdk-examples/blob/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/wayfinding/WayfindingOverlayActivity.java#L260): In this example, a wayfinding graph json file is loaded. On the UI, you'll see your current location, and when you tap another point on the floorplan, you'll be shown a wayfinding route to that location. 

* Note: to setup, you need to draw a wayfinding graph for your venue using app.indooratlas.com and save it. Obviously you also need to fingerprint the venue and generate a map. 

![](/example-screenshots/wayfinding_12.jpg)


### Location Sharing aka "Find your friend" Example

* [Location sharing](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/sharelocation): Demonstrates sharing location via 3rd party cloud service. Can be used as an example of an multidot application.

![](/example-screenshots/sharelocation-05.jpg)


### Foreground Service Positioning Example

* [Foreground Service Positioning Example](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/foregroundservice): Demonstrates running IndoorAtlas positioning when the app is in the background, using an Android Foreground Service.

![](/example-screenshots/foreground-service_14.png)


### Geofences Example

* [Geofences](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/geofence): Demonstrates how to set geofences and receive the geofence events.

![](/example-screenshots/geofences_10.png)


### Orientation Example

* [Orientation](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/orientation): Demonstrates IndoorAtlas 3D Orientation API.

![](/example-screenshots/orientation_09.jpg)


### Set Credentials from Code Example

* [Set credentials](https://github.com/IndoorAtlas/android-sdk-examples/tree/master/Basic/src/main/java/com/indooratlas/android/sdk/examples/credentials): Demonstrates how to set IndoorAtlas credentials from code in runtime.

![](/example-screenshots/set-credentials_06.jpg)


## Documentation

The IndoorAtlas SDK API documentation is available in the documentation portal: <http://docs.indooratlas.com/android/>

## SDK Changelog

<http://docs.indooratlas.com/android/CHANGELOG.html>

## License

Copyright 2015-2019 IndoorAtlas Ltd. The IndoorAtlas SDK Examples are released under the Apache License. See the [LICENSE.md](https://github.com/IndoorAtlas/android-sdk-examples/blob/master/LICENSE.md) file for details.


