package com.seailz.discordjar.utils.image;

import java.util.Arrays;
import java.util.Base64;

public class ImageUtils {

    public static String getUrl(String hash, ImageType type, String... params) {
        String urlWithBase = "https://cdn.discordapp.com/" + type.getUrl();
        String paramable = urlWithBase.replaceAll("%h", hash);

        Object[] paramsList = Arrays.stream(params).toArray();
        return String.format(paramable, paramsList);
    }

    public static Image createImageData(ImageFormat format, String rawImageData) {
        String base64Data = Base64.getEncoder().encodeToString(rawImageData.getBytes());
        return new Image(format, "data:%s;base64,%s".formatted(format.toString(), base64Data));
    }

    public record Image(
            ImageFormat format,
            String base64Data
    ) {}

    public enum ImageFormat {
        JPEG("image/jpeg"),
        PNG("image/png"),
        GIF("image/gif");

        private final String f;

        ImageFormat(String f) {
            this.f = f;
        }

        @Override
        public String toString() {
            return f;
        }
    }

    public enum ImageType {
        CUSTOM_EMOJI("emojis/%h"),
        GUILD_ICON("icons/%s/%h"),
        GUILD_SPLASH("splashes/%s/%h"),
        GUILD_DISCOVERY_SPLASH("discovery-splashes/%s/%h"),
        GUILD_BANNER("banners/%s/%h"),
        USER_BANNER("banners/%s/%h"),
        DEFAULT_USER_AVATAR("embed/avatars/%h"),
        USER_AVATAR("avatars/%s/%h"),
        GUILD_MEMBER_AVATAR("guilds/%s/users/%s/avatars/%h"),
        APPLICATION_ICON("app-icons/%s/%h"),
        APPLICATION_COVER("app-icons/%s/%h"),
        APPLICATION_ASSET("app-assets/%s/%h"),
        ACHIEVEMENT_ICON("app-assets/%s/achievements/%s/icons/%h"),
        STICKER_PACK_BANNER("app-assets/710982414301790216/store/%s"),
        TEAM_ICON("team-icons/%s/%h"),
        STICKER("stickers/%s"),
        ROLE_ICON("role-icons/%s/%h"),
        GUILD_SCHEDULED_EVENT_COVER("guild-events/%s/%h"),
        GUILD_MEMBER_BANNER("guild/%s/users/%s/banners/%h"),
        DM_ICON("channel-icons/%s/%h");

        private String url;

        ImageType(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

}
