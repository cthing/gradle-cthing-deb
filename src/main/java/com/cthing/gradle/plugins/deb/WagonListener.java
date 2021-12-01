/*
 * Copyright 2021 C Thing Software
 * All rights reserved.
 */
package com.cthing.gradle.plugins.deb;

import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Writes Wagon session and transfer related information to the Gradle log.
 */
class WagonListener implements SessionListener, TransferListener {

    private static final Logger LOG = LoggerFactory.getLogger(WagonListener.class);

    @Override
    public void sessionOpening(final SessionEvent sessionEvent) {
        LOG.debug("Session opening: {}", sessionEvent);
    }

    @Override
    public void sessionOpened(final SessionEvent sessionEvent) {
        LOG.debug("Session opened: {}", sessionEvent);
    }

    @Override
    public void sessionDisconnecting(final SessionEvent sessionEvent) {
        LOG.debug("Session disconnecting: {}", sessionEvent);
    }

    @Override
    public void sessionDisconnected(final SessionEvent sessionEvent) {
        LOG.debug("Session disconnected: {}", sessionEvent);
    }

    @Override
    public void sessionConnectionRefused(final SessionEvent sessionEvent) {
        LOG.debug("Session connection refused: {}", sessionEvent);
    }

    @Override
    public void sessionLoggedIn(final SessionEvent sessionEvent) {
        LOG.debug("Session logged in: {}", sessionEvent);
    }

    @Override
    public void sessionLoggedOff(final SessionEvent sessionEvent) {
        LOG.debug("Session logged off: {}", sessionEvent);
    }

    @Override
    public void sessionError(final SessionEvent sessionEvent) {
        LOG.error("Session error: {}", sessionEvent);
    }

    @Override
    public void transferInitiated(final TransferEvent transferEvent) {
        LOG.debug("Transfer initiated: {}", transferEvent);
    }

    @Override
    public void transferStarted(final TransferEvent transferEvent) {
        LOG.debug("Transfer started: {}", transferEvent);
    }

    @Override
    public void transferProgress(final TransferEvent transferEvent, final byte[] bytes, final int length) {
        LOG.debug("Transfer progress: {}", transferEvent);
    }

    @Override
    public void transferCompleted(final TransferEvent transferEvent) {
        LOG.debug("Transfer completed: {}", transferEvent);
    }

    @Override
    public void transferError(final TransferEvent transferEvent) {
        LOG.error("Transfer error: {}", transferEvent);
    }

    @Override
    public void debug(final String message) {
        LOG.debug("Debug output: {}", message);
    }
}
