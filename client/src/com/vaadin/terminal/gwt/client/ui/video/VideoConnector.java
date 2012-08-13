/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.client.ui.video;

import com.vaadin.shared.communication.URLReference;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.video.VideoState;
import com.vaadin.terminal.gwt.client.communication.StateChangeEvent;
import com.vaadin.terminal.gwt.client.ui.MediaBaseConnector;
import com.vaadin.ui.Video;

@Connect(Video.class)
public class VideoConnector extends MediaBaseConnector {

    @Override
    public VideoState getState() {
        return (VideoState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        URLReference poster = getState().getPoster();
        if (poster != null) {
            getWidget().setPoster(poster.getURL());
        } else {
            getWidget().setPoster(null);
        }
    }

    @Override
    public VVideo getWidget() {
        return (VVideo) super.getWidget();
    }

    @Override
    protected String getDefaultAltHtml() {
        return "Your browser does not support the <code>video</code> element.";
    }

}
