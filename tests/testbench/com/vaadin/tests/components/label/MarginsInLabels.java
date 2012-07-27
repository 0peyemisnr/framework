package com.vaadin.tests.components.label;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.terminal.WrappedRequest;
import com.vaadin.tests.components.AbstractTestRoot;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

public class MarginsInLabels extends AbstractTestRoot {

    @Override
    protected void setup(WrappedRequest request) {
        AbstractLayout layout = new VerticalLayout();
        layout.addComponent(new Label("<h1>Vertical layout</h1>",
                ContentMode.XHTML));
        layout.addComponent(new Label("Next row"));
        addComponent(layout);

        layout = new GridLayout(1, 2);
        layout.setWidth("100%");
        layout.addComponent(new Label("<h1>Grid layout</h1>", ContentMode.XHTML));
        layout.addComponent(new Label("Next row"));
        addComponent(layout);

        TabSheet tabSheet = new TabSheet();
        tabSheet.addTab(new Label("<h1>Tabsheet</h1>", ContentMode.XHTML),
                "Label");
        addComponent(tabSheet);

        Accordion accordion = new Accordion();
        accordion.addTab(new Label("<h1>Accordion</h1>", ContentMode.XHTML),
                "Label");
        addComponent(accordion);
    }

    @Override
    protected String getTestDescription() {
        return "Margins inside labels should not be allowed to collapse out of the label as it causes problems with layotus measuring the label.";
    }

    @Override
    protected Integer getTicketNumber() {
        return Integer.valueOf(8671);
    }

}
