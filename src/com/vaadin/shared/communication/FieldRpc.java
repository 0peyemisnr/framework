/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.shared.communication;

public class FieldRpc {
    public interface FocusServerRpc extends ServerRpc {
        public void focus();
    }

    public interface BlurServerRpc extends ServerRpc {
        public void blur();
    }

    public interface FocusAndBlurServerRpc extends FocusServerRpc,
            BlurServerRpc {

    }
}
