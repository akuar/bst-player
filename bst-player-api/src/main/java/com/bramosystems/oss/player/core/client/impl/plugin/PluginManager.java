/*
 * Copyright 2011 Sikirulai Braheem <sbraheem at bramosystems.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bramosystems.oss.player.core.client.impl.plugin;

import com.bramosystems.oss.player.core.client.Plugin;
import com.bramosystems.oss.player.core.client.PluginNotFoundException;
import com.bramosystems.oss.player.core.client.PluginVersion;
import com.bramosystems.oss.player.util.client.BrowserPlugin;
import com.bramosystems.oss.player.util.client.MimeType;
import com.bramosystems.oss.player.util.client.RegExp;
import com.bramosystems.oss.player.util.client.RegExp.RegexException;
import com.google.gwt.core.client.GWT;
import java.util.EnumMap;
import java.util.HashMap;

/**
 *
 * @author Sikirulai Braheem <sbraheem at bramosystems.com>
 */
public class PluginManager {

    private static final EnumMap<Plugin, PluginInfo> pluginInfoMap = new EnumMap<Plugin, PluginInfo>(Plugin.class);
    private static final MimeParserBase mpb = GWT.create(MimeParserBase.class);        

    static { // init infoMap ...
        PluginManagerImpl pmi = GWT.create(PluginManagerImpl.class);
        for (Plugin p : Plugin.values()) {
            try {
                PluginInfo inf = pmi.getPluginInfo(p);
                mpb.addPluginProperties(inf);
                pluginInfoMap.put(p, inf);
            } catch (PluginNotFoundException ex) {
                // plugin not available ...
            }
        }
    }

    public static PluginInfo getPluginInfo(Plugin plugin) throws PluginNotFoundException {
        if (pluginInfoMap.containsKey(plugin)) {
            return pluginInfoMap.get(plugin);
        } else {
            throw new PluginNotFoundException(plugin);
        }
    }

    public static native boolean isHTML5CompliantClient() /*-{
    try {
    var t = new Audio();
    t = null;
    return true;
    } catch(e){
    return false;
    }
    }-*/;
    
    public static HashMap<String, String> getRegisteredMimeTypes() {
        return mpb.getMimeTypes();
    }

    protected  static class PluginManagerImpl {

        public PluginInfo getPluginInfo(Plugin plugin) throws PluginNotFoundException {
            BrowserPlugin plug = null;
            PluginInfo pi = new PluginInfo();
            pi.setPlugin(plugin);

            if (plugin.equals(Plugin.Native) || plugin.equals(Plugin.WinMediaPlayer)) {
                switch (plugin) {
                    case WinMediaPlayer:
                        boolean found = false;
                        MimeType mt = MimeType.getMimeType("application/x-ms-wmp");
                        if (mt != null) {   // firefox plugin present...
                            found = true;
                            pi.setWrapperType(PluginInfo.PlayerPluginWrapperType.WMPForFirefox);
                        } else {   // firefox plugin not found check for generic..
                            mt = MimeType.getMimeType("application/x-mplayer2");
                            if (mt != null) {
                                try {
                                    plug = mt.getEnabledPlugin(); // who's got the mime ? (WMP / VLC)
                                    if (plug.getName().contains("Windows Media Player")) {
                                        found = true;
                                    }
                                } catch (PluginNotFoundException ex) {
                                }
                            }
                        }

                        if (found) {
                            pi.setVersion(PluginVersion.get(1, 1, 1));
                            plug = mt.getEnabledPlugin();
                            if (plug.getFileName().toLowerCase().contains("totem")
                                    || plug.getDescription().toLowerCase().contains("totem")) {
                                pi.setWrapperType(PluginInfo.PlayerPluginWrapperType.Totem);
                            }
                        } else {
                            throw new PluginNotFoundException(plugin);
                        }
                        break;
                    case Native:
                        if (isHTML5CompliantClient()) {
                            pi.setVersion(PluginVersion.get(5, 0, 0));
                        } else {
                            throw new PluginNotFoundException(plugin);
                        }
                }
                return pi;
            }

            PluginMimeTypes pt = PluginMimeTypes.none;
            switch (plugin) {
                case DivXPlayer:
                    pt = PluginMimeTypes.divx;
                    break;
                case FlashPlayer:
                    pt = PluginMimeTypes.flash;
                    break;
                case QuickTimePlayer:
                    pt = PluginMimeTypes.quicktime;
                    break;
                case VLCPlayer:
                    pt = PluginMimeTypes.vlc;
                    break;
            }

            MimeType mt = MimeType.getMimeType(pt.mime);
            if (mt != null) {   // plugin present...
                try {
                    String desc = mt.getEnabledPlugin().getDescription();
                    String name = mt.getEnabledPlugin().getName();
                    if (name.toLowerCase().contains(pt.whois)) { // who has it?
                        RegExp.RegexResult res = RegExp.getRegExp(pt.regex, "").exec(pt.versionInName ? name : desc);
                        pi.getVersion().setMajor(Integer.parseInt(res.getMatch(1)));
                        pi.getVersion().setMinor(Integer.parseInt(res.getMatch(2)));
                        pi.getVersion().setRevision(Integer.parseInt(res.getMatch(3)));
                        if (mt.getEnabledPlugin().getFileName().toLowerCase().contains("totem")
                                || desc.toLowerCase().contains("totem")) {
                            pi.setWrapperType(PluginInfo.PlayerPluginWrapperType.Totem);
                        }
                    }
                } catch (RegexException ex) {
                }
            } else {
                throw new PluginNotFoundException(plugin);
            }
            return pi;
        }
    }

    /**
     * IE specific native implementation of the PluginManagerImpl class. It is not recommended to
     * interact with this class directly.
     */
    protected static class PluginManagerImplIE extends PluginManagerImpl {

        private native void getFlashPluginVersion(PluginVersion version) /*-{
        try {
        verRegex = new RegExp("\\d+,\\d+,\\d+,\\d+", "");   // "WIN A,B,CCC,DD
        ax = new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
        ver = ax.GetVariable("$version");   // "WIN A,B,CCC,DD
        ver = (verRegex.exec(ver))[0].split(",");
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMajor(I)(parseInt(ver[0]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMinor(I)(parseInt(ver[1]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setRevision(I)(parseInt(ver[2]));
        ax.Quit();
        } catch (e) {}
        }-*/;

        private native void getQuickTimePluginVersion(PluginVersion version) /*-{
        try {
        ax = new ActiveXObject('QuickTimeCheckObject.QuickTimeCheck');
        ver = ax.QuickTimeVersion.toString(16);
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMajor(I)(parseInt(ver.charAt(0)));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMinor(I)(parseInt(ver.charAt(1)));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setRevision(I)(parseInt(ver.charAt(2)));
        ax.Quit();
        } catch (e) {}
        }-*/;

        /**
         * Native implementation of Windows Media Player plugin detection
         * @param version wraps the detected version numbers.
         */
        private native void getWindowsMediaPlayerVersion(PluginVersion version) /*-{
        try {
        ax = new ActiveXObject('WMPlayer.ocx');
        ver = ax.versionInfo;
        ver = ver.split(".");
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMajor(I)(parseInt(ver[0]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMinor(I)(parseInt(0));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setRevision(I)(parseInt(0));
        ax.Quit();
        } catch (e) {}
        }-*/;

        private native void getVLCPluginVersion(PluginVersion version) /*-{
        try {
        descRegex = new RegExp("\\d+.\\d+.\\d+", "");
        ax = new ActiveXObject('VideoLAN.VLCPlugin');
        ver = ax.VersionInfo;
        verArray = (descRegex.exec(ver))[0].split(".");
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMajor(I)(parseInt(verArray[0]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMinor(I)(parseInt(verArray[1]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setRevision(I)(parseInt(verArray[2]));
        ax.Quit();
        } catch (e) {}
        }-*/;

        private native void getDivXPluginVersion(PluginVersion version) /*-{
        try {
        descRegex = new RegExp("\\d+.\\d+.\\d+", "");
        ax = new ActiveXObject('npdivx.DivXBrowserPlugin');
        ver = ax.GetVersion();
        verArray = (descRegex.exec(ver))[0].split(".");
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMajor(I)(parseInt(verArray[0]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setMinor(I)(parseInt(verArray[1]));
        version.@com.bramosystems.oss.player.core.client.PluginVersion::setRevision(I)(parseInt(verArray[2]));
        ax.Quit();
        } catch (e) {}
        }-*/;

        @Override
        public PluginInfo getPluginInfo(Plugin plugin) throws PluginNotFoundException {
            PluginVersion pv = new PluginVersion();
            switch (plugin) {
                case DivXPlayer:
                    getDivXPluginVersion(pv);
                    break;
                case FlashPlayer:
                    getFlashPluginVersion(pv);
                    break;
                case QuickTimePlayer:
                    getQuickTimePluginVersion(pv);
                    break;
                case VLCPlayer:
                    getVLCPluginVersion(pv);
                    break;
                case WinMediaPlayer:
                    getWindowsMediaPlayerVersion(pv);
                    break;
                case Native:
                    if (isHTML5CompliantClient()) {
                        pv = PluginVersion.get(5, 0, 0);
                    }
            }
            if(pv.compareTo(new PluginVersion()) <= 0) {
                throw new PluginNotFoundException(plugin);
            }
            return new PluginInfo(plugin, pv, PluginInfo.PlayerPluginWrapperType.Native);
        }
    }

    private enum PluginMimeTypes {

        none("", "", "", false),
        divx("video/divx", "divx", "(\\d+).(\\d+).(\\d+)", false),
        flash("application/x-shockwave-flash", "shockwave flash", "(\\d+).(\\d+)\\s*[r|d|b](\\d+)", false),
        vlc("application/x-vlc-plugin", "vlc", "(\\d+).(\\d+).(\\d+)", false),
        quicktime("video/quicktime", "quicktime", "(\\d+).(\\d+).(\\d+)", true);

        private PluginMimeTypes(String mime, String whois, String regex, boolean versionInName) {
            this.mime = mime;
            this.whois = whois;
            this.regex = regex;
            this.versionInName = versionInName;
        }
        String mime, whois, regex;
        boolean versionInName;
    }
}
