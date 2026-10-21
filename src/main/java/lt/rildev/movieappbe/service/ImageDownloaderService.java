package lt.rildev.movieappbe.service;

import lt.rildev.movieappbe.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final RestClient restClient;
    private final Path outputDirectory;
    private final long maxFileSizeBytes;

    public ImageDownloaderService(
            RestClient restClient,
            @Value("${image.downloader.output-directory:./downloads/posters}") String outputDirectory,
            @Value("${image.downloader.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.restClient = restClient;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public Path download(String imageUrl) {
        URI uri = UrlUtil.validateHttpUrl(imageUrl);

        try {
            Files.createDirectories(outputDirectory);

            org.springframework.http.ResponseEntity<byte[]> response = restClient.get()
                    .uri(uri)
                    .header("Accept", "image/*")
                    .retrieve()
                    .toEntity(byte[].class);

            if (response.getStatusCode().value() < 200 || response.getStatusCode().value() >= 300) {
                throw new IllegalStateException(
                        "Image download failed with HTTP status " + response.getStatusCode().value()
                );
            }

            String contentType = (response.getHeaders().getFirst("Content-Type") == null ? "" : response.getHeaders().getFirst("Content-Type"))
                    .split(";", 2)[0]
                    .toLowerCase(Locale.ROOT);
            String extension = IMAGE_EXTENSIONS.get(contentType);
            if (extension == null) {
                throw new IllegalStateException("Unsupported image content type: " + contentType);
            }

            byte[] image = response.getBody();
            if (image.length > maxFileSizeBytes) {
                throw new IllegalStateException("Image exceeds the maximum allowed size");
            }

            Path file = outputDirectory.resolve(UUID.randomUUID() + extension);
            Files.write(file, image);
            return file;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not download image", exception);
        }
    }

}
