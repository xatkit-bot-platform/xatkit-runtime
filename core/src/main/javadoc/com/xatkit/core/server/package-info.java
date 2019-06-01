/**
 * Contains the Xatkit server implementation.
 * <p>
 * The Xatkit server is an experimental feature (i.e. not officially supported for now). It exposes a REST API that
 * can be used to send webhook events to the framework from remote services. These events are then handled by
 * specific event providers that transform them into Xatkit-compatible events.
 */
package com.xatkit.core.server;