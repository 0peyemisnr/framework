/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.event.selection;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.ui.AbstractMultiSelect;

/**
 * Event fired when the the selection changes in a
 * {@link com.vaadin.shared.data.selection.SelectionModel.Multi}.
 *
 * @author Vaadin Ltd
 *
 * @since 8.0
 *
 * @param <T>
 *            the data type of the selection model
 */
public class MultiSelectionEvent<T> extends ValueChangeEvent<Set<T>>
        implements SelectionEvent<T> {

    private final Set<T> oldSelection;

    /**
     * Creates a new event.
     *
     * @param source
     *            the listing component in which the selection changed
     * @param oldSelection
     *            the old set of selected items
     * @param userOriginated
     *            {@code true} if this event originates from the client,
     *            {@code false} otherwise.
     */
    public MultiSelectionEvent(AbstractMultiSelect<T> source,
            Set<T> oldSelection, boolean userOriginated) {
        super(source, userOriginated);
        this.oldSelection = oldSelection;
    }

    /**
     * Gets the new selection.
     * <p>
     * The result is the current selection of the source
     * {@link AbstractMultiSelect} object. So it's always exactly the same as
     * {@link AbstractMultiSelect#getValue()}
     * 
     * @see #getValue()
     *
     * @return a set of items selected after the selection was changed
     */
    public Set<T> getNewSelection() {
        return getValue();
    }

    /**
     * Gets the old selection.
     *
     * @return a set of items selected before the selection was changed
     */
    public Set<T> getOldSelection() {
        return Collections.unmodifiableSet(oldSelection);
    }

    @Override
    public Optional<T> getFirstSelected() {
        return getValue().stream().findFirst();
    }
}
