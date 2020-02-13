# Changelog

All notable changes for the Xatkit runtime component will be documented in this file.

Note that there is no changelog available for the initial release of the platform (2.0.0), you can find the release notes [here](https://github.com/xatkit-bot-platform/xatkit-runtime/releases).

The changelog format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/v2.0.0.html)

## Unreleased

### Added

- Utility test class `AbstractActionTest` and `AbstractEventProviderTest`that eases the definition of action and event provider test classes, respectively.
- Class `EmptyContentRestHandler` which can be used to define REST endpoints that do not accept any request content (this is for example the case for most of the *GET* requests). 
- Support for pre/post processors wrapping intent recognition (see [#252](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/252)). This allows to define specific functions to adapt the textual input (e.g. to improve the intent recognition quality), as well as the computed intent (e.g. to normalize values in extracted contexts). **This change breaks the public API**: the `IntentRecognitionProvider` interface is now an abstract class, existing classes implementing the interface should now extend it.
- Post-processor `RemoveEnglishStopWordsPostProcessor` that removes English stop words from recognized intent's parameter values that have been extracted from `any` entities. This processor should help normalizing DialogFlow values when using `any` entities (see [#265](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/265)). The processor can be activated using the following property: `xatkit.recognition.postprocessors = RemoveEnglishStopWords`.
- Hook method `RuntimeArtifactAction#beforeDelay(delay)` that can be extended by concrete messaging actions that need to perform any computation before a potential message delay (specified with `xatkit.message.delay`). This is for example the case if the action needs to notify a client to print a waiting message or loading dots. This change does not break the public API: the hook does nothing if not implemented.
- Support for `CompositeEntity` in the DialogFlow intent provider (see [#271](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/271)). The keyword was already present in the language but the mapping to DialogFlow wasn't working properly (nested values were not supported by the connector). `CompositeEntities` can be accessed as multi-level maps, e.g. `(context.get("context").get("composite") as Map).get("nested1")`.
- New utility methods in `HttpEntityHelper` to create and parse `HttpEntity` instances.

## Changed

- `HttpHandler` now supports `HttpEntity` instances returned from `RestHandler#handle`. This allows to define handlers that directly return a valid `HttpEntity` (e.g. the content of an HTML page). In this case, the `RestHandler` implementation is responsible of the `HttpEntity` creation.
- `RestHandler` instances can now throw a `RestHandlerException` to notify the server that an error occurred when handling the request. For now this exception is used to return a *404* status code instead of *200*.
- Change log level of non-critical messages in `XatkitServer`, `DialogFlow` and `RegEx` intent recognition providers. This reduces the amount of noise in Xatkit logs.
- `RestHandlerException` now supports HTTP error codes, and the `HttpHandler` uses this value to return the corresponding HTTP status to the client.
- Monitoring endpoint `GET: /analytics/monitoring/session` now returns a `404` with an error message if the `sessionId` parameter is missing or if the corresponding session cannot be found. **This change breaks the public API**: client application expecting a status code `200` should be updated.

## Removed

- Class `AdminHttpHandler`: this class was designed to test the *ReactPlatform*, it didn't make sense to keep it in *xatkit-runtime*. The functionality of the `AdminHttpHandler` are still available when starting a bot based on the *ReactPlatform*, but the handlers are now defined in the [*xatkit-react-platform*](https://github.com/xatkit-bot-platform/xatkit-react-platform) project.

## Fixed

- [#251](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/251): *AdminHttpHandler should be moved to react platform*
- The `XatkitServer` now correctly returns a `404` error if there is no `RestHandler` associated to a requested URI.
- [#267](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/267): *Change log level of XatkitServer logs to debug*
- [#269](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/269): *Reduce default logging of DialogFlow API*
- [#252](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/252): *Pre/Post processing of user inputs*
- [#265](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/265): *An option to remove stop words from parameters extracted with any*
- [#271](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/271): *Add support for composite entity*
- [#279](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/279): *Monitoring API: error responses should use proper HTTP codes*

## [4.0.0] - 2019-12-01

### Added

- Configuration option `xatkit.message.delay` that allows to specify a delay (in milliseconds) the bot should wait for before sending a message (`0` by default, meaning that the bots replies immediately). This option impacts all the `RuntimeActions` inheriting from `RuntimeArtifactMessage`.
- Support for *from clause* in execution rule. Execution rules now accept an optional `from <PlatformDefinition>` that allows to filter execution rules based on the platform that triggered the event. When the a *from clause* is specified Xatkit will take it into account to only execute the rules matching both the triggered event and the specified platform. This allows to define precise bot interactions when manipulating multiple messaging platforms.
- Support for all the Http methods supported by Apache http-core in XatkitServer (fix [#222]( https://github.com/xatkit-bot-platform/xatkit-runtime/issues/222 )). This includes requests with parameters (`?param=value`), that are correctly mapped to the handler corresponding to their base URI. **This change breaks the public API**: it is now required to specify the `HttpMethod` when registering a rest handler.
- We have replaced the previous monitoring API with a brand new REST API that will offer us the required flexibility to expose additional information in the future. You can take a look at the available endpoints and the associated responses in this [wiki article](https://github.com/xatkit-bot-platform/xatkit-releases/wiki/REST-API). Note that this API is far from perfect, and we already have a couple of opened issues to improve it (see [here](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/258) and [here](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/257)).
- `RuntimeActions` creating a dedicated `XatkitSession` (e.g. messaging actions getting a session based on the targeted channel) now update the execution rule's session to reflect the session shift. This allows, in the context of an event reaction bot, to set session variables in reaction to an *event*  that will be merged in the session corresponding to the messaging action triggered within the rule.
- `AdminHttpHandler` now uses a list of test client names to simulate multi-client bots using the web-based component. The name is printed in the rendered HTML page, and used to initialize the *xatkit-react* client.
- Support for bots hosted on `https` domains. The Xatkit configuration can now contain a `xatkit.server.ssl.keystore` property specifying the location of the keystore to use to sign http responses. Additional properties specifying keystore password are also required, check [Xatkit configuration options](https://github.com/xatkit-bot-platform/xatkit-releases/wiki/Xatkit-Options) for more information.

### Changed

- `JsonEventMatcher` now logs the builder content even if the intent definition is not known. This allows to inspect the logs and copy-paste new events easily in the platform editor (the builder content is populated from the received JSON payload and is printed using the *platform language* syntax).
- `XatkitCore` now loads `.execution` files instead of `.xmi`. **This change breaks the public API**: existing  `.properties` file need to be updated with the path to the `.execution` file.
- `ExecutionService` now inherits from `XbaseInterpreter`, offering complete support for Xbase expressions in the execution models. **This change breaks the public API**: the interpreter public methods have changed to reflect this integration.
- Renamed `DefaultIntentProvider` to `RegExIntentProvider` to reflect how intents are extracted. **This change breaks the public API**: classes depending on `DefaultIntentProvider` need to update their dependencies. The behavior of the provider is not changed by this update.
- The monitoring database structure has been changed. Data stored with previous versions of Xatkit won't be accessible with the new version, and existing databases need to be deleted/moved before starting bots to avoid data corruption.
- `XatkitSession.merge(other)` now performs a deep copy of the `other` session variables. This allows to update a merged session without altering the base one. 
- `RegExIntentRecognitionProvider` is now case insensitive for *training sentences*. This means that the training sentence `hi` now matches `HI`, `Hi`, etc. This change does not affect **mapping entities**, that are still case sensitive. To extend the behavior to mapping entities we need to implement support for synonyms (see [#261](https://github.com/xatkit-bot-platform/xatkit-runtime/issues/261))

### Removed

- Monitoring REST endpoints ` POST /analytics/unmatched ` and `POST /analytics/matched ` have been removed and replace by a new monitoring API. **This change breaks the public API**: client applications relying on these endpoints need to adapt to the new API.

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

