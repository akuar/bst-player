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

import com.bramosystems.oss.player.core.client.*;
import com.bramosystems.oss.player.core.client.MediaInfo.MediaInfoKey;
import com.bramosystems.oss.player.core.client.impl.BeforeUnloadCallback;
import com.bramosystems.oss.player.core.client.impl.PlayerWidget;
import com.bramosystems.oss.player.core.client.impl.WMPStateManager;
import com.bramosystems.oss.player.core.client.impl.WinMediaPlayerImpl;
import com.bramosystems.oss.player.core.event.client.DebugEvent;
import com.bramosystems.oss.player.core.event.client.DebugHandler;
import com.bramosystems.oss.player.core.event.client.MediaInfoEvent;
import com.bramosystems.oss.player.core.event.client.MediaInfoHandler;
import com.bramosystems.oss.player.core.event.client.PlayerStateEvent.State;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

/**
 * Widget to embed Windows Media Player&trade; plugin.
 *
 * <h3>Usage Example</h3>
 *
 * <p>
 * <code><pre>
 * SimplePanel panel = new SimplePanel();   // create panel to hold the player
 * Widget player = null;
 * try {
 *      // create the player
 *      player = new WinMediaPlayer("www.example.com/mediafile.wma");
 * } catch(LoadException e) {
 *      // catch loading exception and alert user
 *      Window.alert("An error occured while loading");
 * } catch(PluginVersionException e) {
 *      // catch plugin version exception and alert user, possibly providing a link
 *      // to the plugin download page.
 *      player = new HTML(".. some nice message telling the user to download plugin first ..");
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
public class WinMediaPlayer extends AbstractMediaPlayer {

    private static WMPStateManager stateManager;
    private WinMediaPlayerImpl impl;
    private PlayerWidget playerWidget;
    private String playerId, mediaURL, _width, _height;
    private Logger logger;
    private boolean isEmbedded, resizeToVideoSize;
    private UIMode uiMode;
    private BeforeUnloadCallback unloadCallback;

    private WinMediaPlayer() throws PluginNotFoundException, PluginVersionException {
        PluginVersion req = Plugin.WinMediaPlayer.getVersion();
        PluginVersion v = PlayerUtil.getWindowsMediaPlayerPluginVersion();
        if (v.compareTo(req) < 0) {
            throw new PluginVersionException(req.toString(), v.toString());
        }

        if (stateManager == null) {
            stateManager = GWT.create(WMPStateManager.class);
        }

        unloadCallback = new BeforeUnloadCallback() {

            public void onBeforeUnload() {
                stateManager.close(playerId);
                impl.close();
            }
        };

        playerId = DOM.createUniqueId().replace("-", "");
        resizeToVideoSize = false;
    }

    /**
     * Constructs <code>WinMediaPlayer</code> with the specified {@code height} and
     * {@code width} to playback media located at {@code mediaURL}. Media playback
     * begins automatically if {@code autoplay} is {@code true}.
     *
     * <p> {@code height} and {@code width} are specified as CSS units. A value of {@code null}
     * for {@code height} or {@code width} puts the player in embedded mode.  When in embedded mode,
     * the player is made invisible on the page and media state events are propagated to registered
     * listeners only.  This is desired especially when used with custom sound controls.  For custom
     * video control, specify valid CSS values for {@code height} and {@code width} but hide the
     * player controls with {@code setControllerVisible(false)}.
     *
     * @param mediaURL the URL of the media to playback
     * @param autoplay {@code true} to start playing automatically, {@code false} otherwise
     * @param height the height of the player
     * @param width the width of the player.
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Windows Media Player plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Windows Media Player plugin is not installed on the client.
     */
    public WinMediaPlayer(String mediaURL, boolean autoplay, String height, String width)
            throws LoadException, PluginNotFoundException, PluginVersionException {
        this();

        this.mediaURL = mediaURL;
        isEmbedded = (height == null) || (width == null);

        _height = height;
        _width = width;

        FlowPanel panel = new FlowPanel();
        initWidget(panel);

        playerWidget = new PlayerWidget(Plugin.WinMediaPlayer, playerId, mediaURL, autoplay, unloadCallback);
        panel.add(playerWidget);

        if (!isEmbedded) {
            logger = new Logger();
            logger.setVisible(false);
            panel.add(logger);

            addDebugHandler(new DebugHandler() {

                public void onDebug(DebugEvent event) {
                    logger.log(event.getMessage(), false);
                }
            });
            addMediaInfoHandler(new MediaInfoHandler() {

                public void onMediaInfoAvailable(MediaInfoEvent event) {
                    logger.log(event.getMediaInfo().asHTMLString(), true);
                    MediaInfo info = event.getMediaInfo();
                    if (info.getAvailableItems().contains(MediaInfoKey.VideoHeight)
                            || info.getAvailableItems().contains(MediaInfoKey.VideoWidth)) {
                        checkVideoSize(Integer.parseInt(info.getItem(MediaInfoKey.VideoHeight)) + 50,
                                Integer.parseInt(info.getItem(MediaInfoKey.VideoWidth)));
                    }
                }
            });
        } else {
            _width = "0px";
            _height = "0px";
            setConfigParameter(ConfigParameter.WMPUIMode, UIMode.INVISIBLE);
        }
    }

    /**
     * Constructs <code>WinMediaPlayer</code> to automatically playback media located at
     * {@code mediaURL} using the default height of 50px and width of 100%.
     *
     * <p>This is the same as calling {@code WinMediaPlayer(mediaURL, true, "50px", "100%")}</p>
     *
     * @param mediaURL the URL of the media to playback
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Windows Media Player plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Windows Media Player plugin is not installed on the client.
     */
    public WinMediaPlayer(String mediaURL) throws LoadException,
            PluginNotFoundException, PluginVersionException {
        this(mediaURL, true, "50px", "100%");
    }

    /**
     * Constructs <code>WinMediaPlayer</code> to playback media located at {@code mediaURL} using
     * the default height of 50px and width of 100%. Media playback begins automatically if
     * {@code autoplay} is {@code true}.
     *
     * <p> This is the same as calling {@code WinMediaPlayer(mediaURL, autoplay, "50px", "100%")}
     *
     * @param mediaURL the URL of the media to playback
     * @param autoplay {@code true} to start playing automatically, {@code false} otherwise
     *
     * @throws LoadException if an error occurs while loading the media.
     * @throws PluginVersionException if the required Windows Media Player plugin version is not installed on the client.
     * @throws PluginNotFoundException if the Windows Media Player plugin is not installed on the client.
     */
    public WinMediaPlayer(String mediaURL, boolean autoplay) throws LoadException,
            PluginNotFoundException, PluginVersionException {
        this(mediaURL, autoplay, "50px", "100%");
    }

    /**
     * Quick resizing-fix for non-IE browsers. Method replaces player object
     * with another using video size of previously loaded media metadata.
     *
     * @param isResizing
     */
    private void setupPlayer(final boolean isResizing) {
        if (isResizing) {
            playerWidget.replace(Plugin.WinMediaPlayer, playerId, mediaURL, true);
        }

        impl = WinMediaPlayerImpl.getPlayer(playerId);
        stateManager.init(impl, WinMediaPlayer.this, isResizing);
        stateManager.registerMediaStateHandlers(impl);
        if (uiMode != null) {
            setUIMode(uiMode);
        }
    }

    /**
     * Overridden to register player for plugin DOM events
     */
    @Override
    protected final void onLoad() {
        playerWidget.setVisible(false);
        playerWidget.setSize(_width, _height);
        playerWidget.setVisible(true);
        setWidth(_width);
        setupPlayer(false);
    }

    public void loadMedia(String mediaURL) throws LoadException {
        checkAvailable();
        impl.setURL(mediaURL);
    }

    public void playMedia() throws PlayException {
        checkAvailable();
        impl.play();
    }

    public void stopMedia() {
        checkAvailable();
        stateManager.stop(playerId);
        impl.stop();
    }

    public void pauseMedia() {
        checkAvailable();
        impl.pause();
    }

    /**
     * @deprecated As of version 1.1, remove player from panel instead
     */
    @Override
    public void close() {
        stateManager.close(playerId);
        impl.close();
    }

    public long getMediaDuration() {
        checkAvailable();
        return (long) impl.getDuration();
    }

    public double getPlayPosition() {
        checkAvailable();
        return impl.getCurrentPosition();
    }

    public void setPlayPosition(double position) {
        checkAvailable();
        impl.setCurrentPosition(position);
    }

    public double getVolume() {
        checkAvailable();
        return impl.getVolume() / (double) 100;
    }

    public void setVolume(double volume) {
        checkAvailable();
        volume *= 100;
        impl.setVolume((int) volume);
        fireDebug("Volume set to " + ((int) volume) + "%");
    }

    private boolean isAvailable() {
        return isPlayerOnPage(playerId) && stateManager.isPlayerStateManaged(playerId);
    }

    private void checkAvailable() {
        if (!isAvailable()) {
            String message = "Player not available, create an instance";
            fireDebug(message);
            throw new IllegalStateException(message);
        }
    }

    @Override
    public void showLogger(boolean enable) {
        if (!isEmbedded) {
            logger.setVisible(enable);
        }
    }

    /**
     * Displays or hides the player controls.
     */
    @Override
    public void setControllerVisible(boolean show) {
        UIMode mode = null;
        if (show) {
            mode = isEmbedded ? UIMode.INVISIBLE : UIMode.FULL;
        } else {
            mode = isEmbedded ? UIMode.INVISIBLE : UIMode.NONE;
        }
        setUIMode(mode);
    }

    /**
     * Checks whether the player controls are visible.
     */
    @Override
    public boolean isControllerVisible() {
        UIMode mode = getUIMode();
        return mode.equals(UIMode.FULL) || mode.equals(UIMode.MINI);
    }

    /**
     * Returns the number of times this player repeats playback before stopping.
     */
    @Override
    public int getLoopCount() {
        checkAvailable();
        return impl.getLoopCount();
    }

    /**
     * Sets the number of times the current media file should repeat playback before stopping.
     *
     * <p>As of version 1.0, if this player is not available on the panel, this method
     * call is added to the command-queue for later execution.
     */
    @Override
    public void setLoopCount(final int loop) {
        if (isAvailable()) {
            impl.setLoopCount(loop);
        } else {
            addToPlayerReadyCommandQueue("loopcount", new Command() {

                public void execute() {
                    impl.setLoopCount(loop);
                }
            });
        }
    }

    @Override
    public int getVideoHeight() {
        checkAvailable();
        return impl.getVideoHeight();
    }

    @Override
    public int getVideoWidth() {
        checkAvailable();
        return impl.getVideoWidth();
    }

    @Override
    public void setResizeToVideoSize(boolean resize) {
        resizeToVideoSize = resize;
        if (isAvailable()) {
            // if player is on panel now update its size, otherwise
            // allow it to be handled by the MediaInfoHandler...
            checkVideoSize(getVideoHeight() + 50, getVideoWidth());
        }
    }

    @Override
    public boolean isResizeToVideoSize() {
        return resizeToVideoSize;
    }

    private void checkVideoSize(int vidHeight, int vidWidth) {
        String _h = _height, _w = _width;
        if (resizeToVideoSize) {
            if ((vidHeight > 0) && (vidWidth > 0)) {
                // adjust to video size ...
                fireDebug("Resizing Player : " + vidWidth + " x " + vidHeight);
                _w = vidWidth + "px";
                _h = vidHeight + "px";
            }
        }

        playerWidget.setSize(_w, _h);
        setWidth(_w);

        if (!_height.equals(_h) && !_width.equals(_w)) {
            if (stateManager.shouldRunResizeQuickFix()) {
                setupPlayer(true);
            }
            firePlayerStateEvent(State.DimensionChangedOnVideo);
        }
    }

    /**
     * Retrieves the current media library access rights of the player
     *
     * @return current media access rights
     * @since 1.0
     */
    public MediaAccessRights getMediaAccessRights() {
        checkAvailable();
        return MediaAccessRights.valueOf(impl.getMediaAccessRight().toLowerCase());
    }

    /**
     * Sets the media library access rights level of the player.
     *
     * <p>If the player is not attached to a player, this call is scheduled for execution
     * when the underlying plugin is initialized.
     *
     * <p><em>Note:</em> The request succeeds IF AND ONLY IF the user grants this permission.
     * <p><em>Note:</em> This operation ALWAYS FAIL on browsers using the Windows Media Player
     * plugin for Firefox.
     *
     * @param accessRights access level
     * @since 1.0
     */
    public void setMediaAccessRights(final MediaAccessRights accessRights) {
        if (isAvailable()) {
            impl.requestMediaAccessRight(accessRights.name().toLowerCase());
        } else {
            addToPlayerReadyCommandQueue("accessright", new Command() {

                public void execute() {
                    impl.requestMediaAccessRight(accessRights.name().toLowerCase());
                }
            });
        }
    }

    /**
     * Sets the UI mode of the player
     *
     * <p>If the player is not attached to a player, this call is scheduled for execution
     * when the underlying plugin is initialized.
     *
     * <p><b>Note - Google Chrome 3:</b> This method fails! As of version 1.1 use
     * {@linkplain #setConfigParameter(ConfigParameter, ConfigValue)} instead.</p>
     *
     * @since 1.0
     * @param uiMode the UI mode to set
     */
    public void setUIMode(final UIMode uiMode) {
        this.uiMode = uiMode;
        if (isAvailable()) {
            impl.setUIMode(uiMode.name().toLowerCase());
        } else {
            addToPlayerReadyCommandQueue("uimode", new Command() {

                public void execute() {
                    impl.setUIMode(uiMode.name().toLowerCase());
                }
            });
        }
    }

    /**
     * Retrieves the current UI mode of the player
     *
     * @return current UI mode
     * @since 1.0
     */
    public UIMode getUIMode() {
        checkAvailable();
        try {
            return UIMode.valueOf(impl.getUIMode().toUpperCase());
        } catch (Exception e) {
            // Chrome 3 UIMode workaround ...
            String wm = playerWidget.getParam("uimode");
            if (wm != null) {
                return UIMode.valueOf(wm.toUpperCase());
            } else {
                return null;
            }
        }
    }

    @Override
    public <T extends ConfigValue> void setConfigParameter(ConfigParameter param, T value) {
        super.setConfigParameter(param, value);
        if (!isPlayerOnPage(playerId)) {
            switch (param) {
                case WMPUIMode:
                    if (isEmbedded) {
                        playerWidget.addParam("uimode", UIMode.INVISIBLE.name().toLowerCase());
                        return;
                    }

                    if (value != null) {
                        playerWidget.addParam("uimode", ((UIMode) value).name().toLowerCase());
                    } else {
                        playerWidget.removeParam("uimode");
                    }
            }
        }
    }

    @Override
    public void setRate(final double rate) {
        if (isPlayerOnPage(playerId)) {
            impl.setRate(rate);
        } else {
            addToPlayerReadyCommandQueue("rate", new Command() {

                public void execute() {
                    impl.setRate(rate);
                }
            });
        }
    }

    @Override
    public double getRate() {
        checkAvailable();
        return impl.getRate();
    }

    /**
     * An enum of Library Access levels of the Windows Media Player&trade; plugin.
     *
     * @since 1.0
     * @author Sikirulai Braheem
     */
    public static enum MediaAccessRights {

        /**
         * No access
         */
        NONE,
        /**
         * Read only access
         */
        READ,
        /**
         * Read/write access
         */
        FULL
    }

    /**
     * An enum of user interface modes of the Windows Media Player&trade; plugin.
     *
     * <p>The mode indicates which controls are shown on the user interface.
     *
     * @since 1.0
     * @author Sikirulai Braheem
     */
    public static enum UIMode implements ConfigValue {

        /**
         * The player is embedded without any visible user interface
         * (controls, video or visualization window).
         */
        INVISIBLE,
        /**
         * The player is embedded without controls, and with only the video or
         * visualization window displayed.
         */
        NONE,
        /**
         * The player is embedded with the status window, play/pause, stop, mute, and
         * volume controls shown in addition to the video or visualization window.
         */
        MINI,
        /**
         * The player is embedded with the status window, seek bar, play/pause,
         * stop, mute, next, previous, fast forward, fast reverse, and
         * volume controls in addition to the video or visualization window.
         */
        FULL
    }
}