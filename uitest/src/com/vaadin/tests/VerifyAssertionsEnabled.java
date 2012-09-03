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

package com.vaadin.tests;

import com.vaadin.server.WrappedRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.Label;

public class VerifyAssertionsEnabled extends AbstractTestUI {

    @Override
    protected void setup(WrappedRequest request) {
        try {
            assert false;
            addComponent(new Label("Assertions are not enabled"));
        } catch (AssertionError e) {
            addComponent(new Label("Assertions are enabled"));
        }
    }

    @Override
    protected String getTestDescription() {
        return "Tests whether the testing server is run with assertions enabled.";
    }

    @Override
    protected Integer getTicketNumber() {
        return Integer.valueOf(9450);
    }

}
