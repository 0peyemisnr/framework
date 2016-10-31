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

package com.vaadin.ui;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.vaadin.data.HasValue;
import com.vaadin.event.FieldEvents;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusAndBlurServerRpcDecorator;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.server.KeyMapper;
import com.vaadin.server.Resource;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.data.DataCommunicator;
import com.vaadin.server.data.DataKeyMapper;
import com.vaadin.server.data.DataSource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.data.DataCommunicatorConstants;
import com.vaadin.shared.ui.combobox.ComboBoxConstants;
import com.vaadin.shared.ui.combobox.ComboBoxServerRpc;
import com.vaadin.shared.ui.combobox.ComboBoxState;

import elemental.json.JsonObject;

/**
 * A filtering dropdown single-select. Items are filtered based on user input.
 * Supports the creation of new items when a handler is set by the user.
 *
 * @param <T>
 *            item (bean) type in ComboBox
 * @author Vaadin Ltd
 */
@SuppressWarnings("serial")
public class ComboBox<T> extends AbstractSingleSelect<T> implements HasValue<T>,
        FieldEvents.BlurNotifier, FieldEvents.FocusNotifier {

    /**
     * Custom single selection model for ComboBox.
     */
    protected class ComboBoxSelectionModel extends SimpleSingleSelection {
        @Override
        protected void doSetSelectedKey(String key) {
            super.doSetSelectedKey(key);

            String selectedCaption = null;
            T value = getDataCommunicator().getKeyMapper().get(key);
            if (value != null) {
                selectedCaption = getItemCaptionGenerator().apply(value);
            }
            getState().selectedItemCaption = selectedCaption;
        }

    }

    /**
     * Handler that adds a new item based on user input when the new items
     * allowed mode is active.
     */
    @FunctionalInterface
    public interface NewItemHandler extends Consumer<String>, Serializable {
    }

    /**
     * Filter can be used to customize the filtering of items based on user
     * input.
     *
     * @see ComboBox#setFilter(ItemFilter)
     * @param <T>
     *            item type in the combo box
     */
    @FunctionalInterface
    public interface ItemFilter<T>
            extends BiFunction<String, T, Boolean>, Serializable {
    }

    private ComboBoxServerRpc rpc = new ComboBoxServerRpc() {
        @Override
        public void createNewItem(String itemValue) {
            // New option entered
            if (getNewItemHandler() != null && itemValue != null
                    && itemValue.length() > 0) {
                getNewItemHandler().accept(itemValue);
                // rebuild list
                filterstring = null;
            }
        }

        @Override
        public void setFilter(String filterText) {
            filterstring = filterText;
            if (filterText != null) {
                getDataCommunicator().setInMemoryFilter(
                        item -> filter.apply(filterstring, item));
            } else {
                getDataCommunicator().setInMemoryFilter(null);
            }
        }
    };

    private String filterstring;

    /**
     * Handler for new items entered by the user.
     */
    private NewItemHandler newItemHandler;

    private ItemCaptionGenerator<T> itemCaptionGenerator = String::valueOf;

    private StyleGenerator<T> itemStyleGenerator = item -> null;
    private IconGenerator<T> itemIconGenerator = item -> null;

    private ItemFilter<T> filter = (filterText, item) -> {
        if (filterText == null) {
            return true;
        } else {
            return getItemCaptionGenerator().apply(item)
                    .toLowerCase(getLocale())
                    .contains(filterText.toLowerCase(getLocale()));
        }
    };

    /**
     * Constructs an empty combo box without a caption. The content of the combo
     * box can be set with {@link #setDataSource(DataSource)} or
     * {@link #setItems(Collection)}
     */
    public ComboBox() {
        super(new DataCommunicator<T>() {
            @Override
            protected DataKeyMapper<T> createKeyMapper() {
                return new KeyMapper<T>() {
                    @Override
                    public void remove(T removeobj) {
                        // never remove keys from ComboBox to support selection
                        // of items that are not currently visible
                    }
                };
            }
        });
        setSelectionModel(new ComboBoxSelectionModel());

        init();
    }

    /**
     * Constructs an empty combo box, whose content can be set with
     * {@link #setDataSource(DataSource)} or {@link #setItems(Collection)}.
     *
     * @param caption
     *            the caption to show in the containing layout, null for no
     *            caption
     */
    public ComboBox(String caption) {
        this();
        setCaption(caption);
    }

    /**
     * Constructs a combo box with a static in-memory data source with the given
     * options.
     *
     * @param caption
     *            the caption to show in the containing layout, null for no
     *            caption
     * @param options
     *            collection of options, not null
     */
    public ComboBox(String caption, Collection<T> options) {
        this(caption, DataSource.create(options));
    }

    /**
     * Constructs a combo box with the given data source.
     *
     * @param caption
     *            the caption to show in the containing layout, null for no
     *            caption
     * @param dataSource
     *            the data source to use, not null
     */
    public ComboBox(String caption, DataSource<T> dataSource) {
        this(caption);
        setDataSource(dataSource);
    }

    /**
     * Initialize the ComboBox with default settings and register client to
     * server RPC implementation.
     */
    private void init() {
        registerRpc(rpc);
        registerRpc(new FocusAndBlurServerRpcDecorator(this, this::fireEvent));

        addDataGenerator((T data, JsonObject jsonObject) -> {
            jsonObject.put(DataCommunicatorConstants.NAME,
                    getItemCaptionGenerator().apply(data));
            String style = itemStyleGenerator.apply(data);
            if (style != null) {
                jsonObject.put(ComboBoxConstants.STYLE, style);
            }
            Resource icon = itemIconGenerator.apply(data);
            if (icon != null) {
                String iconUrl = ResourceReference
                        .create(icon, ComboBox.this, null).getURL();
                jsonObject.put(ComboBoxConstants.ICON, iconUrl);
            }
        });
    }

    /**
     * Gets the current placeholder text shown when the combo box would be
     * empty.
     *
     * @see #setPlaceholder(String)
     * @return the current placeholder string, or null if not enabled
     */
    public String getPlaceholder() {
        return getState(false).placeholder;
    }

    /**
     * Sets the placeholder string - a textual prompt that is displayed when the
     * select would otherwise be empty, to prompt the user for input.
     *
     * @param placeholder
     *            the desired placeholder, or null to disable
     */
    public void setPlaceholder(String placeholder) {
        getState().placeholder = placeholder;
    }

    /**
     * Sets whether it is possible to input text into the field or whether the
     * field area of the component is just used to show what is selected. By
     * disabling text input, the comboBox will work in the same way as a
     * {@link NativeSelect}
     *
     * @see #isTextInputAllowed()
     *
     * @param textInputAllowed
     *            true to allow entering text, false to just show the current
     *            selection
     */
    public void setTextInputAllowed(boolean textInputAllowed) {
        getState().textInputAllowed = textInputAllowed;
    }

    /**
     * Returns true if the user can enter text into the field to either filter
     * the selections or enter a new value if {@link #isNewItemsAllowed()}
     * returns true. If text input is disabled, the comboBox will work in the
     * same way as a {@link NativeSelect}
     *
     * @return true if text input is allowed
     */
    public boolean isTextInputAllowed() {
        return getState(false).textInputAllowed;
    }

    @Override
    public Registration addBlurListener(BlurListener listener) {
        addListener(BlurEvent.EVENT_ID, BlurEvent.class, listener,
                BlurListener.blurMethod);
        return () -> removeListener(BlurEvent.EVENT_ID, BlurEvent.class,
                listener);
    }

    @Override
    @Deprecated
    public void removeBlurListener(BlurListener listener) {
        removeListener(BlurEvent.EVENT_ID, BlurEvent.class, listener);
    }

    @Override
    public Registration addFocusListener(FocusListener listener) {
        addListener(FocusEvent.EVENT_ID, FocusEvent.class, listener,
                FocusListener.focusMethod);
        return () -> removeListener(FocusEvent.EVENT_ID, FocusEvent.class,
                listener);
    }

    @Override
    @Deprecated
    public void removeFocusListener(FocusListener listener) {
        removeListener(FocusEvent.EVENT_ID, FocusEvent.class, listener);
    }

    /**
     * Returns the page length of the suggestion popup.
     *
     * @return the pageLength
     */
    public int getPageLength() {
        return getState(false).pageLength;
    }

    /**
     * Returns the suggestion pop-up's width as a CSS string.
     *
     * @see #setPopupWidth
     * @since 7.7
     * @return explicitly set popup width as CSS size string or null if not set
     */
    public String getPopupWidth() {
        return getState(false).suggestionPopupWidth;
    }

    /**
     * Sets the page length for the suggestion popup. Setting the page length to
     * 0 will disable suggestion popup paging (all items visible).
     *
     * @param pageLength
     *            the pageLength to set
     */
    public void setPageLength(int pageLength) {
        getState().pageLength = pageLength;
    }

    /**
     * Returns whether the user is allowed to select nothing in the combo box.
     *
     * @return true if empty selection is allowed, false otherwise
     */
    public boolean isEmptySelectionAllowed() {
        return getState(false).emptySelectionAllowed;
    }

    /**
     * Sets whether the user is allowed to select nothing in the combo box. When
     * true, a special empty item is shown to the user.
     *
     * @param emptySelectionAllowed
     *            true to allow not selecting anything, false to require
     *            selection
     */
    public void setEmptySelectionAllowed(boolean emptySelectionAllowed) {
        getState().emptySelectionAllowed = emptySelectionAllowed;
    }

    /**
     * Sets the suggestion pop-up's width as a CSS string. By using relative
     * units (e.g. "50%") it's possible to set the popup's width relative to the
     * ComboBox itself.
     *
     * @see #getPopupWidth()
     * @since 7.7
     * @param width
     *            the width
     */
    public void setPopupWidth(String width) {
        getState().suggestionPopupWidth = width;
    }

    /**
     * Sets whether to scroll the selected item visible (directly open the page
     * on which it is) when opening the combo box popup or not.
     *
     * This requires finding the index of the item, which can be expensive in
     * many large lazy loading containers.
     *
     * @param scrollToSelectedItem
     *            true to find the page with the selected item when opening the
     *            selection popup
     */
    public void setScrollToSelectedItem(boolean scrollToSelectedItem) {
        getState().scrollToSelectedItem = scrollToSelectedItem;
    }

    /**
     * Returns true if the select should find the page with the selected item
     * when opening the popup.
     *
     * @see #setScrollToSelectedItem(boolean)
     *
     * @return true if the page with the selected item will be shown when
     *         opening the popup
     */
    public boolean isScrollToSelectedItem() {
        return getState(false).scrollToSelectedItem;
    }

    /**
     * Gets the item caption generator that is used to produce the strings shown
     * in the combo box for each item.
     *
     * @return the item caption generator used, not null
     */
    public ItemCaptionGenerator<T> getItemCaptionGenerator() {
        return itemCaptionGenerator;
    }

    /**
     * Sets the item caption generator that is used to produce the strings shown
     * in the combo box for each item. By default,
     * {@link String#valueOf(Object)} is used.
     *
     * @param itemCaptionGenerator
     *            the item caption provider to use, not null
     */
    public void setItemCaptionGenerator(
            ItemCaptionGenerator<T> itemCaptionGenerator) {
        Objects.requireNonNull(itemCaptionGenerator,
                "Item caption generators must not be null");
        this.itemCaptionGenerator = itemCaptionGenerator;
        getDataCommunicator().reset();
    }

    /**
     * Sets the style generator that is used to produce custom class names for
     * items visible in the popup. The CSS class name that will be added to the
     * item is <tt>v-filterselect-item-[style name]</tt>. Returning null from
     * the generator results in no custom style name being set.
     *
     * @see StyleGenerator
     *
     * @param itemStyleGenerator
     *            the item style generator to set, not null
     * @throws NullPointerException
     *             if {@code itemStyleGenerator} is {@code null}
     */
    public void setStyleGenerator(StyleGenerator<T> itemStyleGenerator) {
        Objects.requireNonNull(itemStyleGenerator,
                "Item style generator must not be null");
        this.itemStyleGenerator = itemStyleGenerator;
        getDataCommunicator().reset();
    }

    /**
     * Gets the currently used style generator that is used to generate CSS
     * class names for items. The default item style provider returns null for
     * all items, resulting in no custom item class names being set.
     *
     * @see StyleGenerator
     * @see #setStyleGenerator(StyleGenerator)
     *
     * @return the currently used item style generator, not null
     */
    public StyleGenerator<T> getStyleGenerator() {
        return itemStyleGenerator;
    }

    /**
     * Sets the item icon generator that is used to produce custom icons for
     * showing items in the popup. The generator can return null for items with
     * no icon.
     *
     * @see IconGenerator
     *
     * @param itemIconGenerator
     *            the item icon generator to set, not null
     * @throws NullPointerException
     *             if {@code itemIconGenerator} is {@code null}
     */
    public void setItemIconGenerator(IconGenerator<T> itemIconGenerator) {
        Objects.requireNonNull(itemIconGenerator,
                "Item icon generator must not be null");
        this.itemIconGenerator = itemIconGenerator;
        getDataCommunicator().reset();
    }

    /**
     * Gets the currently used item icon generator. The default item icon
     * provider returns null for all items, resulting in no icons being used.
     *
     * @see IconGenerator
     * @see #setItemIconGenerator(IconGenerator)
     *
     * @return the currently used item icon generator, not null
     */
    public IconGenerator<T> getItemIconGenerator() {
        return itemIconGenerator;
    }

    /**
     * Sets the handler that is called when user types a new item. The creation
     * of new items is allowed when a new item handler has been set.
     *
     * @param newItemHandler
     *            handler called for new items, null to only permit the
     *            selection of existing items
     */
    public void setNewItemHandler(NewItemHandler newItemHandler) {
        this.newItemHandler = newItemHandler;
        getState().allowNewItems = (newItemHandler != null);
        markAsDirty();
    }

    /**
     * Returns the handler called when the user enters a new item (not present
     * in the data source).
     *
     * @return new item handler or null if none specified
     */
    public NewItemHandler getNewItemHandler() {
        return newItemHandler;
    }

    // HasValue methods delegated to the selection model

    /**
     * Returns the filter used to customize the list based on user input.
     *
     * @return the current filter, not null
     */
    public ItemFilter<T> getFilter() {
        return filter;
    }

    /**
     * Sets the filter used to customize the list based on user input. The
     * default filter checks case-insensitively that the input string is
     * contained in the item caption.
     *
     * @param filter
     *            the filter function to use, not null
     */
    public void setFilter(ItemFilter<T> filter) {
        Objects.requireNonNull(filter, "Item filter must not be null");
        this.filter = filter;
    }

    /**
     * Sets the value of this object. If the new value is not equal to
     * {@code getValue()}, fires a {@link ValueChangeEvent}.
     *
     * @param value
     *            the new value, may be {@code null}
     */
    @Override
    public void setValue(T value) {
        getSelectionModel().setSelectedFromServer(value);

    }

    @Override
    public T getValue() {
        return getSelectionModel().getSelectedItem().orElse(null);
    }

    @Override
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<T> listener) {
        return addSelectionListener(event -> {
            listener.accept(new ValueChangeEvent<>(event.getComponent(), this,
                    event.isUserOriginated()));
        });
    }

    @Override
    protected ComboBoxState getState() {
        return (ComboBoxState) super.getState();
    }

    @Override
    protected ComboBoxState getState(boolean markAsDirty) {
        return (ComboBoxState) super.getState(markAsDirty);
    }

}
