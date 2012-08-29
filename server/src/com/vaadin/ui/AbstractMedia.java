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

package com.vaadin.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.shared.communication.URLReference;
import com.vaadin.shared.ui.AbstractMediaState;
import com.vaadin.shared.ui.MediaControl;

/**
 * Abstract base class for the HTML5 media components.
 * 
 * @author Vaadin Ltd
 */
public abstract class AbstractMedia extends AbstractComponent {

    @Override
    protected AbstractMediaState getState() {
        return (AbstractMediaState) super.getState();
    }

    /**
     * Sets a single media file as the source of the media component.
     * 
     * @param source
     */
    public void setSource(Resource source) {
        clearSources();

        addSource(source);
    }

    private void clearSources() {
        getState().getSources().clear();
        getState().getSourceTypes().clear();
    }

    /**
     * Adds an alternative media file to the sources list. Which of the sources
     * is used is selected by the browser depending on which file formats it
     * supports. See <a
     * href="http://en.wikipedia.org/wiki/HTML5_video#Table">wikipedia</a> for a
     * table of formats supported by different browsers.
     * 
     * @param source
     */
    public void addSource(Resource source) {
        if (source != null) {
            getState().getSources().add(new ResourceReference(source));
            getState().getSourceTypes().add(source.getMIMEType());
        }
    }

    /**
     * Set multiple sources at once. Which of the sources is used is selected by
     * the browser depending on which file formats it supports. See <a
     * href="http://en.wikipedia.org/wiki/HTML5_video#Table">wikipedia</a> for a
     * table of formats supported by different browsers.
     * 
     * @param sources
     */
    public void setSources(Resource... sources) {
        clearSources();
        for (Resource source : sources) {
            addSource(source);
        }
    }

    /**
     * @return The sources pointed to in this media.
     */
    public List<Resource> getSources() {
        ArrayList<Resource> sources = new ArrayList<Resource>();
        for (URLReference ref : getState().getSources()) {
            sources.add(((ResourceReference) ref).getResource());
        }
        return sources;
    }

    /**
     * Sets whether or not the browser should show native media controls.
     * 
     * @param showControls
     */
    public void setShowControls(boolean showControls) {
        getState().setShowControls(showControls);
    }

    /**
     * @return true if the browser is to show native media controls.
     */
    public boolean isShowControls() {
        return getState().isShowControls();
    }

    /**
     * Sets the alternative text to be displayed if the browser does not support
     * HTML5. This text is rendered as HTML if
     * {@link #setHtmlContentAllowed(boolean)} is set to true. With HTML
     * rendering, this method can also be used to implement fallback to a
     * flash-based player, see the <a href=
     * "https://developer.mozilla.org/En/Using_audio_and_video_in_Firefox#Using_Flash"
     * >Mozilla Developer Network</a> for details.
     * 
     * @param altText
     */
    public void setAltText(String altText) {
        getState().setAltText(altText);
    }

    /**
     * @return The text/html that is displayed when a browser doesn't support
     *         HTML5.
     */
    public String getAltText() {
        return getState().getAltText();
    }

    /**
     * Set whether the alternative text ({@link #setAltText(String)}) is
     * rendered as HTML or not.
     * 
     * @param htmlContentAllowed
     */
    public void setHtmlContentAllowed(boolean htmlContentAllowed) {
        getState().setHtmlContentAllowed(htmlContentAllowed);
    }

    /**
     * @return true if the alternative text ({@link #setAltText(String)}) is to
     *         be rendered as HTML.
     */
    public boolean isHtmlContentAllowed() {
        return getState().isHtmlContentAllowed();
    }

    /**
     * Sets whether the media is to automatically start playback when enough
     * data has been loaded.
     * 
     * @param autoplay
     */
    public void setAutoplay(boolean autoplay) {
        getState().setAutoplay(autoplay);
    }

    /**
     * @return true if the media is set to automatically start playback.
     */
    public boolean isAutoplay() {
        return getState().isAutoplay();
    }

    /**
     * Set whether to mute the audio or not.
     * 
     * @param muted
     */
    public void setMuted(boolean muted) {
        getState().setMuted(muted);
    }

    /**
     * @return true if the audio is muted.
     */
    public boolean isMuted() {
        return getState().isMuted();
    }

    /**
     * Pauses the media.
     */
    public void pause() {
        getRpcProxy(MediaControl.class).pause();
    }

    /**
     * Starts playback of the media.
     */
    public void play() {
        getRpcProxy(MediaControl.class).play();
    }

}
