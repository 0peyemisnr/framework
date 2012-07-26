/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.terminal.gwt.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.vaadin.Application;
import com.vaadin.Application.ApplicationStartEvent;
import com.vaadin.Application.SystemMessages;
import com.vaadin.terminal.DeploymentConfiguration;
import com.vaadin.terminal.Terminal;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.WrappedRequest;
import com.vaadin.terminal.WrappedResponse;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.server.AbstractCommunicationManager.Callback;
import com.vaadin.ui.Root;

/**
 * Abstract implementation of the ApplicationServlet which handles all
 * communication between the client and the server.
 * 
 * It is possible to extend this class to provide own functionality but in most
 * cases this is unnecessary.
 * 
 * 
 * @author Vaadin Ltd.
 * @version
 * @VERSION@
 * @since 6.0
 */

@SuppressWarnings("serial")
public abstract class AbstractApplicationServlet extends HttpServlet implements
        Constants {

    private static class AbstractApplicationServletWrapper implements Callback {

        private final AbstractApplicationServlet servlet;

        public AbstractApplicationServletWrapper(
                AbstractApplicationServlet servlet) {
            this.servlet = servlet;
        }

        @Override
        public void criticalNotification(WrappedRequest request,
                WrappedResponse response, String cap, String msg,
                String details, String outOfSyncURL) throws IOException {
            servlet.criticalNotification(
                    WrappedHttpServletRequest.cast(request),
                    ((WrappedHttpServletResponse) response), cap, msg, details,
                    outOfSyncURL);
        }
    }

    // TODO Move some (all?) of the constants to a separate interface (shared
    // with portlet)

    private Properties applicationProperties;

    private boolean productionMode = false;

    private final String resourcePath = null;

    private int resourceCacheTime = 3600;

    private DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration() {

        @Override
        public String getStaticFileLocation(WrappedRequest request) {
            HttpServletRequest servletRequest = WrappedHttpServletRequest
                    .cast(request);
            return AbstractApplicationServlet.this
                    .getStaticFilesLocation(servletRequest);
        }

        @Override
        public String getConfiguredWidgetset(WrappedRequest request) {
            return getApplicationOrSystemProperty(
                    AbstractApplicationServlet.PARAMETER_WIDGETSET,
                    AbstractApplicationServlet.DEFAULT_WIDGETSET);
        }

        @Override
        public String getConfiguredTheme(WrappedRequest request) {
            // Use the default
            return AbstractApplicationServlet.getDefaultTheme();
        }

        @Override
        public String getApplicationOrSystemProperty(String propertyName,
                String defaultValue) {
            return AbstractApplicationServlet.this
                    .getApplicationOrSystemProperty(propertyName, defaultValue);
        }

        @Override
        public boolean isStandalone(WrappedRequest request) {
            return true;
        }

        @Override
        public ClassLoader getClassLoader() {
            try {
                return AbstractApplicationServlet.this.getClassLoader();
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
    };

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service.
     * 
     * @param servletConfig
     *            the object containing the servlet's configuration and
     *            initialization parameters
     * @throws javax.servlet.ServletException
     *             if an exception has occurred that interferes with the
     *             servlet's normal operation.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(javax.servlet.ServletConfig servletConfig)
            throws javax.servlet.ServletException {
        super.init(servletConfig);
        applicationProperties = new Properties();

        // Read default parameters from server.xml
        final ServletContext context = servletConfig.getServletContext();
        for (final Enumeration<String> e = context.getInitParameterNames(); e
                .hasMoreElements();) {
            final String name = e.nextElement();
            applicationProperties.setProperty(name,
                    context.getInitParameter(name));
        }

        // Override with application config from web.xml
        for (final Enumeration<String> e = servletConfig
                .getInitParameterNames(); e.hasMoreElements();) {
            final String name = e.nextElement();
            applicationProperties.setProperty(name,
                    servletConfig.getInitParameter(name));
        }

        checkProductionMode();
        checkCrossSiteProtection();
        checkResourceCacheTime();
    }

    private void checkCrossSiteProtection() {
        if (getApplicationOrSystemProperty(
                SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION, "false").equals(
                "true")) {
            /*
             * Print an information/warning message about running with xsrf
             * protection disabled
             */
            getLogger().warning(WARNING_XSRF_PROTECTION_DISABLED);
        }
    }

    private void checkProductionMode() {
        // Check if the application is in production mode.
        // We are in production mode if productionMode=true
        if (getApplicationOrSystemProperty(SERVLET_PARAMETER_PRODUCTION_MODE,
                "false").equals("true")) {
            productionMode = true;
        }

        if (!productionMode) {
            /* Print an information/warning message about running in debug mode */
            getLogger().warning(NOT_PRODUCTION_MODE_INFO);
        }

    }

    private void checkResourceCacheTime() {
        // Check if the browser caching time has been set in web.xml
        try {
            String rct = getApplicationOrSystemProperty(
                    SERVLET_PARAMETER_RESOURCE_CACHE_TIME, "3600");
            resourceCacheTime = Integer.parseInt(rct);
        } catch (NumberFormatException nfe) {
            // Default is 1h
            resourceCacheTime = 3600;
            getLogger().warning(WARNING_RESOURCE_CACHING_TIME_NOT_NUMERIC);
        }
    }

    /**
     * Gets an application property value.
     * 
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    protected String getApplicationProperty(String parameterName) {

        String val = applicationProperties.getProperty(parameterName);
        if (val != null) {
            return val;
        }

        // Try lower case application properties for backward compatibility with
        // 3.0.2 and earlier
        val = applicationProperties.getProperty(parameterName.toLowerCase());

        return val;
    }

    /**
     * Gets an system property value.
     * 
     * @param parameterName
     *            the Name or the parameter.
     * @return String value or null if not found
     */
    protected String getSystemProperty(String parameterName) {
        String val = null;

        String pkgName;
        final Package pkg = getClass().getPackage();
        if (pkg != null) {
            pkgName = pkg.getName();
        } else {
            final String className = getClass().getName();
            pkgName = new String(className.toCharArray(), 0,
                    className.lastIndexOf('.'));
        }
        val = System.getProperty(pkgName + "." + parameterName);
        if (val != null) {
            return val;
        }

        // Try lowercased system properties
        val = System.getProperty(pkgName + "." + parameterName.toLowerCase());
        return val;
    }

    /**
     * Gets an application or system property value.
     * 
     * @param parameterName
     *            the Name or the parameter.
     * @param defaultValue
     *            the Default to be used.
     * @return String value or default if not found
     */
    String getApplicationOrSystemProperty(String parameterName,
            String defaultValue) {

        String val = null;

        // Try application properties
        val = getApplicationProperty(parameterName);
        if (val != null) {
            return val;
        }

        // Try system properties
        val = getSystemProperty(parameterName);
        if (val != null) {
            return val;
        }

        return defaultValue;
    }

    /**
     * Returns true if the servlet is running in production mode. Production
     * mode disables all debug facilities.
     * 
     * @return true if in production mode, false if in debug mode
     */
    public boolean isProductionMode() {
        return productionMode;
    }

    /**
     * Returns the amount of milliseconds the browser should cache a file.
     * Default is 1 hour (3600 ms).
     * 
     * @return The amount of milliseconds files are cached in the browser
     */
    public int getResourceCacheTime() {
        return resourceCacheTime;
    }

    /**
     * Receives standard HTTP requests from the public service method and
     * dispatches them.
     * 
     * @param request
     *            the object that contains the request the client made of the
     *            servlet.
     * @param response
     *            the object that contains the response the servlet returns to
     *            the client.
     * @throws ServletException
     *             if an input or output error occurs while the servlet is
     *             handling the TRACE request.
     * @throws IOException
     *             if the request for the TRACE cannot be handled.
     */

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        service(createWrappedRequest(request), createWrappedResponse(response));
    }

    private void service(WrappedHttpServletRequest request,
            WrappedHttpServletResponse response) throws ServletException,
            IOException {
        RequestTimer requestTimer = new RequestTimer();
        requestTimer.start();

        AbstractApplicationServletWrapper servletWrapper = new AbstractApplicationServletWrapper(
                this);

        RequestType requestType = getRequestType(request);
        if (!ensureCookiesEnabled(requestType, request, response)) {
            return;
        }

        if (requestType == RequestType.STATIC_FILE) {
            serveStaticResources(request, response);
            return;
        }

        Application application = null;
        boolean transactionStarted = false;
        boolean requestStarted = false;

        try {
            // If a duplicate "close application" URL is received for an
            // application that is not open, redirect to the application's main
            // page.
            // This is needed as e.g. Spring Security remembers the last
            // URL from the application, which is the logout URL, and repeats
            // it.
            // We can tell apart a real onunload request from a repeated one
            // based on the real one having content (at least the UIDL security
            // key).
            if (requestType == RequestType.UIDL
                    && request.getParameterMap().containsKey(
                            ApplicationConnection.PARAM_UNLOADBURST)
                    && request.getContentLength() < 1
                    && getExistingApplication(request, false) == null) {
                redirectToApplication(request, response);
                return;
            }

            // Find out which application this request is related to
            application = findApplicationInstance(request, requestType);
            if (application == null) {
                return;
            }
            Application.setCurrent(application);

            /*
             * Get or create a WebApplicationContext and an ApplicationManager
             * for the session
             */
            WebApplicationContext webApplicationContext = getApplicationContext(request
                    .getSession());
            CommunicationManager applicationManager = webApplicationContext
                    .getApplicationManager(application, this);

            if (requestType == RequestType.CONNECTOR_RESOURCE) {
                String pathInfo = getRequestPathInfo(request);
                // + 2 to also remove beginning and ending slashes
                String resourceName = pathInfo
                        .substring(ApplicationConnection.CONNECTOR_RESOURCE_PREFIX
                                .length() + 2);

                final String mimetype = getServletContext().getMimeType(
                        resourceName);

                applicationManager.serveConnectorResource(resourceName,
                        request, response, mimetype);
                return;
            }

            /* Update browser information from the request */
            webApplicationContext.getBrowser().updateRequestDetails(request);

            /*
             * Call application requestStart before Application.init() is called
             * (bypasses the limitation in TransactionListener)
             */
            if (application instanceof HttpServletRequestListener) {
                ((HttpServletRequestListener) application).onRequestStart(
                        request, response);
                requestStarted = true;
            }

            // Start the application if it's newly created
            startApplication(request, application, webApplicationContext);

            /*
             * Transaction starts. Call transaction listeners. Transaction end
             * is called in the finally block below.
             */
            webApplicationContext.startTransaction(application, request);
            transactionStarted = true;

            /* Handle the request */
            if (requestType == RequestType.FILE_UPLOAD) {
                // Root is resolved in communication manager
                applicationManager.handleFileUpload(application, request,
                        response);
                return;
            } else if (requestType == RequestType.UIDL) {
                Root root = application.getRootForRequest(request);
                if (root == null) {
                    throw new ServletException(ERROR_NO_ROOT_FOUND);
                }
                // Handles AJAX UIDL requests
                applicationManager.handleUidlRequest(request, response,
                        servletWrapper, root);
                return;
            } else if (requestType == RequestType.BROWSER_DETAILS) {
                // Browser details - not related to a specific root
                applicationManager.handleBrowserDetailsRequest(request,
                        response, application);
                return;
            }

            // Removes application if it has stopped (maybe by thread or
            // transactionlistener)
            if (!application.isRunning()) {
                endApplication(request, response, application);
                return;
            }

            if (applicationManager.handleApplicationRequest(request, response)) {
                return;
            }
            // TODO Should return 404 error here and not do anything more

        } catch (final SessionExpiredException e) {
            // Session has expired, notify user
            handleServiceSessionExpired(request, response);
        } catch (final GeneralSecurityException e) {
            handleServiceSecurityException(request, response);
        } catch (final Throwable e) {
            handleServiceException(request, response, application, e);
        } finally {
            // Notifies transaction end
            try {
                if (transactionStarted) {
                    ((WebApplicationContext) application.getContext())
                            .endTransaction(application, request);

                }

            } finally {
                try {
                    if (requestStarted) {
                        ((HttpServletRequestListener) application)
                                .onRequestEnd(request, response);
                    }
                } finally {
                    Root.setCurrent(null);
                    Application.setCurrent(null);

                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        requestTimer.stop(getApplicationContext(session));
                    }
                }
            }

        }
    }

    private WrappedHttpServletResponse createWrappedResponse(
            HttpServletResponse response) {
        WrappedHttpServletResponse wrappedResponse = new WrappedHttpServletResponse(
                response, getDeploymentConfiguration());
        return wrappedResponse;
    }

    /**
     * Create a wrapped request for a http servlet request. This method can be
     * overridden if the wrapped request should have special properties.
     * 
     * @param request
     *            the original http servlet request
     * @return a wrapped request for the original request
     */
    protected WrappedHttpServletRequest createWrappedRequest(
            HttpServletRequest request) {
        return new WrappedHttpServletRequest(request,
                getDeploymentConfiguration());
    }

    /**
     * Gets a the deployment configuration for this servlet.
     * 
     * @return the deployment configuration
     */
    protected DeploymentConfiguration getDeploymentConfiguration() {
        return deploymentConfiguration;
    }

    /**
     * Check that cookie support is enabled in the browser. Only checks UIDL
     * requests.
     * 
     * @param requestType
     *            Type of the request as returned by
     *            {@link #getRequestType(HttpServletRequest)}
     * @param request
     *            The request from the browser
     * @param response
     *            The response to which an error can be written
     * @return false if cookies are disabled, true otherwise
     * @throws IOException
     */
    private boolean ensureCookiesEnabled(RequestType requestType,
            WrappedHttpServletRequest request,
            WrappedHttpServletResponse response) throws IOException {
        if (requestType == RequestType.UIDL && !isRepaintAll(request)) {
            // In all other but the first UIDL request a cookie should be
            // returned by the browser.
            // This can be removed if cookieless mode (#3228) is supported
            if (request.getRequestedSessionId() == null) {
                // User has cookies disabled
                criticalNotification(request, response, getSystemMessages()
                        .getCookiesDisabledCaption(), getSystemMessages()
                        .getCookiesDisabledMessage(), null, getSystemMessages()
                        .getCookiesDisabledURL());
                return false;
            }
        }
        return true;
    }

    protected ClassLoader getClassLoader() throws ServletException {
        // Gets custom class loader
        final String classLoaderName = getApplicationOrSystemProperty(
                "ClassLoader", null);
        ClassLoader classLoader;
        if (classLoaderName == null) {
            classLoader = getClass().getClassLoader();
        } else {
            try {
                final Class<?> classLoaderClass = getClass().getClassLoader()
                        .loadClass(classLoaderName);
                final Constructor<?> c = classLoaderClass
                        .getConstructor(new Class[] { ClassLoader.class });
                classLoader = (ClassLoader) c
                        .newInstance(new Object[] { getClass().getClassLoader() });
            } catch (final Exception e) {
                throw new ServletException(
                        "Could not find specified class loader: "
                                + classLoaderName, e);
            }
        }
        return classLoader;
    }

    /**
     * Send a notification to client's application. Used to notify client of
     * critical errors, session expiration and more. Server has no knowledge of
     * what application client refers to.
     * 
     * @param request
     *            the HTTP request instance.
     * @param response
     *            the HTTP response to write to.
     * @param caption
     *            the notification caption
     * @param message
     *            to notification body
     * @param details
     *            a detail message to show in addition to the message. Currently
     *            shown directly below the message but could be hidden behind a
     *            details drop down in the future. Mainly used to give
     *            additional information not necessarily useful to the end user.
     * @param url
     *            url to load when the message is dismissed. Null will reload
     *            the current page.
     * @throws IOException
     *             if the writing failed due to input/output error.
     */
    protected void criticalNotification(WrappedHttpServletRequest request,
            HttpServletResponse response, String caption, String message,
            String details, String url) throws IOException {

        if (ServletPortletHelper.isUIDLRequest(request)) {

            if (caption != null) {
                caption = "\"" + JsonPaintTarget.escapeJSON(caption) + "\"";
            }
            if (details != null) {
                if (message == null) {
                    message = details;
                } else {
                    message += "<br/><br/>" + details;
                }
            }

            if (message != null) {
                message = "\"" + JsonPaintTarget.escapeJSON(message) + "\"";
            }
            if (url != null) {
                url = "\"" + JsonPaintTarget.escapeJSON(url) + "\"";
            }

            String output = "for(;;);[{\"changes\":[], \"meta\" : {"
                    + "\"appError\": {" + "\"caption\":" + caption + ","
                    + "\"message\" : " + message + "," + "\"url\" : " + url
                    + "}}, \"resources\": {}, \"locales\":[]}]";
            writeResponse(response, "application/json; charset=UTF-8", output);
        } else {
            // Create an HTML reponse with the error
            String output = "";

            if (url != null) {
                output += "<a href=\"" + url + "\">";
            }
            if (caption != null) {
                output += "<b>" + caption + "</b><br/>";
            }
            if (message != null) {
                output += message;
                output += "<br/><br/>";
            }

            if (details != null) {
                output += details;
                output += "<br/><br/>";
            }
            if (url != null) {
                output += "</a>";
            }
            writeResponse(response, "text/html; charset=UTF-8", output);

        }

    }

    /**
     * Writes the response in {@code output} using the contentType given in
     * {@code contentType} to the provided {@link HttpServletResponse}
     * 
     * @param response
     * @param contentType
     * @param output
     *            Output to write (UTF-8 encoded)
     * @throws IOException
     */
    private void writeResponse(HttpServletResponse response,
            String contentType, String output) throws IOException {
        response.setContentType(contentType);
        final ServletOutputStream out = response.getOutputStream();
        // Set the response type
        final PrintWriter outWriter = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(out, "UTF-8")));
        outWriter.print(output);
        outWriter.flush();
        outWriter.close();
        out.flush();

    }

    /**
     * Returns the application instance to be used for the request. If an
     * existing instance is not found a new one is created or null is returned
     * to indicate that the application is not available.
     * 
     * @param request
     * @param requestType
     * @return
     * @throws MalformedURLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ServletException
     * @throws SessionExpiredException
     */
    private Application findApplicationInstance(HttpServletRequest request,
            RequestType requestType) throws MalformedURLException,
            ServletException, SessionExpiredException {

        boolean requestCanCreateApplication = requestCanCreateApplication(
                request, requestType);

        /* Find an existing application for this request. */
        Application application = getExistingApplication(request,
                requestCanCreateApplication);

        if (application != null) {
            /*
             * There is an existing application. We can use this as long as the
             * user not specifically requested to close or restart it.
             */

            final boolean restartApplication = (request
                    .getParameter(URL_PARAMETER_RESTART_APPLICATION) != null);
            final boolean closeApplication = (request
                    .getParameter(URL_PARAMETER_CLOSE_APPLICATION) != null);

            if (restartApplication) {
                closeApplication(application, request.getSession(false));
                return createApplication(request);
            } else if (closeApplication) {
                closeApplication(application, request.getSession(false));
                return null;
            } else {
                return application;
            }
        }

        // No existing application was found

        if (requestCanCreateApplication) {
            /*
             * If the request is such that it should create a new application if
             * one as not found, we do that.
             */
            return createApplication(request);
        } else {
            /*
             * The application was not found and a new one should not be
             * created. Assume the session has expired.
             */
            throw new SessionExpiredException();
        }

    }

    /**
     * Check if the request should create an application if an existing
     * application is not found.
     * 
     * @param request
     * @param requestType
     * @return true if an application should be created, false otherwise
     */
    boolean requestCanCreateApplication(HttpServletRequest request,
            RequestType requestType) {
        if (requestType == RequestType.UIDL && isRepaintAll(request)) {
            /*
             * UIDL request contains valid repaintAll=1 event, the user probably
             * wants to initiate a new application through a custom index.html
             * without using the bootstrap page.
             */
            return true;

        } else if (requestType == RequestType.OTHER) {
            /*
             * I.e URIs that are not application resources or static (theme)
             * files.
             */
            return true;

        }

        return false;
    }

    /**
     * Gets resource path using different implementations. Required to
     * supporting different servlet container implementations (application
     * servers).
     * 
     * @param servletContext
     * @param path
     *            the resource path.
     * @return the resource path.
     */
    protected static String getResourcePath(ServletContext servletContext,
            String path) {
        String resultPath = null;
        resultPath = servletContext.getRealPath(path);
        if (resultPath != null) {
            return resultPath;
        } else {
            try {
                final URL url = servletContext.getResource(path);
                resultPath = url.getFile();
            } catch (final Exception e) {
                // FIXME: Handle exception
                getLogger().log(Level.INFO,
                        "Could not find resource path " + path, e);
            }
        }
        return resultPath;
    }

    /**
     * Creates a new application and registers it into WebApplicationContext
     * (aka session). This is not meant to be overridden. Override
     * getNewApplication to create the application instance in a custom way.
     * 
     * @param request
     * @return
     * @throws ServletException
     * @throws MalformedURLException
     */
    private Application createApplication(HttpServletRequest request)
            throws ServletException, MalformedURLException {
        Application newApplication = getNewApplication(request);

        final WebApplicationContext context = getApplicationContext(request
                .getSession());
        context.addApplication(newApplication);

        return newApplication;
    }

    private void handleServiceException(WrappedHttpServletRequest request,
            WrappedHttpServletResponse response, Application application,
            Throwable e) throws IOException, ServletException {
        // if this was an UIDL request, response UIDL back to client
        if (getRequestType(request) == RequestType.UIDL) {
            Application.SystemMessages ci = getSystemMessages();
            criticalNotification(request, response,
                    ci.getInternalErrorCaption(), ci.getInternalErrorMessage(),
                    null, ci.getInternalErrorURL());
            if (application != null) {
                application.getErrorHandler()
                        .terminalError(new RequestError(e));
            } else {
                throw new ServletException(e);
            }
        } else {
            // Re-throw other exceptions
            throw new ServletException(e);
        }

    }

    /**
     * A helper method to strip away characters that might somehow be used for
     * XSS attacs. Leaves at least alphanumeric characters intact. Also removes
     * eg. ( and ), so values should be safe in javascript too.
     * 
     * @param themeName
     * @return
     */
    protected static String stripSpecialChars(String themeName) {
        StringBuilder sb = new StringBuilder();
        char[] charArray = themeName.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (!CHAR_BLACKLIST.contains(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final Collection<Character> CHAR_BLACKLIST = new HashSet<Character>(
            Arrays.asList(new Character[] { '&', '"', '\'', '<', '>', '(', ')',
                    ';' }));

    /**
     * Returns the default theme. Must never return null.
     * 
     * @return
     */
    public static String getDefaultTheme() {
        return DEFAULT_THEME_NAME;
    }

    void handleServiceSessionExpired(WrappedHttpServletRequest request,
            WrappedHttpServletResponse response) throws IOException,
            ServletException {

        if (isOnUnloadRequest(request)) {
            /*
             * Request was an unload request (e.g. window close event) and the
             * client expects no response if it fails.
             */
            return;
        }

        try {
            Application.SystemMessages ci = getSystemMessages();
            if (getRequestType(request) != RequestType.UIDL) {
                // 'plain' http req - e.g. browser reload;
                // just go ahead redirect the browser
                response.sendRedirect(ci.getSessionExpiredURL());
            } else {
                /*
                 * Invalidate session (weird to have session if we're saying
                 * that it's expired, and worse: portal integration will fail
                 * since the session is not created by the portal.
                 * 
                 * Session must be invalidated before criticalNotification as it
                 * commits the response.
                 */
                request.getSession().invalidate();

                // send uidl redirect
                criticalNotification(request, response,
                        ci.getSessionExpiredCaption(),
                        ci.getSessionExpiredMessage(), null,
                        ci.getSessionExpiredURL());

            }
        } catch (SystemMessageException ee) {
            throw new ServletException(ee);
        }

    }

    private void handleServiceSecurityException(
            WrappedHttpServletRequest request,
            WrappedHttpServletResponse response) throws IOException,
            ServletException {
        if (isOnUnloadRequest(request)) {
            /*
             * Request was an unload request (e.g. window close event) and the
             * client expects no response if it fails.
             */
            return;
        }

        try {
            Application.SystemMessages ci = getSystemMessages();
            if (getRequestType(request) != RequestType.UIDL) {
                // 'plain' http req - e.g. browser reload;
                // just go ahead redirect the browser
                response.sendRedirect(ci.getCommunicationErrorURL());
            } else {
                // send uidl redirect
                criticalNotification(request, response,
                        ci.getCommunicationErrorCaption(),
                        ci.getCommunicationErrorMessage(),
                        INVALID_SECURITY_KEY_MSG, ci.getCommunicationErrorURL());
                /*
                 * Invalidate session. Portal integration will fail otherwise
                 * since the session is not created by the portal.
                 */
                request.getSession().invalidate();
            }
        } catch (SystemMessageException ee) {
            throw new ServletException(ee);
        }

        log("Invalid security key received from " + request.getRemoteHost());
    }

    /**
     * Creates a new application for the given request.
     * 
     * @param request
     *            the HTTP request.
     * @return A new Application instance.
     * @throws ServletException
     */
    protected abstract Application getNewApplication(HttpServletRequest request)
            throws ServletException;

    /**
     * Starts the application if it is not already running.
     * 
     * @param request
     * @param application
     * @param webApplicationContext
     * @throws ServletException
     * @throws MalformedURLException
     */
    private void startApplication(HttpServletRequest request,
            Application application, WebApplicationContext webApplicationContext)
            throws ServletException, MalformedURLException {

        if (!application.isRunning()) {
            // Create application
            final URL applicationUrl = getApplicationUrl(request);

            // Initial locale comes from the request
            Locale locale = request.getLocale();
            application.setLocale(locale);
            application.start(new ApplicationStartEvent(applicationUrl,
                    applicationProperties, webApplicationContext,
                    isProductionMode()));
        }
    }

    /**
     * Check if this is a request for a static resource and, if it is, serve the
     * resource to the client.
     * 
     * @param request
     * @param response
     * @return true if a file was served and the request has been handled, false
     *         otherwise.
     * @throws IOException
     * @throws ServletException
     */
    private boolean serveStaticResources(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {

        // FIXME What does 10 refer to?
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 10) {
            return false;
        }

        if ((request.getContextPath() != null)
                && (request.getRequestURI().startsWith("/VAADIN/"))) {
            serveStaticResourcesInVAADIN(request.getRequestURI(), request,
                    response);
            return true;
        } else if (request.getRequestURI().startsWith(
                request.getContextPath() + "/VAADIN/")) {
            serveStaticResourcesInVAADIN(
                    request.getRequestURI().substring(
                            request.getContextPath().length()), request,
                    response);
            return true;
        }

        return false;
    }

    /**
     * Serve resources from VAADIN directory.
     * 
     * @param filename
     *            The filename to serve. Should always start with /VAADIN/.
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private void serveStaticResourcesInVAADIN(String filename,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        final ServletContext sc = getServletContext();
        URL resourceUrl = sc.getResource(filename);
        if (resourceUrl == null) {
            // try if requested file is found from classloader

            // strip leading "/" otherwise stream from JAR wont work
            filename = filename.substring(1);
            resourceUrl = getClassLoader().getResource(filename);

            if (resourceUrl == null) {
                // cannot serve requested file
                getLogger()
                        .info("Requested resource ["
                                + filename
                                + "] not found from filesystem or through class loader."
                                + " Add widgetset and/or theme JAR to your classpath or add files to WebContent/VAADIN folder.");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // security check: do not permit navigation out of the VAADIN
            // directory
            if (!isAllowedVAADINResourceUrl(request, resourceUrl)) {
                getLogger()
                        .info("Requested resource ["
                                + filename
                                + "] not accessible in the VAADIN directory or access to it is forbidden.");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // Find the modification timestamp
        long lastModifiedTime = 0;
        URLConnection connection = null;
        try {
            connection = resourceUrl.openConnection();
            lastModifiedTime = connection.getLastModified();
            // Remove milliseconds to avoid comparison problems (milliseconds
            // are not returned by the browser in the "If-Modified-Since"
            // header).
            lastModifiedTime = lastModifiedTime - lastModifiedTime % 1000;

            if (browserHasNewestVersion(request, lastModifiedTime)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        } catch (Exception e) {
            // Failed to find out last modified timestamp. Continue without it.
            getLogger()
                    .log(Level.FINEST,
                            "Failed to find out last modified timestamp. Continuing without it.",
                            e);
        } finally {
            if (connection instanceof URLConnection) {
                try {
                    // Explicitly close the input stream to prevent it
                    // from remaining hanging
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700
                    InputStream is = connection.getInputStream();
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    getLogger().log(Level.INFO,
                            "Error closing URLConnection input stream", e);
                }
            }
        }

        // Set type mime type if we can determine it based on the filename
        final String mimetype = sc.getMimeType(filename);
        if (mimetype != null) {
            response.setContentType(mimetype);
        }

        // Provide modification timestamp to the browser if it is known.
        if (lastModifiedTime > 0) {
            response.setDateHeader("Last-Modified", lastModifiedTime);
            /*
             * The browser is allowed to cache for 1 hour without checking if
             * the file has changed. This forces browsers to fetch a new version
             * when the Vaadin version is updated. This will cause more requests
             * to the servlet than without this but for high volume sites the
             * static files should never be served through the servlet. The
             * cache timeout can be configured by setting the resourceCacheTime
             * parameter in web.xml
             */
            response.setHeader("Cache-Control",
                    "max-age= " + String.valueOf(resourceCacheTime));
        }

        // Write the resource to the client.
        final OutputStream os = response.getOutputStream();
        final byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        InputStream is = resourceUrl.openStream();
        while ((bytes = is.read(buffer)) >= 0) {
            os.write(buffer, 0, bytes);
        }
        is.close();
    }

    /**
     * Check whether a URL obtained from a classloader refers to a valid static
     * resource in the directory VAADIN.
     * 
     * Warning: Overriding of this method is not recommended, but is possible to
     * support non-default classloaders or servers that may produce URLs
     * different from the normal ones. The method prototype may change in the
     * future. Care should be taken not to expose class files or other resources
     * outside the VAADIN directory if the method is overridden.
     * 
     * @param request
     * @param resourceUrl
     * @return
     * 
     * @since 6.6.7
     */
    protected boolean isAllowedVAADINResourceUrl(HttpServletRequest request,
            URL resourceUrl) {
        if ("jar".equals(resourceUrl.getProtocol())) {
            // This branch is used for accessing resources directly from the
            // Vaadin JAR in development environments and in similar cases.

            // Inside a JAR, a ".." would mean a real directory named ".." so
            // using it in paths should just result in the file not being found.
            // However, performing a check in case some servers or class loaders
            // try to normalize the path by collapsing ".." before the class
            // loader sees it.

            if (!resourceUrl.getPath().contains("!/VAADIN/")) {
                getLogger().info(
                        "Blocked attempt to access a JAR entry not starting with /VAADIN/: "
                                + resourceUrl);
                return false;
            }
            getLogger().fine(
                    "Accepted access to a JAR entry using a class loader: "
                            + resourceUrl);
            return true;
        } else {
            // Some servers such as GlassFish extract files from JARs (file:)
            // and e.g. JBoss 5+ use protocols vsf: and vfsfile: .

            // Check that the URL is in a VAADIN directory and does not contain
            // "/../"
            if (!resourceUrl.getPath().contains("/VAADIN/")
                    || resourceUrl.getPath().contains("/../")) {
                getLogger().info(
                        "Blocked attempt to access file: " + resourceUrl);
                return false;
            }
            getLogger().fine(
                    "Accepted access to a file using a class loader: "
                            + resourceUrl);
            return true;
        }
    }

    /**
     * Checks if the browser has an up to date cached version of requested
     * resource. Currently the check is performed using the "If-Modified-Since"
     * header. Could be expanded if needed.
     * 
     * @param request
     *            The HttpServletRequest from the browser.
     * @param resourceLastModifiedTimestamp
     *            The timestamp when the resource was last modified. 0 if the
     *            last modification time is unknown.
     * @return true if the If-Modified-Since header tells the cached version in
     *         the browser is up to date, false otherwise
     */
    private boolean browserHasNewestVersion(HttpServletRequest request,
            long resourceLastModifiedTimestamp) {
        if (resourceLastModifiedTimestamp < 1) {
            // We do not know when it was modified so the browser cannot have an
            // up-to-date version
            return false;
        }
        /*
         * The browser can request the resource conditionally using an
         * If-Modified-Since header. Check this against the last modification
         * time.
         */
        try {
            // If-Modified-Since represents the timestamp of the version cached
            // in the browser
            long headerIfModifiedSince = request
                    .getDateHeader("If-Modified-Since");

            if (headerIfModifiedSince >= resourceLastModifiedTimestamp) {
                // Browser has this an up-to-date version of the resource
                return true;
            }
        } catch (Exception e) {
            // Failed to parse header. Fail silently - the browser does not have
            // an up-to-date version in its cache.
        }
        return false;
    }

    protected enum RequestType {
        FILE_UPLOAD, BROWSER_DETAILS, UIDL, OTHER, STATIC_FILE, APPLICATION_RESOURCE, CONNECTOR_RESOURCE;
    }

    protected RequestType getRequestType(WrappedHttpServletRequest request) {
        if (ServletPortletHelper.isFileUploadRequest(request)) {
            return RequestType.FILE_UPLOAD;
        } else if (ServletPortletHelper.isConnectorResourceRequest(request)) {
            return RequestType.CONNECTOR_RESOURCE;
        } else if (isBrowserDetailsRequest(request)) {
            return RequestType.BROWSER_DETAILS;
        } else if (ServletPortletHelper.isUIDLRequest(request)) {
            return RequestType.UIDL;
        } else if (isStaticResourceRequest(request)) {
            return RequestType.STATIC_FILE;
        } else if (ServletPortletHelper.isApplicationResourceRequest(request)) {
            return RequestType.APPLICATION_RESOURCE;
        }
        return RequestType.OTHER;

    }

    private static boolean isBrowserDetailsRequest(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && request.getParameter("browserDetails") != null;
    }

    private boolean isStaticResourceRequest(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 10) {
            return false;
        }

        if ((request.getContextPath() != null)
                && (request.getRequestURI().startsWith("/VAADIN/"))) {
            return true;
        } else if (request.getRequestURI().startsWith(
                request.getContextPath() + "/VAADIN/")) {
            return true;
        }

        return false;
    }

    private boolean isOnUnloadRequest(HttpServletRequest request) {
        return request.getParameter(ApplicationConnection.PARAM_UNLOADBURST) != null;
    }

    /**
     * Get system messages from the current application class
     * 
     * @return
     */
    protected SystemMessages getSystemMessages() {
        Class<? extends Application> appCls = null;
        try {
            appCls = getApplicationClass();
        } catch (ClassNotFoundException e) {
            // Previous comment claimed that this should never happen
            throw new SystemMessageException(e);
        }
        return getSystemMessages(appCls);
    }

    public static SystemMessages getSystemMessages(
            Class<? extends Application> appCls) {
        try {
            if (appCls != null) {
                Method m = appCls
                        .getMethod("getSystemMessages", (Class[]) null);
                return (Application.SystemMessages) m.invoke(null,
                        (Object[]) null);
            }
        } catch (SecurityException e) {
            throw new SystemMessageException(
                    "Application.getSystemMessage() should be static public", e);
        } catch (NoSuchMethodException e) {
            // This is completely ok and should be silently ignored
        } catch (IllegalArgumentException e) {
            // This should never happen
            throw new SystemMessageException(e);
        } catch (IllegalAccessException e) {
            throw new SystemMessageException(
                    "Application.getSystemMessage() should be static public", e);
        } catch (InvocationTargetException e) {
            // This should never happen
            throw new SystemMessageException(e);
        }
        return Application.getSystemMessages();
    }

    protected abstract Class<? extends Application> getApplicationClass()
            throws ClassNotFoundException;

    /**
     * Return the URL from where static files, e.g. the widgetset and the theme,
     * are served. In a standard configuration the VAADIN folder inside the
     * returned folder is what is used for widgetsets and themes.
     * 
     * The returned folder is usually the same as the context path and
     * independent of the application.
     * 
     * @param request
     * @return The location of static resources (should contain the VAADIN
     *         directory). Never ends with a slash (/).
     */
    protected String getStaticFilesLocation(HttpServletRequest request) {

        return getWebApplicationsStaticFileLocation(request);
    }

    /**
     * The default method to fetch static files location (URL). This method does
     * not check for request attribute {@value #REQUEST_VAADIN_STATIC_FILE_PATH}
     * 
     * @param request
     * @return
     */
    private String getWebApplicationsStaticFileLocation(
            HttpServletRequest request) {
        String staticFileLocation;
        // if property is defined in configurations, use that
        staticFileLocation = getApplicationOrSystemProperty(
                PARAMETER_VAADIN_RESOURCES, null);
        if (staticFileLocation != null) {
            return staticFileLocation;
        }

        // the last (but most common) option is to generate default location
        // from request

        // if context is specified add it to widgetsetUrl
        String ctxPath = request.getContextPath();

        // FIXME: ctxPath.length() == 0 condition is probably unnecessary and
        // might even be wrong.

        if (ctxPath.length() == 0
                && request.getAttribute("javax.servlet.include.context_path") != null) {
            // include request (e.g portlet), get context path from
            // attribute
            ctxPath = (String) request
                    .getAttribute("javax.servlet.include.context_path");
        }

        // Remove heading and trailing slashes from the context path
        ctxPath = removeHeadingOrTrailing(ctxPath, "/");

        if (ctxPath.equals("")) {
            return "";
        } else {
            return "/" + ctxPath;
        }
    }

    /**
     * Remove any heading or trailing "what" from the "string".
     * 
     * @param string
     * @param what
     * @return
     */
    private static String removeHeadingOrTrailing(String string, String what) {
        while (string.startsWith(what)) {
            string = string.substring(1);
        }

        while (string.endsWith(what)) {
            string = string.substring(0, string.length() - 1);
        }

        return string;
    }

    /**
     * Write a redirect response to the main page of the application.
     * 
     * @param request
     * @param response
     * @throws IOException
     *             if sending the redirect fails due to an input/output error or
     *             a bad application URL
     */
    private void redirectToApplication(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String applicationUrl = getApplicationUrl(request).toExternalForm();
        response.sendRedirect(response.encodeRedirectURL(applicationUrl));
    }

    /**
     * Gets the current application URL from request.
     * 
     * @param request
     *            the HTTP request.
     * @throws MalformedURLException
     *             if the application is denied access to the persistent data
     *             store represented by the given URL.
     */
    protected URL getApplicationUrl(HttpServletRequest request)
            throws MalformedURLException {
        final URL reqURL = new URL(
                (request.isSecure() ? "https://" : "http://")
                        + request.getServerName()
                        + ((request.isSecure() && request.getServerPort() == 443)
                                || (!request.isSecure() && request
                                        .getServerPort() == 80) ? "" : ":"
                                + request.getServerPort())
                        + request.getRequestURI());
        String servletPath = "";
        if (request.getAttribute("javax.servlet.include.servlet_path") != null) {
            // this is an include request
            servletPath = request.getAttribute(
                    "javax.servlet.include.context_path").toString()
                    + request
                            .getAttribute("javax.servlet.include.servlet_path");

        } else {
            servletPath = request.getContextPath() + request.getServletPath();
        }

        if (servletPath.length() == 0
                || servletPath.charAt(servletPath.length() - 1) != '/') {
            servletPath = servletPath + "/";
        }
        URL u = new URL(reqURL, servletPath);
        return u;
    }

    /**
     * Gets the existing application for given request. Looks for application
     * instance for given request based on the requested URL.
     * 
     * @param request
     *            the HTTP request.
     * @param allowSessionCreation
     *            true if a session should be created if no session exists,
     *            false if no session should be created
     * @return Application instance, or null if the URL does not map to valid
     *         application.
     * @throws MalformedURLException
     *             if the application is denied access to the persistent data
     *             store represented by the given URL.
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SessionExpiredException
     */
    protected Application getExistingApplication(HttpServletRequest request,
            boolean allowSessionCreation) throws MalformedURLException,
            SessionExpiredException {

        // Ensures that the session is still valid
        final HttpSession session = request.getSession(allowSessionCreation);
        if (session == null) {
            throw new SessionExpiredException();
        }

        WebApplicationContext context = getApplicationContext(session);

        // Gets application list for the session.
        final Collection<Application> applications = context.getApplications();

        // Search for the application (using the application URI) from the list
        for (final Iterator<Application> i = applications.iterator(); i
                .hasNext();) {
            final Application sessionApplication = i.next();
            final String sessionApplicationPath = sessionApplication.getURL()
                    .getPath();
            String requestApplicationPath = getApplicationUrl(request)
                    .getPath();

            if (requestApplicationPath.equals(sessionApplicationPath)) {
                // Found a running application
                if (sessionApplication.isRunning()) {
                    return sessionApplication;
                }
                // Application has stopped, so remove it before creating a new
                // application
                getApplicationContext(session).removeApplication(
                        sessionApplication);
                break;
            }
        }

        // Existing application not found
        return null;
    }

    /**
     * Ends the application.
     * 
     * @param request
     *            the HTTP request.
     * @param response
     *            the HTTP response to write to.
     * @param application
     *            the application to end.
     * @throws IOException
     *             if the writing failed due to input/output error.
     */
    private void endApplication(HttpServletRequest request,
            HttpServletResponse response, Application application)
            throws IOException {

        String logoutUrl = application.getLogoutURL();
        if (logoutUrl == null) {
            logoutUrl = application.getURL().toString();
        }

        final HttpSession session = request.getSession();
        if (session != null) {
            getApplicationContext(session).removeApplication(application);
        }

        response.sendRedirect(response.encodeRedirectURL(logoutUrl));
    }

    /**
     * Returns the path info; note that this _can_ be different than
     * request.getPathInfo(). Examples where this might be useful:
     * <ul>
     * <li>An application runner servlet that runs different Vaadin applications
     * based on an identifier.</li>
     * <li>Providing a REST interface in the context root, while serving a
     * Vaadin UI on a sub-URI using only one servlet (e.g. REST on
     * http://example.com/foo, UI on http://example.com/foo/vaadin)</li>
     * 
     * @param request
     * @return
     */
    protected String getRequestPathInfo(HttpServletRequest request) {
        return request.getPathInfo();
    }

    /**
     * Gets relative location of a theme resource.
     * 
     * @param theme
     *            the Theme name.
     * @param resource
     *            the Theme resource.
     * @return External URI specifying the resource
     */
    public String getResourceLocation(String theme, ThemeResource resource) {

        if (resourcePath == null) {
            return resource.getResourceId();
        }
        return resourcePath + theme + "/" + resource.getResourceId();
    }

    private boolean isRepaintAll(HttpServletRequest request) {
        return (request.getParameter(URL_PARAMETER_REPAINT_ALL) != null)
                && (request.getParameter(URL_PARAMETER_REPAINT_ALL).equals("1"));
    }

    private void closeApplication(Application application, HttpSession session) {
        if (application == null) {
            return;
        }

        application.close();
        if (session != null) {
            WebApplicationContext context = getApplicationContext(session);
            context.removeApplication(application);
        }
    }

    /**
     * 
     * Gets the application context from an HttpSession. If no context is
     * currently stored in a session a new context is created and stored in the
     * session.
     * 
     * @param session
     *            the HTTP session.
     * @return the application context for HttpSession.
     */
    protected WebApplicationContext getApplicationContext(HttpSession session) {
        /*
         * TODO the ApplicationContext.getApplicationContext() should be removed
         * and logic moved here. Now overriding context type is possible, but
         * the whole creation logic should be here. MT 1101
         */
        return WebApplicationContext.getApplicationContext(session);
    }

    public class RequestError implements Terminal.ErrorEvent, Serializable {

        private final Throwable throwable;

        public RequestError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public Throwable getThrowable() {
            return throwable;
        }

    }

    /**
     * Override this method if you need to use a specialized communicaiton
     * mananger implementation.
     * 
     * @deprecated Instead of overriding this method, override
     *             {@link WebApplicationContext} implementation via
     *             {@link AbstractApplicationServlet#getApplicationContext(HttpSession)}
     *             method and in that customized implementation return your
     *             CommunicationManager in
     *             {@link WebApplicationContext#getApplicationManager(Application, AbstractApplicationServlet)}
     *             method.
     * 
     * @param application
     * @return
     */
    @Deprecated
    public CommunicationManager createCommunicationManager(
            Application application) {
        return new CommunicationManager(application);
    }

    /**
     * Escapes characters to html entities. An exception is made for some
     * "safe characters" to keep the text somewhat readable.
     * 
     * @param unsafe
     * @return a safe string to be added inside an html tag
     */
    public static final String safeEscapeForHtml(String unsafe) {
        if (null == unsafe) {
            return null;
        }
        StringBuilder safe = new StringBuilder();
        char[] charArray = unsafe.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (isSafe(c)) {
                safe.append(c);
            } else {
                safe.append("&#");
                safe.append((int) c);
                safe.append(";");
            }
        }

        return safe.toString();
    }

    private static boolean isSafe(char c) {
        return //
        c > 47 && c < 58 || // alphanum
                c > 64 && c < 91 || // A-Z
                c > 96 && c < 123 // a-z
        ;
    }

    private static final Logger getLogger() {
        return Logger.getLogger(AbstractApplicationServlet.class.getName());
    }
}
