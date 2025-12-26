# Teledom Android

## Project History
This application was originally commissioned in 2020 by the telecom operator [LanTa](https://www.lanta-net.ru) (Tambov) from the mobile development studio [Mad Brains](https://madbrains.ru/) (Ulyanovsk) for a smart intercom project. From the very beginning it was an MVP capable of receiving video calls from Beward IP intercoms, opening doors, gates and barriers, accepting payments from customers, confirming user access to an address, submitting connection requests, displaying CCTV cameras with archive access, receiving and displaying text notifications, chatting with the operator, managing intercom settings, and managing access for other apartment residents.

Later, we continued developing this project on our own and extended it with additional features. We added overview cameras, an event log, face recognition settings, and also fixed bugs that kept appearing here and there during operation.

In October 2021 we decided to open-source the project and invite everyone interested in building similar services not to “reinvent the wheel” from scratch, but to develop this project together with us by sharing ideas and solutions. At that time the application was being used by around 15,000 users living in buildings equipped with our intercom panels and video surveillance systems.

## API
The application uses our own proprietary API. [(API link)](https://rosteleset.github.io/ApplicationAPI/)  
At the moment, the back-end source code implementing this API is tightly coupled with our overall architecture and all of our other systems, so at this stage we cannot offer anything better than implementing this API on your side by yourself.

The API documentation contains an example of integration between Asterisk and a mobile application using Linphone. Studying it is also useful for understanding the principles of building such a system.

## Frameworks and Components Used (Main)
* Liblinphone SDK for the SIP functionality
* Flussonic or Nimble for working with video archive
* Firebase Cloud Messaging for push notifications
* osmdroid for maps
* Crashlytics for crash reporting and analytics

## Design
You may also need to change something in the application design, or you may see references to screen numbers in the code. In that case, our [Figma application screen mockups](https://www.figma.com/file/bGLlEJbu8mVWY7gg4P0Hs2/%D0%9B%D0%B0%D0%BD%D1%82%D0%B0-App-(iOS%2BAndroid)-(Public-copy)?node-id=1377%3A0) may be useful.

## Frequently Asked Questions (FAQ)

* Where can I get `google-services.json` to build the application?

`google-services.json` is a project configuration file for Google Firebase that must be downloaded from the Firebase Console after registering your project there. Setup instructions: https://firebase.google.com/docs/android/setup

* How can I add my own server to the list of operators supported by the application?

Please send your request for inclusion to: sesameware@gmail.com or contact us via Telegram: https://t.me/+39S-IGTfmMdmZDJi

* How can I create my own version of the application based on the repository code?

Use the instructions at: https://developer.android.com/studio/build/build-variants

* How can I test the application over plain HTTP?

You need to build the project using the `teledomTest` flavor, which has the `cleartextTrafficPermitted="true"` attribute enabled in its settings.

## License and Terms of Use
This project is published under the standard open-source license [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html).

You are free to modify and use our work in your own projects, including commercial ones, provided that you publish the source code.

We are also happy to review your pull requests if you would like our project to evolve taking your modifications and improvements into account.
