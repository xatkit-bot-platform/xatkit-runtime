# Getting Started Tutorial

[Access the related tutorial page](https://github.com/xatkit-bot-platform/xatkit/wiki/Getting-Started)

## Contents

A simple greeting chatbot deployed on Slack and relying on DialogFlow to extract user intents.

- `TutorialIntentLibrary.intent` defines a single intent *HowAreYou* with a few training sentences
- `TutorialExecution.execution` binds the *HowAreYou* intent to a *Reply* action from the built-in *SlackPlatform*
- `xatkit-tutorial.properties` contains the credentials and access tokens required to deploy the chatbot

## Deployment

For security reasons the provided `xatkit-tutorial.properties` does not contain valid credentials and access tokens, you can check [our wiki](https://github.com/xatkit-bot-platform/xatkit/wiki/Deploying-chatbots) to learn how to deploy a chatbot and fill its property file with your own access tokens.

This bot requires the following property keys:

- `xatkit.dialogflow.projectId` the identifier of the DialogFlow agent used to match user intents
- `xatkit.dialogflow.credentials.path` the path to the JSON file containing the DialogFlow service account key associated to the bot
- `xatkit.slack.token` the Slack authentication token associated to the bot

