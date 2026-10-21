package lt.rildev.movieappbe.service;

import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class VideoDownloaderService implements DownloadService {

    private final RestClient restClient;
    private final Path outputDirectory;
    private final long maxFileSizeBytes;

    public VideoDownloaderService(
            RestClient restClient,
            @Value("${video.downloader.output-directory:./downloads/trailers}") String outputDirectory,
            @Value("${video.downloader.max-file-size-bytes:524288000}") long maxFileSizeBytes
    ) {
        this.restClient = restClient;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public Path download(String videoUrl) {
        log.debug("Downloading direct video {}", videoUrl);
        URI uri = UrlUtil.validateHttpUrl(videoUrl);

        try {
            Files.createDirectories(outputDirectory);

            org.springframework.http.ResponseEntity<byte[]> response = restClient.get()
                    .uri(uri)
                    .header("Accept", "video/*")
                    .retrieve()
                    .toEntity(byte[].class);

            if (response.getStatusCode().value() < 200 || response.getStatusCode().value() >= 300) {
                throw new IllegalStateException(
                        "Video download failed with HTTP status " + response.getStatusCode().value()
                );
            }

            byte[] video = response.getBody();
            if (video.length > maxFileSizeBytes) {
                throw new IllegalStateException("Video exceeds the maximum allowed size");
            }

            Path file = outputDirectory.resolve(UUID.randomUUID() + ".mp4");
            Files.write(file, video);
            log.info("Downloaded video {} to {}", videoUrl, file);
            return file;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not download video", exception);
        }
    }
}
