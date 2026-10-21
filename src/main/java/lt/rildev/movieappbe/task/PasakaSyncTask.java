package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.PasakaDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
 import lt.rildev.movieappbe.service.MovieMediaService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.springframework.stereotype.Component;

@Component
public class PasakaSyncTask extends SyncTask {

    public PasakaSyncTask(
            PasakaDataProvider dataProvider,
            YoutubeVideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieMediaService movieMediaService,
            MovieService movieService
    ) {
        super(dataProvider, movieService, movieMediaService, videoDownloader, imageDownloader);
    }

    public void synchronizePasaka() {
        synchronize(Cinema.PASAKA);
    }
}
