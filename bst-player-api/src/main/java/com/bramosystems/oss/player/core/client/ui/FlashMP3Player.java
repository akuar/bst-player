/*
 * Copyright 2009 Sikirulai Braheem
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bramosystems.oss.player.core.client.ui;

import com.bramosystems.oss.player.core.client.LoadException;
import com.bramosystems.oss.player.core.client.PluginVersion;
import com.bramosystems.oss.player.core.client.MediaInfo;
import com.bramosystems.oss.player.core.client.PluginVersionException;
import com.bramosystems.oss.player.core.client.PlayException;
import com.bramosystems.oss.player.core.client.MediaStateListener;
import com.bramosystems.oss.player.core.client.MediaStateListenerAdapter;
import com.bramosystems.oss.player.core.client.PluginNotFoundException;
import com.bramosystems.oss.player.core.client.AbstractMediaPlayer;
import com.bramosystems.oss.player.core.client.impl.FlashMP3PlayerImpl;
import com.bramosystems.oss.player.core.client.skin.FlatCustomControl;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;

/**
 * Widget to embed MP3 media using Flash Plugin
 *
 * <h3>Usage Example</h3>
 *
 * <p>
 * <code><pre>
 * SimplePanel panel = new SimplePanel();   // create panel to hold the player
 * Widget player = null;
 * try {
 *      // create the player
 *      player = new FlashMP3Player("www.example.com/mediafile.mp3");
 * } catch(LoadException e) {
 *      // catch loading exception and alert user
 *      Window.alert("An error occured while loading");
 * } catch(PluginVersionException e) {
 *      // catch plugin version exception and alert user to download plugin first.
 *      // An option is to use the utility method in PlayerUtil class.
 *      player = PlayerUtil.getMissingPluginNotice(Plugin.FlashMP3Player, "Missing Plugin",
 *              ".. some nice message telling the user to click and download plugin first ..",
 *              false);
 * } catch(PluginNotFoundException e) {
 *      // catch PluginNotFoundException and tell user to download plugin, possibly providing
 *      // a link to the plugin download page.
 *      player = new HTML(".. another kind of message telling the user to download plugin..");
 * }
 *
 * panel.setWidget(player); // add player to panel.
 * </pre></code>
 *
 * @author Sikirulai Braheem
 */
public class FlashMP3Player extends AbstractMediaPlayer {

    private static FlashMP3PlayerImpl impl = new FlashMP3PlayerImpl();
    private String playerId;
    private boolean isEmbedded;
    private Logger logger;
    private MediaStateListener _onInitLoopCountListener,  _onInitListListener,  _onInitSuffleListener;

    /**
     * Constructs <code>FlashMP3Player</code> with the specified {@code height} and
     * {@code width} to playback media located at {@code mediaURL}. Media playback
     * begins automatically if {@code autoplay} is {@code true}.
     *
     * <p> {@code height} and {@code width} are specified as CSS units. A value of {@code null}
     * for {@code height} or {@code width} puts the player in embedded mode.  When in embedded mode,
     * the player is made invisible on the page and media state events are never handled by this player
     * but rather propagated to registered listeners only.  This is desired especially when used
     * with custom controls.
     *
     * @param mediaURL the URL of the media to playback
     * @param autoplay {@code true} to start playing automatically, {@code false} otherwise
     * @param height the height of the player
     * @param width the width of the player.
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Flash plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Flash plugin is not installed on the client.
     */
    public FlashMP3Player(String mediaURL, boolean autoplay, String height, String width)
            throws PluginNotFoundException, PluginVersionException, LoadException {

        SWFWidget swf = new SWFWidget(GWT.getModuleBaseURL() + "mp3-player-1.0-SNAPSHOT.swf",
                "0px", "0px", PluginVersion.get(9, 0, 0));
        playerId = swf.getId();
        swf.addProperty("flashVars", "playerId=" + playerId);
        swf.addProperty("allowScriptAccess", "sameDomain");

        impl.init(playerId, mediaURL, autoplay, new MediaStateListener() {

            public void onPlayFinished() {
                firePlayFinished();
            }

            public void onLoadingComplete() {
                fireLoadingComplete();
            }

            public void onError(String description) {
                fireError(description);
            }

            public void onDebug(String report) {
                fireDebug(report);
            }

            public void onLoadingProgress(double progress) {
                fireLoadingProgress(progress);
            }

            public void onPlayStarted() {
                firePlayStarted();
            }

            public void onPlayerReady() {
                firePlayerReady();
            }

            public void onMediaInfoAvailable(MediaInfo info) {
                fireMediaInfoAvailable(info);
            }
        });

        AbsolutePanel ap = new AbsolutePanel();
        ap.add(swf, 0, 0);

        VerticalPanel hp = new VerticalPanel();
        hp.add(ap);

        isEmbedded = (height == null) || (width == null);
        if (!isEmbedded) {
            // placed to bypass UI setup in embedded cases especially
            // when used with CustomPlayer.
            logger = new Logger();
            logger.setVisible(false);
            ap.add(new FlatCustomControl(this), 0, 0);
            ap.setSize("100%", "22px");
            hp.add(logger);
            addMediaStateListener(new MediaStateListenerAdapter() {

                @Override
                public void onError(String description) {
                    logger.log(description, false);
                }

                @Override
                public void onDebug(String message) {
                    logger.log(message, false);
                }

                @Override
                public void onMediaInfoAvailable(MediaInfo info) {
                    logger.log(info.asHTMLString(), true);
                }
            });
        }
        initWidget(hp);
        setWidth(isEmbedded ? "0px" : width);
    }

    /**
     * Constructs <code>FlashMP3Player</code> to automatically playback media located at
     * {@code mediaURL} using the default height of 20px and width of 100%.
     *
     * <p> This is the same as calling {@code FlashMP3Player(mediaURL, true, "20px", "100%")}
     *
     * @param mediaURL the URL of the media to playback
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Flash plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Flash plugin is not installed on the client.
     *
     */
    public FlashMP3Player(String mediaURL) throws PluginNotFoundException,
            PluginVersionException, LoadException {
        this(mediaURL, true, "20px", "100%");
    }

    /**
     * Constructs <code>FlashMP3Player</code> to playback media located at {@code mediaURL}
     * using the default height of 20px and width of 100%. Media playback begins
     * automatically if {@code autoplay} is {@code true}.
     *
     * <p> This is the same as calling {@code FlashMP3Player(mediaURL, autoplay, "20px", "100%")}
     *
     * @param mediaURL the URL of the media to playback
     * @param autoplay {@code true} to start playing automatically, {@code false} otherwise
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Flash plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Flash plugin is not installed on the client.
     */
    public FlashMP3Player(String mediaURL, boolean autoplay) throws PluginNotFoundException,
            PluginVersionException, LoadException {
        this(mediaURL, autoplay, "20px", "100%");
    }

    public void close() {
        impl.closeMedia(playerId);
    }

    private void checkAvailable() {
        if (!impl.isPlayerAvailable(playerId)) {
            String message = "Player closed already or not available on page yet! " +
                    "Create another instance";
            fireDebug(message);
            throw new IllegalStateException(message);
        }
    }

    public long getMediaDuration() {
        checkAvailable();
        return (long) impl.getMediaDuration(playerId);
    }

    public double getPlayPosition() {
        checkAvailable();
        return impl.getPlayPosition(playerId);
    }

    public double getVolume() {
        checkAvailable();
        return impl.getVolume(playerId);
    }

    public void loadMedia(String mediaURL) throws LoadException {
        checkAvailable();
        impl.loadMedia(playerId, mediaURL);
    }

    public void pauseMedia() {
        checkAvailable();
        impl.pauseMedia(playerId);
    }

    public void playMedia() throws PlayException {
        checkAvailable();
        impl.playMedia(playerId);
    }

    public void setPlayPosition(double position) {
        checkAvailable();
        impl.setPlayPosition(playerId, position);
    }

    public void setVolume(double volume) {
        checkAvailable();
        impl.setVolume(playerId, volume);
    }

    public void stopMedia() {
        checkAvailable();
        impl.stopMedia(playerId);
    }

    @Override
    public void showLogger(boolean enable) {
        if (!isEmbedded) {
            logger.setVisible(enable);
        }
    }

    /**
     * Returns the number of times this player repeats playback before stopping.
     */
    @Override
    public int getLoopCount() {
        checkAvailable();
        return impl.getLoopCount(playerId);
    }

    /**
     * Sets the number of times the current media file should repeat playback before stopping.
     */
    @Override
    public void setLoopCount(final int loop) {
        if (impl.isPlayerAvailable(playerId)) {
            impl.setLoopCount(playerId, loop);
        } else {
            if (containsMediaStateListener(_onInitLoopCountListener)) {
                // ensure only one instance is queued ...
                removeMediaStateListener(_onInitLoopCountListener);
            }
            _onInitLoopCountListener = new MediaStateListenerAdapter() {

                @Override
                public void onPlayerReady() {
                    impl.setLoopCount(playerId, loop);
                    removeMediaStateListener(_onInitLoopCountListener);
                }
            };
            addMediaStateListener(_onInitLoopCountListener);
        }
    }

    @Override
    public void addToPlaylist(final String mediaURL) {
        if (impl.isPlayerAvailable(playerId)) {
            impl.addToPlaylist(playerId, mediaURL);
        } else {
            if (containsMediaStateListener(_onInitListListener)) {
                removeMediaStateListener(_onInitListListener);
            }

            _onInitListListener = new MediaStateListenerAdapter() {

                @Override
                public void onPlayerReady() {
                    impl.addToPlaylist(playerId, mediaURL);
                    removeMediaStateListener(_onInitListListener);
                }
            };
            addMediaStateListener(_onInitListListener);
        }
    }

    @Override
    public boolean isShuffleEnabled() {
        checkAvailable();
        return impl.isShuffleEnabled(playerId);
    }

    @Override
    public void removeFromPlaylist(int index) {
        checkAvailable();
        impl.removeFromPlaylist(playerId, index);
    }

    @Override
    public void setShuffleEnabled(final boolean enable) {
        if (impl.isPlayerAvailable(playerId)) {
            impl.setShuffleEnabled(playerId, enable);
        } else {
            if (containsMediaStateListener(_onInitSuffleListener)) {
                removeMediaStateListener(_onInitSuffleListener);
            }

            _onInitSuffleListener = new MediaStateListenerAdapter() {

                @Override
                public void onPlayerReady() {
                    impl.setShuffleEnabled(playerId, enable);
                    removeMediaStateListener(_onInitSuffleListener);
                }
            };
            addMediaStateListener(_onInitSuffleListener);
        }
    }
}
