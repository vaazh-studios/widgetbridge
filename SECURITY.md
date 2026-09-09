# Security

WidgetBridge writes app data where a widget process reads it. A vulnerability here is a vulnerability in every app that uses it, so please report privately.

- Email founder@vaazhstudios.com with steps to reproduce. Expect an acknowledgement within three days.
- Please give us 90 days before public disclosure; we credit reporters in the changelog unless asked not to.
- In scope: anything that lets another app read the feed, or that makes a reader follow a path outside its generation folder. Out of scope: the cloud providers themselves, and apps that misuse the API.

Supported: the latest minor version. Fixes ship as a patch release and a note in `CHANGELOG.md`.
