/* 
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.client.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.shared.GWT;
import com.vaadin.client.metadata.AsyncBundleLoader.State;

public abstract class ConnectorBundleLoader {
    public static final String EAGER_BUNDLE_NAME = "__eager";
    public static final String DEFERRED_BUNDLE_NAME = "__deferred";

    private static ConnectorBundleLoader impl;

    private Map<String, AsyncBundleLoader> asyncBlockLoaders = new HashMap<String, AsyncBundleLoader>();
    private Map<String, String> identifierToBundle = new HashMap<String, String>();

    private final TypeDataStore datStore = new TypeDataStore();

    public ConnectorBundleLoader() {
        init();
    }

    public TypeDataStore getTypeDataStore() {
        return datStore;
    }

    public static ConnectorBundleLoader get() {
        if (impl == null) {
            impl = GWT.create(ConnectorBundleLoader.class);
        }
        return impl;
    }

    public void loadBundle(String packageName, BundleLoadCallback callback) {
        AsyncBundleLoader loader = asyncBlockLoaders.get(packageName);
        switch (loader.getState()) {
        case NOT_STARTED:
            loader.load(callback, getTypeDataStore());
            break;
        case LOADING:
            loader.addCallback(callback);
            break;
        case LOADED:
            if (callback != null) {
                callback.loaded();
            }
            break;
        case ERROR:
            if (callback != null) {
                callback.failed(loader.getError());
            }
        }
    }

    public boolean isBundleLoaded(String bundleName) {
        AsyncBundleLoader loader = asyncBlockLoaders.get(bundleName);
        if (loader == null) {
            throw new IllegalArgumentException("Bundle " + bundleName
                    + " not recognized");
        }
        return loader.getState() == State.LOADED;
    }

    public void setLoaded(String packageName) {
        List<BundleLoadCallback> callbacks = asyncBlockLoaders.get(packageName)
                .setLoaded();
        for (BundleLoadCallback callback : callbacks) {
            if (callback != null) {
                callback.loaded();
            }
        }
    }

    public void setLoadFailure(String bundleName, Throwable reason) {
        reason = new RuntimeException("Failed to load bundle " + bundleName
                + ": " + reason.getMessage(), reason);
        List<BundleLoadCallback> callbacks = asyncBlockLoaders.get(bundleName)
                .setError(reason);
        for (BundleLoadCallback callback : callbacks) {
            callback.failed(reason);
        }
    }

    public String getBundleForIdentifier(String identifier) {
        return identifierToBundle.get(identifier);
    }

    protected void addAsyncBlockLoader(AsyncBundleLoader loader) {
        String name = loader.getName();
        asyncBlockLoaders.put(name, loader);
        String[] indentifiers = loader.getIndentifiers();
        for (String identifier : indentifiers) {
            identifierToBundle.put(identifier, name);
        }
    }

    public abstract void init();

}
