# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# This example uses Picasso, see: https://github.com/square/picasso/blob/master/README.md
-dontwarn com.squareup.okhttp.**

# With Android 6.0, Apache Http is dropped but that is OK since its not being used here.
-dontwarn org.apache.http.**
-dontwarn android.net.http.**

# Required by IndoorAtlas SDK
-keep public class com.indooratlas.algorithm.ClientProcessingManager { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
