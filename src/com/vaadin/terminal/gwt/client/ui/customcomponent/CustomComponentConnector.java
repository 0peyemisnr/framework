/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui.customcomponent;

import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.ConnectorHierarchyChangeEvent;
import com.vaadin.terminal.gwt.client.ui.AbstractComponentContainerConnector;
import com.vaadin.terminal.gwt.client.ui.Connect;
import com.vaadin.terminal.gwt.client.ui.Connect.LoadStyle;
import com.vaadin.ui.CustomComponent;

@Connect(value = CustomComponent.class, loadStyle = LoadStyle.EAGER)
public class CustomComponentConnector extends
        AbstractComponentContainerConnector {

    @Override
    public VCustomComponent getWidget() {
        return (VCustomComponent) super.getWidget();
    }

    @Override
    public void updateCaption(ComponentConnector component) {
        // NOP, custom component dont render composition roots caption
    }

    @Override
    public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent event) {
        super.onConnectorHierarchyChange(event);

        ComponentConnector newChild = null;
        if (getChildComponents().size() == 1) {
            newChild = getChildComponents().get(0);
        }

        VCustomComponent customComponent = getWidget();
        customComponent.setWidget(newChild.getWidget());

    }
}
