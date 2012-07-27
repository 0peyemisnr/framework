/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui.splitpanel;

import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.Connect.LoadStyle;
import com.vaadin.ui.HorizontalSplitPanel;

@Connect(value = HorizontalSplitPanel.class, loadStyle = LoadStyle.EAGER)
public class HorizontalSplitPanelConnector extends AbstractSplitPanelConnector {

    @Override
    public VSplitPanelHorizontal getWidget() {
        return (VSplitPanelHorizontal) super.getWidget();
    }

}
