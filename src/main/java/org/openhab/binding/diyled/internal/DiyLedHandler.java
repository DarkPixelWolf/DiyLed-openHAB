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

import static org.openhab.binding.diyled.internal.DiyLedBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link DiyLedHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sebastian Scheibe - Initial contribution
 */
@NonNullByDefault
public class DiyLedHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(DiyLedHandler.class);

    private @Nullable DiyLedConfiguration config;

    private static final int DIM_STEP = 25;
    private int currentBrightness;

    private final int DEFAULT_REFRESH_INTERVAL = 10;

    private DiyLedHTTP httpHandler;
    private static final int DEFAULT_REFRESH_INITIAL_DELAY = 15;

    public DiyLedHandler(Thing thing, DiyLedHTTP httpHandler) {
        super(thing);
        this.httpHandler = httpHandler;
    }

    private @Nullable ScheduledFuture<?> refreshJob;

    private final Runnable refreshRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                updateChannels();
            } catch (Exception e) {
                logger.debug("ERROR: ");
                logger.debug(e.getMessage());
            }
        }
    };

    private void updateChannels() {
        try {
            logger.debug("IP: " + getThing().getConfiguration().get("IP"));
            String json = "{\"id\": \"infoRequestPacket\", \"data\": {\"request\": \"light\", \"name\": \""
                    + getThing().getConfiguration().get("Name") + "\"}}";

            String response = httpHandler.getHTTPRequest(json, getThing().getConfiguration().get("IP").toString());

            logger.debug("HTTP Response:");
            logger.debug(response.toString());

            JsonObject data = new JsonParser().parse(response.toString()).getAsJsonObject().get("data")
                    .getAsJsonObject();
            logger.debug("JsonObject");
            currentBrightness = data.get("brightness").getAsInt();
            logger.debug("CB: " + currentBrightness);
            logger.debug("P: " + data.get("power").getAsString());
            updateState(CHANNEL_POWER,
                    data.get("power").getAsString().equalsIgnoreCase("true") ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_BRIGHTNESS,
                    data.get("power").getAsString().equalsIgnoreCase("true") ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_BRIGHTNESS, new PercentType(data.get("brightness").getAsInt() * 100 / 255));
        } catch (Exception e) {
            logger.debug("ERROR: ");
            logger.debug(e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handling command: " + command.toString());
        logger.debug(channelUID.getAsString());
        if (command instanceof RefreshType) {
            updateChannels();
        } else {
            String json = "";
            switch (channelUID.getId()) {
                case CHANNEL_POWER:
                    json = "{\"id\": \"changeValueRequestPacket\", \"data\": {\"request\": \"light\", \"name\": \""
                            + getThing().getConfiguration().get("Name") + "\", \"key\": \"power\", \"value\": \""
                            + (command.toString().equalsIgnoreCase("ON") ? "true" : "false")
                            + "\", \"id\": 123456789}}";
                    break;
                case CHANNEL_BRIGHTNESS:
                    if (command instanceof PercentType) {
                        currentBrightness = ((PercentType) command).intValue() * 255 / 100;
                        json = "{\"id\": \"changeValueRequestPacket\", \"data\": {\"request\": \"light\", \"name\": \""
                                + getThing().getConfiguration().get("Name")
                                + "\", \"key\": \"brightness\", \"value\": \"" + currentBrightness
                                + "\", \"id\": 123456789}}";
                    } else if (command instanceof OnOffType) {
                        json = "{\"id\": \"changeValueRequestPacket\", \"data\": {\"request\": \"light\", \"name\": \""
                                + getThing().getConfiguration().get("Name") + "\", \"key\": \"power\", \"value\": \""
                                + (command.toString().equalsIgnoreCase("ON") ? "true" : "false")
                                + "\", \"id\": 123456789}}";
                    } else if (command instanceof IncreaseDecreaseType) {
                        switch (command.toString()) {
                            case "INCREASE":
                                currentBrightness = currentBrightness + DIM_STEP;
                                if (currentBrightness > 255) {
                                    currentBrightness = 255;
                                }
                                break;
                            case "DECREASE":
                                currentBrightness = currentBrightness - DIM_STEP;
                                if (currentBrightness < 255) {
                                    currentBrightness = 0;
                                }
                                break;
                        }
                        json = "{\"id\": \"changeValueRequestPacket\", \"data\": {\"request\": \"light\", \"name\": \""
                                + getThing().getConfiguration().get("Name")
                                + "\", \"key\": \"brightness\", \"value\": \"" + currentBrightness
                                + "\", \"id\": 123456789}}";
                    }
                    break;
            }
            httpHandler.sendHTTPRequest(json, getThing().getConfiguration().get("IP").toString());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(DiyLedConfiguration.class);

        updateStatus(ThingStatus.ONLINE);

        refreshJob = scheduler.scheduleWithFixedDelay(refreshRunnable, DEFAULT_REFRESH_INITIAL_DELAY,
                DEFAULT_REFRESH_INTERVAL, TimeUnit.SECONDS);

        logger.debug("Finished initializing!");
    }

    @Override
    public void dispose() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }
}
