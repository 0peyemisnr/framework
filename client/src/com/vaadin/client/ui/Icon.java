/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.UIObject;
import com.vaadin.client.ApplicationConnection;

public class Icon extends UIObject {
    public static final String CLASSNAME = "v-icon";
    private final ApplicationConnection client;
    private String myUri;

    public Icon(ApplicationConnection client) {
        setElement(DOM.createImg());
        DOM.setElementProperty(getElement(), "alt", "");
        setStyleName(CLASSNAME);
        this.client = client;
    }

    public Icon(ApplicationConnection client, String uidlUri) {
        this(client, uidlUri, "");
    }

    public Icon(ApplicationConnection client, String uidlUri, String iconAltText) {
        this(client);
        setUri(uidlUri, iconAltText);
    }

    public void setUri(String uidlUri) {
        setUri(uidlUri, "");
    }

    public void setUri(String uidlUri, String uidlAlt) {
        if (!uidlUri.equals(myUri)) {
            /*
             * Start sinking onload events, widgets responsibility to react. We
             * must do this BEFORE we set src as IE fires the event immediately
             * if the image is found in cache (#2592).
             */
            sinkEvents(Event.ONLOAD);

            String uri = client.translateVaadinUri(uidlUri);
            DOM.setElementProperty(getElement(), "src", uri);
            myUri = uidlUri;
        }

        if (uidlAlt != null) {
            setAlternateText(uidlAlt);
        } else {
            setAlternateText("");
        }
    }

    /**
     * Sets the alternate text for the icon.
     * 
     * @param uidlAlt
     *            with the alternate text. Must be non null
     */
    public void setAlternateText(String uidlAlt) {
        assert uidlAlt != null : "Alternate text must be non null";
        DOM.setElementProperty(getElement(), "alt", uidlAlt);
    }
}
