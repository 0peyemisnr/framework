/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui;

import java.util.Set;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.ComponentContainerConnector;
import com.vaadin.terminal.gwt.client.ComponentState;
import com.vaadin.terminal.gwt.client.Connector;
import com.vaadin.terminal.gwt.client.ConnectorMap;
import com.vaadin.terminal.gwt.client.LayoutManager;
import com.vaadin.terminal.gwt.client.ServerConnector;
import com.vaadin.terminal.gwt.client.TooltipInfo;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.communication.StateChangeEvent;
import com.vaadin.terminal.gwt.client.ui.root.RootConnector;

public abstract class AbstractComponentConnector extends AbstractConnector
        implements ComponentConnector {

    private Widget widget;

    private String lastKnownWidth = "";
    private String lastKnownHeight = "";

    /**
     * Default constructor
     */
    public AbstractComponentConnector() {
    }

    /**
     * Creates and returns the widget for this VPaintableWidget. This method
     * should only be called once when initializing the paintable.
     * 
     * @return
     */
    protected Widget createWidget() {
        return ConnectorWidgetFactory.createWidget(getClass());
    }

    /**
     * Returns the widget associated with this paintable. The widget returned by
     * this method must not changed during the life time of the paintable.
     * 
     * @return The widget associated with this paintable
     */
    public Widget getWidget() {
        if (widget == null) {
            widget = createWidget();
        }

        return widget;
    }

    @Deprecated
    public static boolean isRealUpdate(UIDL uidl) {
        return !uidl.hasAttribute("cached");
    }

    @Override
    public ComponentState getState() {
        return (ComponentState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        ConnectorMap paintableMap = ConnectorMap.get(getConnection());

        if (getState().getDebugId() != null) {
            getWidget().getElement().setId(getState().getDebugId());
        } else {
            getWidget().getElement().removeAttribute("id");

        }

        /*
         * Disabled state may affect (override) tabindex so the order must be
         * first setting tabindex, then enabled state (through super
         * implementation).
         */
        if (getState() instanceof TabIndexState
                && getWidget() instanceof Focusable) {
            ((Focusable) getWidget()).setTabIndex(((TabIndexState) getState())
                    .getTabIndex());
        }

        super.onStateChanged(stateChangeEvent);

        // Style names
        String styleName = getStyleNames(getWidget().getStylePrimaryName());
        getWidget().setStyleName(styleName);

        // Update tooltip
        TooltipInfo tooltipInfo = paintableMap.getTooltipInfo(this, null);
        if (getState().hasDescription()) {
            tooltipInfo.setTitle(getState().getDescription());
        } else {
            tooltipInfo.setTitle(null);
        }
        // add error info to tooltip if present
        tooltipInfo.setErrorMessage(getState().getErrorMessage());

        // Set captions
        if (delegateCaptionHandling()) {
            ServerConnector parent = getParent();
            if (parent instanceof ComponentContainerConnector) {
                ((ComponentContainerConnector) parent).updateCaption(this);
            } else if (parent == null && !(this instanceof RootConnector)) {
                VConsole.error("Parent of connector "
                        + Util.getConnectorString(this)
                        + " is null. This is typically an indication of a broken component hierarchy");
            }
        }

        /*
         * updateComponentSize need to be after caption update so caption can be
         * taken into account
         */

        updateComponentSize();
    }

    public void setWidgetEnabled(boolean widgetEnabled) {
        if (getWidget() instanceof HasEnabled) {
            // set widget specific enabled state
            ((HasEnabled) getWidget()).setEnabled(widgetEnabled);
            // add or remove v-disabled style name from the widget
            getWidget().setStyleName(ApplicationConnection.DISABLED_CLASSNAME,
                    !widgetEnabled);
            // make sure the caption has or has not v-disabled style
            if (delegateCaptionHandling()) {
                ServerConnector parent = getParent();
                if (parent instanceof ComponentContainerConnector) {
                    ((ComponentContainerConnector) parent).updateCaption(this);
                } else if (parent == null && !(this instanceof RootConnector)) {
                    VConsole.error("Parent of connector "
                            + Util.getConnectorString(this)
                            + " is null. This is typically an indication of a broken component hierarchy");
                }
            }
        }
    }

    private void updateComponentSize() {
        String newWidth = getState().getWidth();
        String newHeight = getState().getHeight();

        // Parent should be updated if either dimension changed between relative
        // and non-relative
        if (newWidth.endsWith("%") != lastKnownWidth.endsWith("%")) {
            Connector parent = getParent();
            if (parent instanceof ManagedLayout) {
                getLayoutManager().setNeedsHorizontalLayout(
                        (ManagedLayout) parent);
            }
        }

        if (newHeight.endsWith("%") != lastKnownHeight.endsWith("%")) {
            Connector parent = getParent();
            if (parent instanceof ManagedLayout) {
                getLayoutManager().setNeedsVerticalLayout(
                        (ManagedLayout) parent);
            }
        }

        lastKnownWidth = newWidth;
        lastKnownHeight = newHeight;

        // Set defined sizes
        Widget widget = getWidget();

        widget.setStyleName("v-has-width", !isUndefinedWidth());
        widget.setStyleName("v-has-height", !isUndefinedHeight());

        widget.setHeight(newHeight);
        widget.setWidth(newWidth);
    }

    public boolean isRelativeHeight() {
        return getState().getHeight().endsWith("%");
    }

    public boolean isRelativeWidth() {
        return getState().getWidth().endsWith("%");
    }

    public boolean isUndefinedHeight() {
        return getState().getHeight().length() == 0;
    }

    public boolean isUndefinedWidth() {
        return getState().getWidth().length() == 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.terminal.gwt.client.ComponentConnector#delegateCaptionHandling
     * ()
     */
    public boolean delegateCaptionHandling() {
        return true;
    }

    /**
     * Generates the style name for the widget based on the given primary style
     * name and the shared state.
     * <p>
     * This method can be overridden to provide additional style names for the
     * component
     * </p>
     * 
     * @param primaryStyleName
     *            The primary style name to use when generating the final style
     *            names
     * @return The style names, settable using
     *         {@link Widget#setStyleName(String)}
     */
    protected String getStyleNames(String primaryStyleName) {
        ComponentState state = getState();

        StringBuilder styleBuf = new StringBuilder();
        styleBuf.append(primaryStyleName);
        styleBuf.append(" v-connector");

        // Uses connector methods to enable connectors to take hierarchy or
        // multiple state variables into account
        if (!isEnabled()) {
            styleBuf.append(" ");
            styleBuf.append(ApplicationConnection.DISABLED_CLASSNAME);
        }
        if (isReadOnly()) {
            styleBuf.append(" ");
            styleBuf.append("v-readonly");
        }

        // add additional styles as css classes, prefixed with component default
        // stylename
        if (state.hasStyles()) {
            for (String style : state.getStyles()) {
                styleBuf.append(" ");
                styleBuf.append(primaryStyleName);
                styleBuf.append("-");
                styleBuf.append(style);
                styleBuf.append(" ");
                styleBuf.append(style);
            }
        }

        // add error classname to components w/ error
        if (null != state.getErrorMessage()) {
            styleBuf.append(" ");
            styleBuf.append(primaryStyleName);
            styleBuf.append(ApplicationConnection.ERROR_CLASSNAME_EXT);
        }

        return styleBuf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.terminal.gwt.client.ComponentConnector#isReadOnly()
     */
    @Deprecated
    public boolean isReadOnly() {
        return getState().isReadOnly();
    }

    public LayoutManager getLayoutManager() {
        return LayoutManager.get(getConnection());
    }

    /**
     * Checks if there is a registered server side listener for the given event
     * identifier.
     * 
     * @param eventIdentifier
     *            The identifier to check for
     * @return true if an event listener has been registered with the given
     *         event identifier on the server side, false otherwise
     */
    public boolean hasEventListener(String eventIdentifier) {
        Set<String> reg = getState().getRegisteredEventListeners();
        return (reg != null && reg.contains(eventIdentifier));
    }

    @Override
    public void updateEnabledState(boolean enabledState) {
        super.updateEnabledState(enabledState);

        setWidgetEnabled(isEnabled());
    }

    @Override
    public void onUnregister() {
        super.onUnregister();

        // Show an error if widget is still attached to DOM. It should never be
        // at this point.
        if (getWidget() != null && getWidget().isAttached()) {
            getWidget().removeFromParent();
            VConsole.error("Widget is still attached to the DOM after the connector ("
                    + Util.getConnectorString(this)
                    + ") has been unregistered. Widget was removed.");
        }
    }
}
