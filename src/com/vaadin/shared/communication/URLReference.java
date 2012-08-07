/* 
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.shared.communication;

import java.io.Serializable;

public class URLReference implements Serializable {

    private String URL;

    /**
     * Returns the URL that this object refers to.
     * <p>
     * Note that the URL can use special protocols like theme://
     * 
     * @return The URL for this reference or null if unknown.
     */
    public String getURL() {
        return URL;
    }

    /**
     * Sets the URL that this object refers to
     * 
     * @param URL
     */
    public void setURL(String URL) {
        this.URL = URL;
    }
}