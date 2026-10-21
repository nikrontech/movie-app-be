package lt.rildev.movieappbe.youtube;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/youtube")
public class YoutubeDownloadController {

    private final YoutubeVideoService youtubeVideoService;

    public YoutubeDownloadController(YoutubeVideoService youtubeVideoService) {
        this.youtubeVideoService = youtubeVideoService;
    }

    @PostMapping("/download")
    public ResponseEntity<DownloadResponse> download(
            @RequestBody DownloadRequest request
    ) {
        Path downloadedFile = youtubeVideoService.download(request.url());
        return ResponseEntity.ok(new DownloadResponse(downloadedFile.toString()));
    }

    public record DownloadRequest(String url) {
    }

    public record DownloadResponse(String path) {
    }
}
