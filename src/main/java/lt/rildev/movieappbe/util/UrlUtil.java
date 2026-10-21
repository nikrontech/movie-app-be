package lt.rildev.movieappbe.util;

import java.net.URI;
import java.util.Locale;

public final class UrlUtil {

    private UrlUtil() {
    }

    public static URI validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid URL", exception);
        }

        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are supported");
        }

        return uri;
    }

    public static URI validateYoutubeUrl(String url) {
        URI uri = validateHttpUrl(url);
        String host = uri.getHost();

        if (host == null || !isYoutubeHost(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only YouTube URLs are supported");
        }

        return uri;
    }

    private static boolean isYoutubeHost(String host) {
        return host.equals("youtube.com")
                || host.endsWith(".youtube.com")
                || host.equals("youtu.be")
                || host.endsWith(".youtu.be");
    }
}
