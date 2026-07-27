## History of major changes

### Version 1.0.98
* Optimized WebRTC streams to prevent device overheating, video freezes, and interface slowdowns
* Added automatic fallback to HLS when the WebRTC stream fails to load frames
* Enhanced Custom Web Views (Web Extensions): added JS APIs to control the status bar appearance and improved handling of large extensions to prevent app crashes
* Improved downloading of video recordings from the notifications screen (fixed interception of download links)
* Made NFC hardware requirement optional to support a wider range of devices
* Added an "Add address" button to the end of the settings list
* Improved global UI layout: fixed content overlapping with the bottom navigation bar across multiple screens
* Fixed scrolling, swiping, and drag-to-sort conflicts in the address list
* Minor fixes and code refactoring


### Version 1.0.92
* Added switching entrance view mode via ProviderConfig
* Replaced bottom navigation menu
* Minor fixes and code refactoring

### Version 1.0.88
* Added stories to the address tab
* The UI for displaying address list entries has been slightly modified: you can now view live WebRTC streams from CCTV cameras at the entrances
* Added tracking of intercom opening events from the app, by key, by code, and by license plate number
* Added NFC bridge to the custom web views
* Minor fixes and code refactoring

### Version 1.0.85
* Added deep link processing for QR code registration
* Added dtmfProtocol support in the incomming call data
* Fixed edge-to-edge
* Added License Plate Recognition support
* Minor fixes and code refactoring

### Version 1.0.81

* Added optional Support incoming call mode.
* Added altCameras attribute to the CamMapResponse
* Fixed account creating in LinphoneProvider
* Added web extensions to the address list
* Fixed config changes restrictions
* Fixed coupling of player sound settings
* Fixed days of the week in the calendar for en locale

### Version 1.0.80

* Fixed mute button in the event item
* Fixed days of the week in the calendar for en locale
* Added ext options to the appeal form and address verification UIs
* Added regex validation for name and patronymic to the AppealForm
* Added notification id to event notifications
* Fixed event record clipping
* Fixed system bars appearance in the incoming call and fullscreen mode
* Update libraries for 16kb alignment
* Added showing push event for RFID
* Fixed System Bars appearance
* Fixed bump animation in the bottom navigation bar

### Version 1.0.76

* Fixed animation in the bottom menu
* Updated minSdk to 24 (Android 7.0)
* Updated target SDK to 35
* Added timeout to snapshot
* Downgraded linphone version to 5.3.19
* Fixed peephole in WebRTC
* Added handling of archive absence
* Minor code fixes and refactoring

### Version 1.0.74

* Sending one DTMF signal via SIP INFO
* Minor fixes

### Version 1.0.73

* Added requesting permission to show app on lock screen for Android 14+.
* Added transition to incoming call ringtone settings.
* New call processing logic: start incoming call actions after notification.
* Fixed WebRTC URL for the incoming calls.
* Added new event to event details: EVENT\_OPEN\_GATES\_BY\_VEHICLE.

### Version 1.0.70

* Add bundle attribute to the registerPushToken API method.

### Version 1.0.68

* Improved Foreground Service usage during the incoming call.
* Add deviceToken to API calls confirmCode, requestCode and checkPhone.
* Fixed notification for 425 response code.
* Fixed navigation to AuthFragment when house list is empty.
* Fixed double sms send in rare cases when entering phone number via google keyboard hint.
* Added OkHttp logs to Crashlytics.
* Updated peephole behavior during a call when there is WebRTC stream.
* Added drag to sort feature to AddressFragment for House Items.
* Turned off default system melody in linphone during a call.
* Code refactoring.

### Version 1.0.66

* Fix checking SDK for incoming calls

### Version 1.0.64

* Add onShowFileChooser to custom web view.
* Remove Huawei Analytics.

### Version 1.0.63

* Add new value userDefined to cctvView configuration parameter.

### Version 1.0.62

* Taking into account DND mode during an incoming call.
* Fix crash in deleting huawei push token.
* Add more attributes to event details.

