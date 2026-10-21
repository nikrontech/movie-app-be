package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.ForumDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.springframework.stereotype.Component;

@Component
public class ForumSyncTask extends SyncTask {

    public ForumSyncTask(
            ForumDataProvider dataProvider,
            YoutubeVideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieService movieService
    ) {
        super(dataProvider, movieService, videoDownloader, imageDownloader);
    }

    public void synchronizeForum() {
        synchronize(Cinema.FORUM);
    }
}
