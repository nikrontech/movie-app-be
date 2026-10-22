package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.ApolloDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieMediaService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.springframework.stereotype.Component;

@Component
public class ApolloSyncTask extends SyncTask {
    public ApolloSyncTask(ApolloDataProvider dataProvider,
                          YoutubeVideoDownloaderService videoDownloader,
                          ImageDownloaderService imageDownloader,
                          MovieMediaService movieMediaService,
                          MovieService movieService) {
        super(dataProvider, movieService, movieMediaService, videoDownloader, imageDownloader);
    }

    public void synchronizeApollo() {
        synchronize(Cinema.APOLLO);
    }
}
