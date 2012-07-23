/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.ui.button;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.vaadin.terminal.gwt.client.EventHelper;
import com.vaadin.terminal.gwt.client.MouseEventDetails;
import com.vaadin.terminal.gwt.client.MouseEventDetailsBuilder;
import com.vaadin.terminal.gwt.client.communication.FieldRpc.FocusAndBlurServerRpc;
import com.vaadin.terminal.gwt.client.communication.RpcProxy;
import com.vaadin.terminal.gwt.client.communication.StateChangeEvent;
import com.vaadin.terminal.gwt.client.ui.AbstractComponentConnector;
import com.vaadin.terminal.gwt.client.ui.Connect;
import com.vaadin.terminal.gwt.client.ui.Connect.LoadStyle;
import com.vaadin.terminal.gwt.client.ui.Icon;
import com.vaadin.ui.Button;

@Connect(value = Button.class, loadStyle = LoadStyle.EAGER)
public class ButtonConnector extends AbstractComponentConnector implements
        BlurHandler, FocusHandler, ClickHandler {

    private ButtonServerRpc rpc = RpcProxy.create(ButtonServerRpc.class, this);
    private FocusAndBlurServerRpc focusBlurProxy = RpcProxy.create(
            FocusAndBlurServerRpc.class, this);

    private HandlerRegistration focusHandlerRegistration = null;
    private HandlerRegistration blurHandlerRegistration = null;

    @Override
    public boolean delegateCaptionHandling() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        getWidget().addClickHandler(this);
        getWidget().client = getConnection();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        focusHandlerRegistration = EventHelper.updateFocusHandler(this,
                focusHandlerRegistration);
        blurHandlerRegistration = EventHelper.updateBlurHandler(this,
                blurHandlerRegistration);
        // Set text
        if (getState().isHtmlContentAllowed()) {
            getWidget().setHtml(getState().getCaption());
        } else {
            getWidget().setText(getState().getCaption());
        }

        // handle error
        if (null != getState().getErrorMessage()) {
            if (getWidget().errorIndicatorElement == null) {
                getWidget().errorIndicatorElement = DOM.createSpan();
                getWidget().errorIndicatorElement
                        .setClassName("v-errorindicator");
            }
            getWidget().wrapper.insertBefore(getWidget().errorIndicatorElement,
                    getWidget().captionElement);

        } else if (getWidget().errorIndicatorElement != null) {
            getWidget().wrapper.removeChild(getWidget().errorIndicatorElement);
            getWidget().errorIndicatorElement = null;
        }

        if (getState().getIcon() != null) {
            if (getWidget().icon == null) {
                getWidget().icon = new Icon(getConnection());
                getWidget().wrapper.insertBefore(getWidget().icon.getElement(),
                        getWidget().captionElement);
            }
            getWidget().icon.setUri(getState().getIcon().getURL());
        } else {
            if (getWidget().icon != null) {
                getWidget().wrapper.removeChild(getWidget().icon.getElement());
                getWidget().icon = null;
            }
        }

        getWidget().clickShortcut = getState().getClickShortcutKeyCode();
    }

    @Override
    public VButton getWidget() {
        return (VButton) super.getWidget();
    }

    @Override
    public ButtonState getState() {
        return (ButtonState) super.getState();
    }

    @Override
    public void onFocus(FocusEvent event) {
        // EventHelper.updateFocusHandler ensures that this is called only when
        // there is a listener on server side
        focusBlurProxy.focus();
    }

    @Override
    public void onBlur(BlurEvent event) {
        // EventHelper.updateFocusHandler ensures that this is called only when
        // there is a listener on server side
        focusBlurProxy.blur();
    }

    @Override
    public void onClick(ClickEvent event) {
        if (getState().isDisableOnClick()) {
            getWidget().setEnabled(false);
            rpc.disableOnClick();
        }

        // Add mouse details
        MouseEventDetails details = MouseEventDetailsBuilder
                .buildMouseEventDetails(event.getNativeEvent(), getWidget()
                        .getElement());
        rpc.click(details);

    }
}
