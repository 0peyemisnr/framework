/*
 * Copyright 2000-2013 Vaadind Ltd.
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

package com.vaadin.tests.tb3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariDriver;

import com.vaadin.testbench.TestBench;

/**
 * Provides values for parameters which depend on where the test is run.
 * Parameters should be configured in work/eclipse-run-selected-test.properties.
 * A template is available in uitest/.
 * 
 * @author Vaadin Ltd
 */
public abstract class PrivateTB3Configuration extends ScreenshotTB3Test {
    private static final String HOSTNAME_PROPERTY = "com.vaadin.testbench.deployment.hostname";
    private static final String PORT_PROPERTY = "com.vaadin.testbench.deployment.port";
    private final Properties properties = new Properties();

    public PrivateTB3Configuration() {
        File file = new File("work", "eclipse-run-selected-test.properties");
        if (file.exists()) {
            try {
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getProperty(String name) {
        String property = properties.getProperty(name);
        if (property == null) {
            property = System.getProperty(name);
        }

        return property;
    }

    @Override
    protected String getScreenshotDirectory() {
        String screenshotDirectory = getProperty("com.vaadin.testbench.screenshot.directory");
        if (screenshotDirectory == null) {
            throw new RuntimeException(
                    "No screenshot directory defined. Use -Dcom.vaadin.testbench.screenshot.directory=<path>");
        }
        return screenshotDirectory;
    }

    @Override
    protected String getHubHostname() {
        return "tb3-hub.intra.itmill.com";
    }

    @Override
    protected String getDeploymentHostname() {
        if (getClass().getAnnotation(RunLocally.class) != null) {
            return "localhost";
        }
        String hostName = getProperty(HOSTNAME_PROPERTY);

        if (hostName == null || "".equals(hostName)) {
            hostName = findAutoHostname();
        }

        return hostName;
    }

    @Override
    protected int getDeploymentPort() {
        String portString = getProperty(PORT_PROPERTY);

        int port = 8888;
        if (portString != null && !"".equals(portString)) {
            port = Integer.parseInt(portString);
        }

        return port;
    }

    /**
     * Tries to automatically determine the IP address of the machine the test
     * is running on.
     * 
     * @return An IP address of one of the network interfaces in the machine.
     * @throws RuntimeException
     *             if there was an error or no IP was found
     */
    private String findAutoHostname() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nwInterface = interfaces.nextElement();
                if (!nwInterface.isUp() || nwInterface.isLoopback()
                        || nwInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = nwInterface
                        .getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }
                    if (address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Could not enumerate ");
        }

        throw new RuntimeException(
                "No compatible (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) ip address found.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.tests.tb3.AbstractTB3Test#setupLocalDriver()
     */
    @Override
    protected void setupLocalDriver(DesiredCapabilities desiredCapabilities) {
        WebDriver driver;
        if (BrowserUtil.isFirefox(desiredCapabilities)) {
            String firefoxPath = getProperty("firefox.path");
            if (firefoxPath != null) {
                driver = new FirefoxDriver(new FirefoxBinary(new File(
                        firefoxPath)), null);
            } else {
                driver = new FirefoxDriver();
            }
        } else if (BrowserUtil.isChrome(desiredCapabilities)) {
            System.setProperty("webdriver.chrome.driver",
                    getProperty("chrome.driver.path"));
            driver = new ChromeDriver();
        } else if (BrowserUtil.isSafari(desiredCapabilities)) {
            driver = new SafariDriver();
        } else {
            throw new RuntimeException(
                    "Not implemented support for running locally on "
                            + BrowserUtil
                                    .getBrowserIdentifier(desiredCapabilities));
        }
        setDriver(TestBench.createDriver(driver));
        setDesiredCapabilities(desiredCapabilities);
    }
}
