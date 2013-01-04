/*
 * Copyright 2000-2013 Vaadin Ltd.
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
package com.vaadin.client;

public class TooltipInfo {

    private String title;

    private String errorMessageHtml;

    public TooltipInfo() {
    }

    public TooltipInfo(String tooltip) {
        setTitle(tooltip);
    }

    public TooltipInfo(String tooltip, String errorMessage) {
        setTitle(tooltip);
        setErrorMessage(errorMessage);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getErrorMessage() {
        return errorMessageHtml;
    }

    public void setErrorMessage(String errorMessage) {
        errorMessageHtml = errorMessage;
    }

    /**
     * Checks is a message has been defined for the tooltip.
     * 
     * @return true if title or error message is present, false if both are
     *         empty
     */
    public boolean hasMessage() {
        return (title != null && !title.isEmpty())
                || (errorMessageHtml != null && !errorMessageHtml.isEmpty());
    }

    public boolean equals(TooltipInfo other) {
        return (other != null && other.title == title && other.errorMessageHtml == errorMessageHtml);
    }
}
