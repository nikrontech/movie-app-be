package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.PasakaDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cinema.sync.pasaka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PasakaSyncTask extends SyncTask {

    public PasakaSyncTask(
            PasakaDataProvider dataProvider,
            YoutubeVideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieService movieService
    ) {
        super(dataProvider, movieService, videoDownloader, imageDownloader);
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.pasaka.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.pasaka.fixed-delay-ms:3600000}"
    )
    public void synchronizePasaka() {
        synchronize(Cinema.PASAKA);
    }
}
