package com.vaadin.tests.tickets;

import com.vaadin.Application;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI.LegacyWindow;

public class Ticket2426 extends Application.LegacyApplication {

    @Override
    public void init() {
        LegacyWindow w = new LegacyWindow();
        setMainWindow(w);

        final String content = "<select/>";

        w.addComponent(new Label("CONTENT_DEFAULT: " + content,
                ContentMode.TEXT));
        w.addComponent(new Label("CONTENT_PREFORMATTED: " + content,
                ContentMode.PREFORMATTED));
        w.addComponent(new Label("CONTENT_RAW: " + content, ContentMode.RAW));
        w.addComponent(new Label("CONTENT_TEXT: " + content, ContentMode.TEXT));
        w.addComponent(new Label("CONTENT_XML: " + content, ContentMode.XML));
        w.addComponent(new Label("CONTENT_XHTML: " + content, ContentMode.XHTML));

    }

}
