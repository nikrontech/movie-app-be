package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.ForumDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cinema.sync.forum.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ForumSyncTask extends SyncTask {

    public ForumSyncTask(
            ForumDataProvider dataProvider,
            YoutubeVideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieService movieService
    ) {
        super(dataProvider, movieService, videoDownloader, imageDownloader);
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.forum.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.forum.fixed-delay-ms:60000}"
    )
    public void synchronizeForum() {
        synchronize(Cinema.FORUM);
    }
}
