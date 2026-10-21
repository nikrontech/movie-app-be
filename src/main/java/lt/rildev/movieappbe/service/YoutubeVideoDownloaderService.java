package lt.rildev.movieappbe.service;

import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class YoutubeVideoDownloaderService implements DownloadService {

    private final String executable;
    private final Path outputDirectory;
    private final String format;
    private final String extractorArgs;
    private final Duration timeout;

    public YoutubeVideoDownloaderService(
            @Value("${youtube.downloader.executable:yt-dlp}") String executable,
            @Value("${youtube.downloader.output-directory:./downloads/videos}") String outputDirectory,
            @Value("${youtube.downloader.format:bestvideo*+bestaudio/best}") String format,
            @Value("${youtube.downloader.extractor-args:youtube:player_client=android}") String extractorArgs,
            @Value("${youtube.downloader.timeout-seconds:300}") long timeoutSeconds
    ) {
        this.executable = executable;
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
        this.format = format;
        this.extractorArgs = extractorArgs;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public Path download(String videoUrl) {
        log.debug("Downloading video with yt-dlp: {}", videoUrl);
        UrlUtil.validateVideoUrl(videoUrl);

        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create video output directory", exception);
        }

        List<String> command = List.of(
                executable,
                "--no-playlist",
                "--restrict-filenames",
                "--extractor-args", extractorArgs,
                "--format", format,
                "--merge-output-format", "mp4",
                "--output", outputDirectory.resolve(UUID.randomUUID() + ".%(ext)s").toString(),
                "--print", "after_move:filepath",
                videoUrl
        );

        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not start yt-dlp. Is it installed and available on PATH?",
                    exception
            );
        }

        String output;
        try {
            if (!process.waitFor(timeout)) {
                process.destroyForcibly();
                throw new IllegalStateException("Video download timed out");
            }
            output = new String(process.getInputStream().readAllBytes());
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new IllegalStateException("Could not read yt-dlp output", exception);
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Video download was interrupted", exception);
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException("Video download failed: " + output.trim());
        }

        Path file = findDownloadedFile(output);
        log.info("Downloaded video {} to {}", videoUrl, file);
        return file;
    }

    private Path findDownloadedFile(String processOutput) {
        return processOutput.lines()
                .map(String::trim)
                .map(Path::of)
                .filter(path -> path.startsWith(outputDirectory) && Files.isRegularFile(path))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException(
                        "yt-dlp completed but did not report the downloaded file"
                ));
    }

}
