package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.provider.VueDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.VideoDownloaderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cinema.sync.vue.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class VueSyncTask extends SyncTask {

    public VueSyncTask(
            VueDataProvider dataProvider,
            VideoDownloaderService videoDownloader,
            ImageDownloaderService imageDownloader,
            MovieService movieService
    ) {
        super(dataProvider, movieService, videoDownloader, imageDownloader);
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.vue.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.vue.fixed-delay-ms:60000}"
    )
    public void synchronizeVue() {
        synchronize(Cinema.VUE);
    }
}
