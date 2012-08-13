/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.communication;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.ConnectorMap;
import com.vaadin.terminal.gwt.server.JsonCodec;

/**
 * Implementors of this interface knows how to serialize an Object of a given
 * type to JSON and how to deserialize the JSON back into an object.
 * 
 * The {@link #serialize(Object, ConnectorMap)} and
 * {@link #deserialize(JSONObject, ConnectorMap)} methods must be symmetric so
 * they can be chained and produce the original result (or an equal result).
 * 
 * Each {@link JSONSerializer} implementation can handle an object of a single
 * type - see {@link SerializerMap}.
 * 
 * @since 7.0
 */
public interface JSONSerializer<T> {

    /**
     * Creates and deserializes an object received from the server. Must be
     * compatible with {@link #serialize(Object, ConnectorMap)} and also with
     * the server side
     * {@link JsonCodec#encode(Object, com.vaadin.terminal.gwt.server.PaintableIdMapper)}
     * .
     * 
     * @param jsonValue
     *            JSON map from property name to property value
     * @return A deserialized object
     */
    T deserialize(Type type, JSONValue jsonValue,
            ApplicationConnection connection);

    /**
     * Serialize the given object into JSON. Must be compatible with
     * {@link #deserialize(JSONObject, ConnectorMap)} and also with the server
     * side
     * {@link JsonCodec#decode(com.vaadin.external.json.JSONArray, com.vaadin.terminal.gwt.server.PaintableIdMapper)}
     * 
     * @param value
     *            The object to serialize
     * @return A JSON serialized version of the object
     */
    JSONValue serialize(T value, ApplicationConnection connection);

}
