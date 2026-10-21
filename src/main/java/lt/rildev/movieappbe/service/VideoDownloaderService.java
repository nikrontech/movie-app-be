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
import java.util.UUID;

@Service
public class VideoDownloaderService implements DownloadService {

    private final HttpClient httpClient;
    private final Path outputDirectory;
    private final long maxFileSizeBytes;

    public VideoDownloaderService(
            HttpClient httpClient,
            @Value("${video.downloader.output-directory:./downloads/trailers}") String outputDirectory,
            @Value("${video.downloader.max-file-size-bytes:524288000}") long maxFileSizeBytes
    ) {
        this.httpClient = httpClient;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public Path download(String videoUrl) {
        URI uri = UrlUtil.validateHttpUrl(videoUrl);

        try {
            Files.createDirectories(outputDirectory);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(10))
                    .header("Accept", "video/*")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClientUtil.send(
                    httpClient,
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Video download failed with HTTP status " + response.statusCode()
                );
            }

            byte[] video = response.body();
            if (video.length > maxFileSizeBytes) {
                throw new IllegalStateException("Video exceeds the maximum allowed size");
            }

            Path file = outputDirectory.resolve(UUID.randomUUID() + ".mp4");
            Files.write(file, video);
            return file;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not download video", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Video download was interrupted", exception);
        }
    }
}
