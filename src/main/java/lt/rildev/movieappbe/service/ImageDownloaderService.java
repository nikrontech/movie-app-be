package lt.rildev.movieappbe.service;

import lt.rildev.movieappbe.util.UrlUtil;
import lt.rildev.movieappbe.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageDownloaderService implements DownloadService {

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final HttpClient httpClient;
    private final Path outputDirectory;
    private final long maxFileSizeBytes;

    public ImageDownloaderService(
            HttpClient httpClient,
            @Value("${image.downloader.output-directory:./downloads/posters}") String outputDirectory,
            @Value("${image.downloader.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.httpClient = httpClient;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public Path download(String imageUrl) {
        URI uri = UrlUtil.validateHttpUrl(imageUrl);

        try {
            Files.createDirectories(outputDirectory);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "image/*")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClientUtil.send(
                    httpClient,
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Image download failed with HTTP status " + response.statusCode()
                );
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .split(";", 2)[0]
                    .toLowerCase(Locale.ROOT);
            String extension = IMAGE_EXTENSIONS.get(contentType);
            if (extension == null) {
                throw new IllegalStateException("Unsupported image content type: " + contentType);
            }

            byte[] image = response.body();
            if (image.length > maxFileSizeBytes) {
                throw new IllegalStateException("Image exceeds the maximum allowed size");
            }

            Path file = outputDirectory.resolve(UUID.randomUUID() + extension);
            Files.write(file, image);
            return file;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not download image", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image download was interrupted", exception);
        }
    }

}
