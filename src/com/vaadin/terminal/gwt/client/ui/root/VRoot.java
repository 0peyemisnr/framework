/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.ui.root;

import java.util.ArrayList;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.BrowserInfo;
import com.vaadin.terminal.gwt.client.ComponentConnector;
import com.vaadin.terminal.gwt.client.ConnectorMap;
import com.vaadin.terminal.gwt.client.Focusable;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ui.ShortcutActionHandler;
import com.vaadin.terminal.gwt.client.ui.ShortcutActionHandler.ShortcutActionHandlerOwner;
import com.vaadin.terminal.gwt.client.ui.TouchScrollDelegate;
import com.vaadin.terminal.gwt.client.ui.VLazyExecutor;
import com.vaadin.terminal.gwt.client.ui.textfield.VTextField;

/**
 *
 */
public class VRoot extends SimplePanel implements ResizeHandler,
        Window.ClosingHandler, ShortcutActionHandlerOwner, Focusable {

    public static final String FRAGMENT_VARIABLE = "fragment";

    private static final String CLASSNAME = "v-view";

    public static final String NOTIFICATION_HTML_CONTENT_NOT_ALLOWED = "useplain";

    String theme;

    String id;

    ShortcutActionHandler actionHandler;

    /*
     * Last known window size used to detect whether VView should be layouted
     * again. Detection must be based on window size, because the VView size
     * might be fixed and thus not automatically adapt to changed window sizes.
     */
    private int windowWidth;
    private int windowHeight;

    /*
     * Last know view size used to detect whether new dimensions should be sent
     * to the server.
     */
    private int viewWidth;
    private int viewHeight;

    ApplicationConnection connection;

    /** Identifies the click event */
    public static final String CLICK_EVENT_ID = "click";

    /**
     * We are postponing resize process with IE. IE bugs with scrollbars in some
     * situations, that causes false onWindowResized calls. With Timer we will
     * give IE some time to decide if it really wants to keep current size
     * (scrollbars).
     */
    private Timer resizeTimer;

    int scrollTop;

    int scrollLeft;

    boolean rendering;

    boolean scrollable;

    boolean immediate;

    boolean resizeLazy = false;

    /**
     * Attribute name for the lazy resize setting .
     */
    public static final String RESIZE_LAZY = "rL";

    private HandlerRegistration historyHandlerRegistration;

    /**
     * The current URI fragment, used to avoid sending updates if nothing has
     * changed.
     */
    String currentFragment;

    /**
     * Listener for URI fragment changes. Notifies the server of the new value
     * whenever the value changes.
     */
    private final ValueChangeHandler<String> historyChangeHandler = new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
            String newFragment = event.getValue();

            // Send the new fragment to the server if it has changed
            if (!newFragment.equals(currentFragment) && connection != null) {
                currentFragment = newFragment;
                connection.updateVariable(id, FRAGMENT_VARIABLE, newFragment,
                        true);
            }
        }
    };

    private VLazyExecutor delayedResizeExecutor = new VLazyExecutor(200,
            new ScheduledCommand() {
                @Override
                public void execute() {
                    windowSizeMaybeChanged(Window.getClientWidth(),
                            Window.getClientHeight());
                }

            });

    public VRoot() {
        super();
        setStyleName(CLASSNAME);

        // Allow focusing the view by using the focus() method, the view
        // should not be in the document focus flow
        getElement().setTabIndex(-1);
        TouchScrollDelegate.enableTouchScrolling(this, getElement());
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        historyHandlerRegistration = History
                .addValueChangeHandler(historyChangeHandler);
        currentFragment = History.getToken();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        historyHandlerRegistration.removeHandler();
        historyHandlerRegistration = null;
    }

    /**
     * Called when the window might have been resized.
     * 
     * @param newWidth
     *            The new width of the window
     * @param newHeight
     *            The new height of the window
     */
    protected void windowSizeMaybeChanged(int newWidth, int newHeight) {
        boolean changed = false;
        ComponentConnector connector = ConnectorMap.get(connection)
                .getConnector(this);
        if (windowWidth != newWidth) {
            windowWidth = newWidth;
            changed = true;
            connector.getLayoutManager().reportOuterWidth(connector, newWidth);
            VConsole.log("New window width: " + windowWidth);
        }
        if (windowHeight != newHeight) {
            windowHeight = newHeight;
            changed = true;
            connector.getLayoutManager()
                    .reportOuterHeight(connector, newHeight);
            VConsole.log("New window height: " + windowHeight);
        }
        if (changed) {
            /*
             * If the window size has changed, layout the VView again and send
             * new size to the server if the size changed. (Just checking VView
             * size would cause us to ignore cases when a relatively sized VView
             * should shrink as the content's size is fixed and would thus not
             * automatically shrink.)
             */
            VConsole.log("Running layout functions due to window resize");

            sendClientResized();

            connector.getLayoutManager().layoutNow();
        }
    }

    public String getTheme() {
        return theme;
    }

    /**
     * Used to reload host page on theme changes.
     */
    static native void reloadHostPage()
    /*-{
         $wnd.location.reload();
     }-*/;

    /**
     * Evaluate the given script in the browser document.
     * 
     * @param script
     *            Script to be executed.
     */
    static native void eval(String script)
    /*-{
      try {
         if (script == null) return;
         $wnd.eval(script);
      } catch (e) {
      }
    }-*/;

    /**
     * Returns true if the body is NOT generated, i.e if someone else has made
     * the page that we're running in. Otherwise we're in charge of the whole
     * page.
     * 
     * @return true if we're running embedded
     */
    public boolean isEmbedded() {
        return !getElement().getOwnerDocument().getBody().getClassName()
                .contains(ApplicationConnection.GENERATED_BODY_CLASSNAME);
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        int type = DOM.eventGetType(event);
        if (type == Event.ONKEYDOWN && actionHandler != null) {
            actionHandler.handleKeyboardEvent(event);
            return;
        } else if (scrollable && type == Event.ONSCROLL) {
            updateScrollPosition();
        }
    }

    /**
     * Updates scroll position from DOM and saves variables to server.
     */
    private void updateScrollPosition() {
        int oldTop = scrollTop;
        int oldLeft = scrollLeft;
        scrollTop = DOM.getElementPropertyInt(getElement(), "scrollTop");
        scrollLeft = DOM.getElementPropertyInt(getElement(), "scrollLeft");
        if (connection != null && !rendering) {
            if (oldTop != scrollTop) {
                connection.updateVariable(id, "scrollTop", scrollTop, false);
            }
            if (oldLeft != scrollLeft) {
                connection.updateVariable(id, "scrollLeft", scrollLeft, false);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.gwt.event.logical.shared.ResizeHandler#onResize(com.google
     * .gwt.event.logical.shared.ResizeEvent)
     */
    @Override
    public void onResize(ResizeEvent event) {
        onResize();
    }

    /**
     * Called when a resize event is received.
     */
    void onResize() {
        /*
         * IE (pre IE9 at least) will give us some false resize events due to
         * problems with scrollbars. Firefox 3 might also produce some extra
         * events. We postpone both the re-layouting and the server side event
         * for a while to deal with these issues.
         * 
         * We may also postpone these events to avoid slowness when resizing the
         * browser window. Constantly recalculating the layout causes the resize
         * operation to be really slow with complex layouts.
         */
        boolean lazy = resizeLazy || BrowserInfo.get().isIE8();

        if (lazy) {
            delayedResizeExecutor.trigger();
        } else {
            windowSizeMaybeChanged(Window.getClientWidth(),
                    Window.getClientHeight());
        }
    }

    /**
     * Send new dimensions to the server.
     */
    private void sendClientResized() {
        Element parentElement = getElement().getParentElement();
        int newViewHeight = parentElement.getClientHeight();
        int newViewWidth = parentElement.getClientWidth();

        // Send the view dimensions if they have changed
        if (newViewHeight != viewHeight || newViewWidth != viewWidth) {
            viewHeight = newViewHeight;
            viewWidth = newViewWidth;
            connection.updateVariable(id, "height", newViewHeight, false);
            connection.updateVariable(id, "width", newViewWidth, immediate);
        }
    }

    public native static void goTo(String url)
    /*-{
       $wnd.location = url;
     }-*/;

    @Override
    public void onWindowClosing(Window.ClosingEvent event) {
        // Change focus on this window in order to ensure that all state is
        // collected from textfields
        // TODO this is a naive hack, that only works with text fields and may
        // cause some odd issues. Should be replaced with a decent solution, see
        // also related BeforeShortcutActionListener interface. Same interface
        // might be usable here.
        VTextField.flushChangesFromFocusedTextField();
    }

    private native static void loadAppIdListFromDOM(ArrayList<String> list)
    /*-{
         var j;
         for(j in $wnd.vaadin.vaadinConfigurations) {
            // $entry not needed as function is not exported
            list.@java.util.Collection::add(Ljava/lang/Object;)(j);
         }
     }-*/;

    @Override
    public ShortcutActionHandler getShortcutActionHandler() {
        return actionHandler;
    }

    @Override
    public void focus() {
        getElement().focus();
    }

}
