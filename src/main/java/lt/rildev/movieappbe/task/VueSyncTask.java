package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.VueDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.VideoDownloaderService;
import org.springframework.stereotype.Component;

@Component
public class VueSyncTask extends SyncTask {

    public VueSyncTask(
            VueDataProvider dataProvider,
            VideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieService movieService
    ) {
        super(dataProvider, movieService, videoDownloader, imageDownloader);
    }

    public void synchronizeVue() {
        synchronize(Cinema.VUE);
    }
}
