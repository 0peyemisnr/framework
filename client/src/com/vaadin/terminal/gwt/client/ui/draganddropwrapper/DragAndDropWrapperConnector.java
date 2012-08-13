/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui.draganddropwrapper;

import java.util.HashMap;
import java.util.Set;

import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.draganddropwrapper.DragAndDropWrapperConstants;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.ui.customcomponent.CustomComponentConnector;
import com.vaadin.ui.DragAndDropWrapper;

@Connect(DragAndDropWrapper.class)
public class DragAndDropWrapperConnector extends CustomComponentConnector
        implements Paintable {

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        getWidget().client = client;
        if (isRealUpdate(uidl) && !uidl.hasAttribute("hidden")) {
            UIDL acceptCrit = uidl.getChildByTagName("-ac");
            if (acceptCrit == null) {
                getWidget().dropHandler = null;
            } else {
                if (getWidget().dropHandler == null) {
                    getWidget().dropHandler = getWidget().new CustomDropHandler();
                }
                getWidget().dropHandler.updateAcceptRules(acceptCrit);
            }

            Set<String> variableNames = uidl.getVariableNames();
            for (String fileId : variableNames) {
                if (fileId.startsWith("rec-")) {
                    String receiverUrl = uidl.getStringVariable(fileId);
                    fileId = fileId.substring(4);
                    if (getWidget().fileIdToReceiver == null) {
                        getWidget().fileIdToReceiver = new HashMap<String, String>();
                    }
                    if ("".equals(receiverUrl)) {
                        Integer id = Integer.parseInt(fileId);
                        int indexOf = getWidget().fileIds.indexOf(id);
                        if (indexOf != -1) {
                            getWidget().files.remove(indexOf);
                            getWidget().fileIds.remove(indexOf);
                        }
                    } else {
                        getWidget().fileIdToReceiver.put(fileId, receiverUrl);
                    }
                }
            }
            getWidget().startNextUpload();

            getWidget().dragStartMode = uidl
                    .getIntAttribute(DragAndDropWrapperConstants.DRAG_START_MODE);
            getWidget().initDragStartMode();
            getWidget().html5DataFlavors = uidl
                    .getMapAttribute(DragAndDropWrapperConstants.HTML5_DATA_FLAVORS);

            // Used to prevent wrapper from stealing tooltips when not defined
            getWidget().hasTooltip = getState().hasDescription();
        }
    }

    @Override
    public VDragAndDropWrapper getWidget() {
        return (VDragAndDropWrapper) super.getWidget();
    }

}
