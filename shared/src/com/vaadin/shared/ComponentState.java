/* 
 * Copyright 2011 Vaadin Ltd.
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

package com.vaadin.shared;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.shared.communication.SharedState;

/**
 * Default shared state implementation for UI components.
 * 
 * State classes of concrete components should extend this class.
 * 
 * @since 7.0
 */
public class ComponentState extends SharedState {
    private String height = "";
    private String width = "";
    private boolean readOnly = false;
    private boolean immediate = false;
    private String description = "";
    // Note: for the caption, there is a difference between null and an empty
    // string!
    private String caption = null;
    private boolean visible = true;
    private List<String> styles = null;
    private String id = null;
    /**
     * A set of event identifiers with registered listeners.
     */
    private Set<String> registeredEventListeners = null;

    // HTML formatted error message for the component
    // TODO this could be an object with more information, but currently the UI
    // only uses the message
    private String errorMessage = null;

    /**
     * Returns the component height as set by the server.
     * 
     * Can be relative (containing the percent sign) or absolute, or empty
     * string for undefined height.
     * 
     * @return component height as defined by the server, not null
     */
    public String getHeight() {
        if (height == null) {
            return "";
        }
        return height;
    }

    /**
     * Sets the height of the component in the server format.
     * 
     * Can be relative (containing the percent sign) or absolute, or null or
     * empty string for undefined height.
     * 
     * @param height
     *            component height
     */
    public void setHeight(String height) {
        this.height = height;
    }

    /**
     * Returns true if the component height is undefined, false if defined
     * (absolute or relative).
     * 
     * @return true if component height is undefined
     */
    public boolean isUndefinedHeight() {
        return "".equals(getHeight());
    }

    /**
     * Returns true if the component height is relative to the parent, i.e.
     * percentage, false if it is fixed/auto.
     * 
     * @return true if component height is relative (percentage)
     */
    public boolean isRelativeHeight() {
        return getHeight().endsWith("%");
    }

    /**
     * Returns the component width as set by the server.
     * 
     * Can be relative (containing the percent sign) or absolute, or empty
     * string for undefined height.
     * 
     * @return component width as defined by the server, not null
     */
    public String getWidth() {
        if (width == null) {
            return "";
        }
        return width;
    }

    /**
     * Sets the width of the component in the server format.
     * 
     * Can be relative (containing the percent sign) or absolute, or null or
     * empty string for undefined width.
     * 
     * @param width
     *            component width
     */
    public void setWidth(String width) {
        this.width = width;
    }

    /**
     * Returns true if the component width is undefined, false if defined
     * (absolute or relative).
     * 
     * @return true if component width is undefined
     */
    public boolean isUndefinedWidth() {
        return "".equals(getWidth());
    }

    /**
     * Returns true if the component width is relative to the parent, i.e.
     * percentage, false if it is fixed/auto.
     * 
     * @return true if component width is relative (percentage)
     */
    public boolean isRelativeWidth() {
        return getWidth().endsWith("%");
    }

    /**
     * Returns true if the component is in read-only mode.
     * 
     * @see com.vaadin.ui.Component#isReadOnly()
     * 
     * @return true if the component is in read-only mode
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets or resets the read-only mode for a component.
     * 
     * @see com.vaadin.ui.Component#setReadOnly()
     * 
     * @param readOnly
     *            new mode for the component
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Returns true if the component is in immediate mode.
     * 
     * @see com.vaadin.server.VariableOwner#isImmediate()
     * 
     * @return true if the component is in immediate mode
     */
    public boolean isImmediate() {
        return immediate;
    }

    /**
     * Sets or resets the immediate mode for a component.
     * 
     * @see com.vaadin.server.VariableOwner#setImmediate()
     * 
     * @param immediate
     *            new mode for the component
     */
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    /**
     * Returns true if the component has user-defined styles.
     * 
     * @return true if the component has user-defined styles
     */
    public boolean hasStyles() {
        return styles != null && !styles.isEmpty();
    }

    /**
     * Gets the description of the component (typically shown as tooltip).
     * 
     * @see com.vaadin.ui.AbstractComponent#getDescription()
     * 
     * @return component description (not null, can be empty string)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the component (typically shown as tooltip).
     * 
     * @see com.vaadin.ui.AbstractComponent#setDescription(String)
     * 
     * @param description
     *            new component description (can be null)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns true if the component has a description.
     * 
     * @return true if the component has a description
     */
    public boolean hasDescription() {
        return getDescription() != null && !"".equals(getDescription());
    }

    /**
     * Gets the caption of the component (typically shown by the containing
     * layout).
     * 
     * @see com.vaadin.ui.Component#getCaption()
     * 
     * @return component caption - can be null (no caption) or empty string
     *         (reserve space for an empty caption)
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the caption of the component (typically shown by the containing
     * layout).
     * 
     * @see com.vaadin.ui.Component#setCaption(String)
     * 
     * @param caption
     *            new component caption - can be null (no caption) or empty
     *            string (reserve space for an empty caption)
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Returns the visibility state of the component. Note that this state is
     * related to the component only, not its parent. This might differ from
     * what {@link com.vaadin.ui.Component#isVisible()} returns as this takes
     * the hierarchy into account.
     * 
     * @return The visibility state.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the visibility state of the component.
     * 
     * @param visible
     *            The new visibility state.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Gets the style names for the component.
     * 
     * @return A List of style names or null if no styles have been set.
     */
    public List<String> getStyles() {
        return styles;
    }

    /**
     * Sets the style names for the component.
     * 
     * @param styles
     *            A list containing style names
     */
    public void setStyles(List<String> styles) {
        this.styles = styles;
    }

    /**
     * Gets the id for the component. The id is added as DOM id for the
     * component.
     * 
     * @return The id for the component or null if not set
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id for the component. The id is added as DOM id for the
     * component.
     * 
     * @param id
     *            The new id for the component or null for no id
     * 
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the identifiers for the event listeners that have been registered
     * for the component (using an event id)
     * 
     * @return A set of event identifiers or null if no identifiers have been
     *         registered
     */
    public Set<String> getRegisteredEventListeners() {
        return registeredEventListeners;
    }

    /**
     * Sets the identifiers for the event listeners that have been registered
     * for the component (using an event id)
     * 
     * @param registeredEventListeners
     *            The new set of identifiers or null if no identifiers have been
     *            registered
     */
    public void setRegisteredEventListeners(Set<String> registeredEventListeners) {
        this.registeredEventListeners = registeredEventListeners;
    }

    /**
     * Adds an event listener id.
     * 
     * @param eventListenerId
     *            The event identifier to add
     */
    public void addRegisteredEventListener(String eventListenerId) {
        if (registeredEventListeners == null) {
            registeredEventListeners = new HashSet<String>();
        }
        registeredEventListeners.add(eventListenerId);

    }

    /**
     * Removes an event listener id.
     * 
     * @param eventListenerId
     *            The event identifier to remove
     */
    public void removeRegisteredEventListener(String eventIdentifier) {
        if (registeredEventListeners == null) {
            return;
        }
        registeredEventListeners.remove(eventIdentifier);
        if (registeredEventListeners.size() == 0) {
            registeredEventListeners = null;
        }
    }

    /**
     * Returns the current error message for the component.
     * 
     * @return HTML formatted error message to show for the component or null if
     *         none
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the current error message for the component.
     * 
     * TODO this could use an object with more details about the error
     * 
     * @param errorMessage
     *            HTML formatted error message to show for the component or null
     *            for none
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
