# Changelog

All notable changes for the Xatkit runtime component will be documented in this file.

Note that there is no changelog available for the initial release of the platform (2.0.0), you can find the release notes [here](https://github.com/xatkit-bot-platform/xatkit-runtime/releases).

The changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/v2.0.0.html)

## Unreleased

### Changed

- `JsonEventMatcher` now logs the builder content even if the intent definition is not known. This allows to inspect the logs and copy-paste new events easily in the platform editor (the builder content is populated from the received JSON payload and is printed using the *platform language* syntax).
- `XatkitCore` now loads `.execution` files instead of `.xmi`. **This change breaks the public API**: existing  `.properties` file need to be updated with the path to the `.execution` file.
- `ExecutionService` now inherits from `XbaseInterpreter`, offering complete support for Xbase expressions in the execution models. **This change breaks the public API**: the interpreter public methods have changed to reflect this integration.

## [3.0.0] - 2019-10-10

### Added

- Class `XatkitServerUtils` containing the configuration keys and default values related to the Xatkit server.
- Xatkit now creates a `public` directory relative to the properties file that is used to store files that can be accessed through an URL.
- Class `ContentHttpHandler` allowing to access files in the `public` directory through public URLs. The URLs must follow this template: `/content/my_file`, where `my_file` is an existing file located at `public/my_file`. The `XatkitServer` class now provides utility methods to create and update public files.

### Changed

- Refactored the Intent/Event provider hierarchy and integrate it with the `RestHandler` infrastructure of Xatkit server. All the providers are now defined in an `EventProvider` hierarchy, and can use the `IntentRecognitionHelper` to recognize intents from inputs when needed. **This change breaks the public API**. See issues [#219](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/219), [#220](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/220), and [#221](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/221) for more information.
- The directory storing monitoring data is now relative to the properties file. See issue [#243](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/243) for more information.
- Moved `CONFIGURATION_FOLDER_PATH` from `Xatkit` to `XatkitCore`. This allows to easily create file/directory relative  to the properties file. **This change breaks the public API**.
- HTTP header matching is now case insensitive. This fixes issues related to server-specific policy for headers (e.g. ngrok forwards them without any processing, while serveo forwards them in lower case).
- Intent libraries are now loaded the same way as platforms (using `$XATKIT/plugins/libraries/` directory). See issue [#249](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/249) for additional information.
- The generated HTML file to test react-based bots (accessible at `localhost:5000/admin`) now connects to the react platform using `localhost:5001` instead of `localhost:5000/react`. This change is related to the new socket infrastructure used in Xatkit react platform (see the changelog [here](https://github.com/xatkit-bot-platform/xatkit-react-platform/blob/master/CHANGELOG.md)).

### Removed

- Removed *core library*: the library is now a [standalone project](https://github.com/xatkit-bot-platform/xatkit-core-library) and is loaded the same way as platforms (using the `$XATKIT/plugins/` directory). See issue [#249](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/249) for additional information.
- Removed `SERVER_PORT_KEY` and `DEFAULT_SERVER_PORT`  from `XatkitServer` and moved them to `XatkitServerUtils`. **This change breaks the public API**.
- Removed *Xatkit metamodels* from the Xatkit runtime project. Xatkit runtime now has an explicit dependency to [xatkit-metamodels](https://github.com/xatkit-bot-platform/xatkit-metamodels).
- Removed *Xatkit core resources* from the Xatkit runtime project. The *core resources* are not needed anymore since all the platforms and libraries are loaded using the `$XATKIT/plugins` directory.

### Fixed

- Fixed an issue that caused `HttpHandler` to ignore single-line JSON payloads.

## [2.0.0] - 2019-08-20 

See the release notes [here](https://github.com/xatkit-bot-platform/xatkit-runtime/releases).

