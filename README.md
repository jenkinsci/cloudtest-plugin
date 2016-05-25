# SOASTA CloudTest Plugin for Jenkins

The CloudTest Jenkins plugin provides the ability to:

* easily run the MakeAppTouchTestable utility on an iOS or Android project
* silently install an iOS app on a connected device
* play CloudTest compositions and include the output in the build's test results

### Pre-requisites
The CloudTest plug-in requires Jenkins 1.580.1 or later.

### Future development on the plugin
* mvn hpi:run -Djetty.port=8081
* To debug from eclipse: Select debug configs, create a new maven debug config. Add the goal clean hpi:run, and jetty.port=8081 under param values. 

### Global Configuration Options

Before using the plug-in, you will need to provide the CloudTest server information, in the "Manage Jenkins" -&gt; "Configure System" page.
This includes the CloudTest URL and a set of credentials.  _We recommend creating a dedicated CloudTest account for Jenkins to use._

If you wish to deploy CloudTest test environments we also suggest you setup a CloudTest Server that points to CloudTest Manager, at https://cloudtestmanager.soasta.com/concerto/ . You will not need to create a dedicated CTM account, the Jenkins plugin will not log you out. 

The CloudTest password is encrypted before saving it to disk.  It is also masked (e.g. "\*\*\*\*") in all output generated during builds.

### Build Steps

The plug-in adds the following new build steps:

##### Make App TouchTestable
Adds the TouchTest Driver library to your app's source code or to your iOS ipa, app, or apk file.  For adding the TouchTest Driver library to the source, typically this build step is inserted right before the build step that compiles the app's code (e.g. before the Xcode build step for iOS, or before the "Invoke Ant" build step for Android).  If you are using an ipa or app file instead, an TouchTestable ipa will be created.  If you are using an apk file, an apk file will be created and returned.  You must provide the necessary provisioning profile and signing key so the resulting ipa can be properly installed onto an Apple device.

##### Install iOS App on Device
Silently installs an IPA file on one or more attached iOS devices.  This ensures that your tests run against the latest version of your app.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality ("adb install")._

##### Play Composition
Executes a CloudTest composition, and saves the output in the build's test results.  You can include this build step multiple times if there are multiple compositions in your test suite.

##### Start Grid
Starts the grid with the specified CloudTest server, and name, and ensures that the object reaches a ready status. In the event that the grid fails to reach a ready status, it will be torn down and the build step will fail. 

##### Stop Grid
Terminates the grid, and ensures that the grid reaches a terminated status. In the event that the grid fails to reach a terminated status, the build step will fail.

##### Start RSDB
Starts the RSDB with the specified CloudTest server, and name, and ensures that the RSDB reaches a ready status. In the event that the RSDB fails to reach a ready status, it will be torn down and the build step will fail. 

##### Stop RSDB
Terminates the RSDB, and ensures that the RSDB reaches a terminated status. In the event that the grid fails to reach a terminated status, the build step will fail.  

##### Test Environments 
Note that for test environments you will need to set up a CloudTest Server to point to CloudTest Manager https://cloudtestmanager.soasta.com/concerto/.  

##### Start Test Environment
Starts the test environment with the specified CloudTest server (Note this MUST be CTM), and name, and ensures that the object reaches a ready status. In the event that the test environment fails to reach a ready status, it will be torn down and the build step will fail. 

##### Stop Test Environment
Terminates the test environment, and ensures that the test environment reaches a terminated status. In the event that the test environment fails to reach a terminated status, the build step will fail.


##### Wake Up iOS Device
Wakes up one or more attached iOS devices, and opens Mobile Safari to the most recently-viewed page (e.g. TouchTest Agent).  This can optionally be used at the beginning of a build, to "prep" the devices for testing.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality._

##### Reboot iOS Device
Reboots one or more attached iOS devices.  This can optionally be used at the end of a build, to "reset" for the next one.  _NOTE: there is no Android version of this build step, because the Android SDK already provides this functionality ("adb reboot")._
