# Android-SDK-Examples
Example applications for IndoorAtlas Android SDK

## Setup

To run examples you need to configure your IndoorAtlas API keys. If you do not have keys yet, 
go to https://developer.indooratlas.com and sign up.

Once you have API keys, edit them into `gradle.properties` in the project root level.


For more info visit our documentation site: http://docs.indooratlas.com.


## Examples

### Basic Application
This app contains a number of small examples:

#### Simple Example
This is the hello world of IndoorAtlas SDK. Displays received location updates as log entries.

#### ImageView
Automatically downloads the floor plan that user has entered and displays it using Dave Morrissey's 
https://github.com/davemorrissey/subsampling-scale-image-view. This is a great library for handling large images!

#### Google Maps - Basic
This is the hello world of IndoorAtlas SDK + Google Map. Shows received locations on world map. Does not retrieve 
floor plans.

#### Google Maps - Overlay
Just like *Google Maps - Basic* but demonstrates how to place floor plan on world map by coordinates.

