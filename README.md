# EEGsync RADAR-pRMT plugin

EEG synchronization plugin for the RADAR-AndroidApplication app, to be run on an Android 4.4 (or later) device, to enable sending synchronization pulses to an EEG machine.

## Installation

First, add the plugin code to your application:

```gradle
repositories {
    flatDir { dirs 'libs' }
    maven { url  'http://dl.bintray.com/radar-cns/org.radarcns' }
}

dependencies {
    compile 'org.radarcns:radar-android-eegsync:0.1-alpha.1'
}
```

## Contributing

To build this project, open it with Android Studio.

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html), except using 4 spaces as indentation. Make a pull request once the code is working.
