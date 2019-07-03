/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.diyled.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DiyLedHTTP} is responsible for handling http requests
 *
 * @author Sebastian Scheibe - Initial contribution
 */
public class DiyLedHTTP {

    private final Logger logger = LoggerFactory.getLogger(DiyLedHTTP.class);

    public void sendHTTPRequest(String request, String ip) {
        logger.debug("Sending HTTP Request: " + request);
        try {
            Properties header = new Properties();
            header.put("Content-Type", "application/json");
            InputStream content = new ByteArrayInputStream(request.getBytes("utf-8"));
            HttpUtil.executeUrl("PUT", "http://" + ip + ":80/diyledinfo", header, content, null, 2000);

            logger.debug("Sending successfull");
        } catch (Exception e) {
            logger.debug("ERROR: ");
            logger.debug(e.getMessage());
        }
    }

    public String getHTTPRequest(String request, String ip) {
        try {
            Properties header = new Properties();
            header.put("Content-Type", "application/json");
            InputStream content = new ByteArrayInputStream(request.getBytes("utf-8"));
            return HttpUtil.executeUrl("PUT", "http://" + ip + ":80/diyledinfo", header, content, null, 2000);
        } catch (Exception e) {
            logger.debug("ERROR: ");
            logger.debug(e.getMessage());
            return "";
        }
    }

}
