package lt.rildev.movieappbe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class HttpClientUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    private HttpClientUtil() {
    }

    public static <T> HttpResponse<T> send(
            HttpClient client,
            HttpRequest request,
            HttpResponse.BodyHandler<T> bodyHandler
    ) throws IOException, InterruptedException {
        long startedAt = System.nanoTime();
        log.debug("HTTP request: {} {}", request.method(), request.uri());

        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.debug(
                    "HTTP response: {} {} -> {} ({} ms)",
                    request.method(),
                    request.uri(),
                    response.statusCode(),
                    durationMs
            );
            return response;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.warn(
                    "HTTP request failed: {} {} ({} ms): {}",
                    request.method(),
                    request.uri(),
                    durationMs,
                    exception.getMessage()
            );
            throw exception;
        }
    }
}
