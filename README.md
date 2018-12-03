jarvis
======
[![Build Status](https://travis-ci.com/SOM-Research/jarvis.svg?token=FBbqzUpaXaqnawrfdPca&branch=master)](https://travis-ci.com/SOM-Research/jarvis)
[![codecov](https://codecov.io/gh/gdaniel/jarvis/branch/master/graph/badge.svg?token=02TcDpkeLm)](https://codecov.io/gh/gdaniel/jarvis)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8f852d0d41b24f4f9a989db243647ac2)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=gdaniel/jarvis&amp;utm_campaign=Badge_Grade)

Jarvis is a generic bot platform that embeds a dedicated **chatbot-specific modeling language** to specify user intentions and received events, and bind them to computable actions. The modeled chatbot definition is handled by the **Jarvis Runtime Engine**, which automatically manages its deployment and execution.

Spend more time on your conversation design, and let the framework manage the tedious work of deploying the chatbot application, connecting to the targeted platforms, and extract information from user inputs!

# Overview

The figure below shows the overview of the Jarvis framework. A chatbot designer specifies the chatbot under construction using the **Jarvis Modeling Language**, that defines three packages:

- **Intent Package** to describe the user intentions using training sentences, contextual information extraction, and matching conditions ([learn more]()).
- **Platform Package** to specify the possible actions available in the potential target platforms, including those existing only on specific environments, e.g. posting a message on a Slack channel, opening an issue on Github, etc ([learn more]()).
- **Execution Package** to bind user intentions to actions as part of the chatbot behavior definition, e.g. sending a welcome message to the user when he *intents* to initiate the conversation ([learn more]()).

![Jarvis Overview](https://raw.githubusercontent.com/wiki/SOM-Research/jarvis/img/overview.png)

These models are complemented with a *Deployment Configuration* file, that specifies the *intent recognition provider* platform to use (e.g. [DialogFlow](https://dialogflow.com/), [IBM Watson Assistant](https://www.ibm.com/watson/ai-assistant/), or [Amazon Lex](https://aws.amazon.com/lex/)), platform-specific configuration (e.g. Slack credentials), as well as custom execution properties.

These assets constitute the input of the **Jarvis Runtime** component that starts by deploying the created chatbot. This implies registering the user intents to the selected *intent recognition provider*, connecting to *instant messaging platforms*, and starting the *external services* specified in the execution model. Then, when a user input is received, the runtime forwards it to the *intent recognition provider*, gets back the recognized intent and perform the required actions based on the chatbot execution model.

There are several benefits in using this modular architecture to specify chatbots:

- The **Jarvis Modeling Language**  packages decouple the different dimensions of a chatbot definition, facilitating the reuse of each dimension across several chatbots. As an example, the Slack platform definition (that provides actions to send messages on specific Slack channels) can be reused in all chatbots interacting with Slack.
- Each sub-language is totally independent of the concrete *intent recognition provider*, and the [Generic_Chat](https://github.com/SOM-Research/jarvis/wiki/Generic_ChatPlatform) platform allows to define messaging actions that are independent of the targeted messaging platforms, easing the maintenance and evolution of the chatbot.
- The **Jarvis Runtime** architecture can be easily extended to support new platform connections and computable actions.  Learn more on custom platform and actions definition [here]().

You are now ready to start using Jarvis and [create your first chatbot](https://github.com/SOM-Research/jarvis/wiki/Getting-Started)! You can also have a look at the [installation instructions](https://github.com/SOM-Research/jarvis/wiki/Installation) to get setup the Jarvis modeling environment. 

# Custom Intent Recognition Providers

Jarvis relies on *intent recognition providers* to translate user inputs into *intents* that can be used by the runtime component to trigger actions. Note that the runtime itself embeds a [DialogFlow](https://github.com/SOM-Research/jarvis/wiki/DialogFlow) connector that can be used out of the box to match user intents, but the engine's architecture is generic and can be extended to support new alternatives such as IBM Watson Assistant or Amazon Lex.

# Packaged Platforms

Jarvis provides a set of pre-packaged *platforms* that can be used in your execution models to compute *actions* and/or receive user *intents* (you don't know what is a Jarvis platform? You can have a look at [this section](Platform) to have an overview of the framework!).

The table below shows the current platforms embedded with the framework, whether they define a *provider*, as well as their development status. Note that each platform has a specific issue label on our [issue tracker](https://github.com/SOM-Research/jarvis/issues), use them if you have a question related to a specific platform!

| Platform                                       | Provider           | Status            |
| ---------------------------------------------- | ------------------ | ----------------- |
| [Core](https://github.com/SOM-Research/jarvis/wiki/CorePlatform)                 | no                 | Supported         |
| [Discord](https://github.com/SOM-Research/jarvis/wiki/DiscordPlatform)           | yes (intents)      | Supported         |
| [Generic_Chat](https://github.com/SOM-Research/jarvis/wiki/Generic_ChatPlatform) | yes***** (intents) | Under development |
| [Github](https://github.com/SOM-Research/jarvis/wiki/GithubPlatform)             | yes (events)       | Under development |
| [Log](https://github.com/SOM-Research/jarvis/wiki/LogPlatform)                | no                 | Supported         |
| [Slack](https://github.com/SOM-Research/jarvis/wiki/SlackPlatform)               | yes (intents)      | Supported         |

[*] The *Generic_Chat* platform provides a special *EventProvider* that is set at runtime using the Jarvis configuration. More information on this platform available [here](https://github.com/SOM-Research/jarvis/wiki/Generic_ChatPlatform.md).
