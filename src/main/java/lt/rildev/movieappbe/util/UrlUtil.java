package lt.rildev.movieappbe.util;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class UrlUtil {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final Set<String> VIDEO_HOSTS = Set.of("youtube.com", "youtu.be", "vimeo.com");

    private UrlUtil() {
    }

    public static URI validateHttpUrl(String url) {
        URI uri = parse(url);
        if (!isHttpScheme(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are supported");
        }
        return uri;
    }

    public static URI validateVideoUrl(String url) {
        URI uri = validateHttpUrl(url);
        String host = uri.getHost();

        if (host == null || !isSupportedVideoHost(host)) {
            throw new IllegalArgumentException("Only YouTube and Vimeo URLs are supported");
        }

        return uri;
    }

    private static boolean isSupportedVideoHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return VIDEO_HOSTS.stream().anyMatch(
                supportedHost -> normalizedHost.equals(supportedHost)
                        || normalizedHost.endsWith("." + supportedHost)
        );
    }

    private static URI parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        try {
            return URI.create(url.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid URL", exception);
        }
    }

    private static boolean isHttpScheme(String scheme) {
        return HTTP.equalsIgnoreCase(scheme) || HTTPS.equalsIgnoreCase(scheme);
    }
}
