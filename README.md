# Questionmark app (Android)

This app is a light-weight wrapper around the [Questionmark website](https://www.thequestionmark.org/),
restricted to searching and viewing products. It adds barcode scanning as the main improvement over
the website.

To avoid having to change and publish the app when we add features, we just load the website on
app launch. In this way, we only need to maintain the website (taking care it looks well on mobile).

Features:

- Loads the website in an app, using [WebView](https://developer.android.com/develop/ui/views/layout/webapps/webview)
- Meant for [single page applications](https://en.wikipedia.org/wiki/Single-page_application).
- Works when online, a notice is shown when offline.
- Opens external links in the system web browser, internal links in the app.
- Website can indicate which links to open in the app. (now hardcoded, update by website pending)
- Allows scanning a barcode, initiated from the website.
- Works on Android.
- Written in Kotlin.

This app supersedes [an earlier Cordova-based approach](https://github.com/q-m/questionmark-app).
Maybe an iOS version will also appear at some point.

## Build

You can use [Android Studio](https://developer.android.com/studio) to develop, build and run the app.

From the command-line, you need a [Java Development Kit](https://openjdk.org/). Tested with version 21.
You may need the Android command-line tools as well. To produce a debug build, run:

```
./gradlew assembleDebug
```

to find an APK in `app/build/outputs/apk/debug/app-debug.apk`

## Release

A Github Action is used to build a release version when a new version is tagged, the resulting
APK is attached to the Github release, which you can upload to the Google Play Console.

To build a release version locally, run `./gradlew assembleRelease` but you may need to do something with keystores.

## Barcode scanner

The website can initiate a barcode scan by pointing to the custom url `app://mobile-scan`.
When this link is followed, the barcode scanner is opened. On successful scan, it will return
to the page indicated by the `ret` query string parameter passed that triggered opening the
scanner. This is a [URL template](https://en.wikipedia.org/wiki/URL_Template) where `{CODE}` is
replaced by the scanned barcode. Links can be relative or absolute.

An example. When following the link in the HTML shown below, a barcode scanner will
be opened, and when barcode `12345` is scanned, the link `http://x.test/scan/12345`
will be opened in the app.

```html
<a href="app://mobile-scan?ret=http%3A%2F%2Fx.test%2Fscan%2F%7BCODE%7D">
  Scan
</a>
```

## License

This project is licensed under the [MIT](LICENSE.md) license.
