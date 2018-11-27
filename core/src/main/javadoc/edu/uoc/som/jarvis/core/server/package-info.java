/**
 * Contains the Jarvis server implementation.
 * <p>
 * The Jarvis server is an experimental feature (i.e. not officially supported for now). It exposes a REST API that
 * can be used to send webhook events to the framework from remote services. These events are then handled by
 * specific event providers that transform them into Jarvis-compatible events.
 */
package edu.uoc.som.jarvis.core.server;