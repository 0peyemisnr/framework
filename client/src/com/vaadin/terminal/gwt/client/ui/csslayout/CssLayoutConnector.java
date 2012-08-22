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
package com.vaadin.terminal.gwt.client.ui.csslayout;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.LayoutClickRpc;
import com.vaadin.shared.ui.csslayout.CssLayoutServerRpc;
import com.vaadin.shared.ui.csslayout.CssLayoutState;
import com.vaadin.terminal.gwt.client.BrowserInfo;
import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.ConnectorHierarchyChangeEvent;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.VCaption;
import com.vaadin.terminal.gwt.client.communication.RpcProxy;
import com.vaadin.terminal.gwt.client.communication.StateChangeEvent;
import com.vaadin.terminal.gwt.client.ui.AbstractLayoutConnector;
import com.vaadin.terminal.gwt.client.ui.LayoutClickEventHandler;
import com.vaadin.terminal.gwt.client.ui.csslayout.VCssLayout.FlowPane;
import com.vaadin.ui.CssLayout;

@Connect(CssLayout.class)
public class CssLayoutConnector extends AbstractLayoutConnector {

    private LayoutClickEventHandler clickEventHandler = new LayoutClickEventHandler(
            this) {

        @Override
        protected ComponentConnector getChildComponent(Element element) {
            return Util.getConnectorForElement(getConnection(), getWidget(),
                    element);
        }

        @Override
        protected LayoutClickRpc getLayoutClickRPC() {
            return rpc;
        };
    };

    private CssLayoutServerRpc rpc;

    private Map<ComponentConnector, VCaption> childToCaption = new HashMap<ComponentConnector, VCaption>();

    @Override
    protected void init() {
        super.init();
        rpc = RpcProxy.create(CssLayoutServerRpc.class, this);
    }

    @Override
    public CssLayoutState getState() {
        return (CssLayoutState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);

        for (ComponentConnector child : getChildComponents()) {
            if (!getState().getChildCss().containsKey(child)) {
                continue;
            }
            String css = getState().getChildCss().get(child);
            Style style = child.getWidget().getElement().getStyle();
            // should we remove styles also? How can we know what we have added
            // as it is added directly to the child component?
            String[] cssRules = css.split(";");
            for (String cssRule : cssRules) {
                String parts[] = cssRule.split(":");
                if (parts.length == 2) {
                    style.setProperty(makeCamelCase(parts[0].trim()),
                            parts[1].trim());
                }
            }
        }

    }

    @Override
    public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent event) {
        super.onConnectorHierarchyChange(event);

        clickEventHandler.handleEventHandlerRegistration();

        int index = 0;
        FlowPane cssLayoutWidgetContainer = getWidget().panel;
        for (ComponentConnector child : getChildComponents()) {
            VCaption childCaption = childToCaption.get(child);
            if (childCaption != null) {
                cssLayoutWidgetContainer.addOrMove(childCaption, index++);
            }
            cssLayoutWidgetContainer.addOrMove(child.getWidget(), index++);
        }

        // Detach old child widgets and possibly their caption
        for (ComponentConnector child : event.getOldChildren()) {
            if (child.getParent() == this) {
                // Skip current children
                continue;
            }
            cssLayoutWidgetContainer.remove(child.getWidget());
            VCaption vCaption = childToCaption.remove(child);
            if (vCaption != null) {
                cssLayoutWidgetContainer.remove(vCaption);
            }
        }
    }

    private static final String makeCamelCase(String cssProperty) {
        // TODO this might be cleaner to implement with regexp
        while (cssProperty.contains("-")) {
            int indexOf = cssProperty.indexOf("-");
            cssProperty = cssProperty.substring(0, indexOf)
                    + String.valueOf(cssProperty.charAt(indexOf + 1))
                            .toUpperCase() + cssProperty.substring(indexOf + 2);
        }
        if ("float".equals(cssProperty)) {
            if (BrowserInfo.get().isIE()) {
                return "styleFloat";
            } else {
                return "cssFloat";
            }
        }
        return cssProperty;
    }

    @Override
    public VCssLayout getWidget() {
        return (VCssLayout) super.getWidget();
    }

    @Override
    public void updateCaption(ComponentConnector child) {
        Widget childWidget = child.getWidget();
        FlowPane cssLayoutWidgetContainer = getWidget().panel;
        int widgetPosition = cssLayoutWidgetContainer
                .getWidgetIndex(childWidget);

        VCaption caption = childToCaption.get(child);
        if (VCaption.isNeeded(child.getState())) {
            if (caption == null) {
                caption = new VCaption(child, getConnection());
                childToCaption.put(child, caption);
            }
            if (!caption.isAttached()) {
                // Insert caption at widget index == before widget
                cssLayoutWidgetContainer.insert(caption, widgetPosition);
            }
            caption.updateCaption();
        } else if (caption != null) {
            childToCaption.remove(child);
            cssLayoutWidgetContainer.remove(caption);
        }
    }

}
