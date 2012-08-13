/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.ui.optiongroup;

import java.util.ArrayList;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.shared.EventId;
import com.vaadin.shared.ui.Connect;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.ui.OptionGroup;

@Connect(OptionGroup.class)
public class OptionGroupConnector extends OptionGroupBaseConnector {

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        getWidget().htmlContentAllowed = uidl
                .hasAttribute(VOptionGroup.HTML_CONTENT_ALLOWED);

        super.updateFromUIDL(uidl, client);

        getWidget().sendFocusEvents = client.hasEventListeners(this,
                EventId.FOCUS);
        getWidget().sendBlurEvents = client.hasEventListeners(this,
                EventId.BLUR);

        if (getWidget().focusHandlers != null) {
            for (HandlerRegistration reg : getWidget().focusHandlers) {
                reg.removeHandler();
            }
            getWidget().focusHandlers.clear();
            getWidget().focusHandlers = null;

            for (HandlerRegistration reg : getWidget().blurHandlers) {
                reg.removeHandler();
            }
            getWidget().blurHandlers.clear();
            getWidget().blurHandlers = null;
        }

        if (getWidget().sendFocusEvents || getWidget().sendBlurEvents) {
            getWidget().focusHandlers = new ArrayList<HandlerRegistration>();
            getWidget().blurHandlers = new ArrayList<HandlerRegistration>();

            // add focus and blur handlers to checkboxes / radio buttons
            for (Widget wid : getWidget().panel) {
                if (wid instanceof CheckBox) {
                    getWidget().focusHandlers.add(((CheckBox) wid)
                            .addFocusHandler(getWidget()));
                    getWidget().blurHandlers.add(((CheckBox) wid)
                            .addBlurHandler(getWidget()));
                }
            }
        }
    }

    @Override
    public VOptionGroup getWidget() {
        return (VOptionGroup) super.getWidget();
    }
}
