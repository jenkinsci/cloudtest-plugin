# SOASTA CloudTest Plugin for Jenkins

The CloudTest Jenkins plugin provides the ability to:

* easily run the MakeAppTouchTestable utility on an iOS or Android project
* silently install an iOS app on a connected device
* play CloudTest compositions and include the output in the build's test results

### Global Configuration Options

Before using the plug-in, you will need to provide the CloudTest server information, in the "Manage Jenkins" -&gt; "Configure System" page.
This includes the CloudTest URL and a set of credentials.  _We recommend creating a dedicated CloudTest account for Jenkins to use._

The CloudTest password is encrypted before saving it to disk.  It is also masked (e.g. "\*\*\*\*") in all output generated during builds.

### Build Steps

The plug-in adds the following new build steps:

##### Make App TouchTestable
Adds the TouchTest Driver library to your app's source code.  Typically this build step is inserted right before the build step that compiles the app's code (e.g. before the Xcode build step for iOS, or before the "Invoke Ant" build step for Android).

##### Install iOS App on Device
Silently installs an IPA file on one or more attached iOS devices.  This ensures that your tests run against the latest version of your app.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality._

##### Play Composition
Executes a CloudTest composition, and saves the output in the build's test results.  You can include this build step multiple times if there are multiple compositions in your test suite.

##### Wake Up iOS Device
Wakes up one or more attached iOS devices, and opens Mobile Safari to the most recently-viewed page (e.g. TouchTest Agent).  This can optionally be used at the beginning of a build, to "prep" the devices for testing.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality._

##### Reboot iOS Device
Reboots one or more attached iOS devices.  This can optionally be used at the end of a build, to "reset" for the next one.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality._
