Xatkit - The easiest way to build complex digital assistants
======
[![License Badge](https://img.shields.io/badge/license-EPL%202.0-brightgreen.svg)](https://opensource.org/licenses/EPL-2.0)
[![Build Status](https://travis-ci.com/jarvis-bot-platform/jarvis.svg?branch=master)](https://travis-ci.com/jarvis-bot-platform/jarvis)
[![codecov](https://codecov.io/gh/jarvis-bot-platform/jarvis/branch/master/graph/badge.svg)](https://codecov.io/gh/jarvis-bot-platform/jarvis)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4cdbb07fc78f4b0f9c3a5b5c254a4c2b)](https://www.codacy.com/app/gdaniel/jarvis?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jarvis-bot-platform/jarvis&amp;utm_campaign=Badge_Grade)
[![BCH compliance](https://bettercodehub.com/edge/badge/jarvis-bot-platform/jarvis?branch=master)](https://bettercodehub.com/)
[![Javadoc Badge](https://img.shields.io/badge/javadoc-v1.0.0-brightgreen.svg)](https://jarvis-bot-platform.github.io/jarvis-docs/releases/v1.0.0/doc/)
[![Latest Javadoc Badge](https://img.shields.io/badge/javadoc-latest-brightgreen.svg)](https://jarvis-bot-platform.github.io/jarvis-docs/releases/snapshot/doc/)
[![Gitter Badge](https://img.shields.io/badge/chat-on%20gitter-404040.svg)](https://gitter.im/jarvis-development/Lobby)

Xatkit is a generic bot platform that embeds a dedicated **chatbot-specific modeling language** to specify user intentions and received events, and bind them to computable actions. The modeled chatbot definition is handled by the **Xatkit Runtime Engine**, which automatically manages its deployment and execution.

Spend more time on your conversation design, and let the framework manage the tedious work of deploying the chatbot / digital assistant application, connecting to the targeted platforms, and extract information from user inputs!

# Overview

The figure below shows the overview of the Xatkit framework. A (chat)bot designer specifies the chatbot under construction using the **Xatkit Modeling Language**, that defines three packages:

- **Intent Package** to describe the user intentions using training sentences, contextual information extraction, and matching conditions.
- **Platform Package** to specify the possible actions available in the potential target platforms, including those existing only on specific environments, e.g. posting a message on a Slack channel, opening an issue on Github, etc.
- **Execution Package** to bind user intentions to actions as part of the chatbot behavior definition, e.g. sending a welcome message to the user when he *intents* to initiate the conversation.

![Xatkit Overview](https://raw.githubusercontent.com/wiki/SOM-Research/jarvis/img/xatkit-overview.png)

These models are complemented with a *Deployment Configuration* file, that specifies the *intent recognition provider* platform to use (e.g. [DialogFlow](https://dialogflow.com/), [IBM Watson Assistant](https://www.ibm.com/watson/ai-assistant/), or [Amazon Lex](https://aws.amazon.com/lex/)), platform-specific configuration (e.g. Slack credentials), as well as custom execution properties.

These assets constitute the input of the **Xatkit Runtime** component that starts by deploying the created chatbot. This implies registering the user intents to the selected *intent recognition provider*, connecting to *instant messaging platforms*, and starting the *external services* specified in the execution model. Then, when a user input is received, the runtime forwards it to the *intent recognition provider*, gets back the recognized intent and perform the required actions based on the chatbot execution model.

There are several benefits in using this modular architecture to specify chatbots:

- The **Xatkit Modeling Language**  packages decouple the different dimensions of a chatbot definition, facilitating the reuse of each dimension across several chatbots. As an example, the Slack platform definition (that provides actions to send messages on specific Slack channels) can be reused in all chatbots interacting with Slack.
- Each sub-language is totally independent of the concrete *intent recognition provider*, and Xatkit allows to define messaging actions that are independent of the targeted messaging platforms (more information [here](Generic_ChatPlatform), easing the maintenance and evolution of the chatbot.
- The **Xatkit Runtime** architecture can be easily extended to support new platform connections and computable actions.  Learn more on custom platform and actions definition [here]().

You are now ready to start using Xatkit and [create your first chatbot](https://github.com/jarvis-bot-platform/jarvis/wiki/Getting-Started)! You can also have a look at the [installation instructions](https://github.com/jarvis-bot-platform/jarvis/wiki/Installation) to get setup the Xatkit modeling environment. 

# Intent Recognition Providers

Xatkit relies on *intent recognition providers* to translate user inputs into *intents* that can be used by the runtime component to trigger actions. Note that the runtime itself embeds a [DialogFlow](https://github.com/SOM-Research/jarvis/wiki/DialogFlow) connector that can be used out of the box to match user intents, but the engine's architecture is generic and can be extended to support new alternatives such as IBM Watson Assistant or Amazon Lex.

# Packaged Platforms

Xatkit provides a set of pre-packaged *platforms* that can be used in your execution models to compute *actions* and/or receive user *intents*. Some platforms are currently provided as part of the Xatkit core but you can find additional ones in the Xatkit organization. Examples of available platforms are GitHub, Slack, Discord, Logs, Generic Chats, React,....
