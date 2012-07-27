/* 
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.shared.ui.button;

import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.communication.ServerRpc;

/**
 * RPC interface for calls from client to server.
 * 
 * @since 7.0
 */
public interface ButtonServerRpc extends ServerRpc {
    /**
     * Button click event.
     * 
     * @param mouseEventDetails
     *            serialized mouse event details
     */
    public void click(MouseEventDetails mouseEventDetails);

    /**
     * Indicate to the server that the client has disabled the button as a
     * result of a click.
     */
    public void disableOnClick();
}