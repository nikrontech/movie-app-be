package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.provider.PasakaDataProvider;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        name = "cinema.sync.pasaka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PasakaSyncTask {

    private static final Logger log = LoggerFactory.getLogger(PasakaSyncTask.class);

    private final PasakaDataProvider pasakaDataProvider;
    private final YoutubeVideoDownloaderService youtubeVideoDownloaderService;
    private final ImageDownloaderService imageDownloaderService;
    private final MovieService movieService;
    private final Map<String, Path> downloadedVideos = new ConcurrentHashMap<>();
    private final Map<String, Path> downloadedPosters = new ConcurrentHashMap<>();

    public PasakaSyncTask(
            PasakaDataProvider pasakaDataProvider,
            YoutubeVideoDownloaderService youtubeVideoDownloaderService,
            ImageDownloaderService imageDownloaderService,
            MovieService movieService
    ) {
        this.pasakaDataProvider = pasakaDataProvider;
        this.youtubeVideoDownloaderService = youtubeVideoDownloaderService;
        this.imageDownloaderService = imageDownloaderService;
        this.movieService = movieService;
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.pasaka.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.pasaka.fixed-delay-ms:3600000}"
    )
    public void synchronize() {
        try {
            List<Movie> movies = pasakaDataProvider.getMovies(null, null, null).stream()
                    .map(movie -> {
                        Movie updatedMovie = downloadTrailer(movie);
                        updatedMovie = downloadPoster(updatedMovie);
                        movieService.save(updatedMovie);
                        return updatedMovie;
                    })
                    .toList();
            long downloadedVideos = movies.stream()
                    .filter(movie -> movie.trailerPath() != null)
                    .count();
            long downloadedPosters = movies.stream()
                    .filter(movie -> movie.posterPath() != null)
                    .count();
            log.info(
                    "Synchronized Pasaka catalog: {} movies, {} downloaded trailers, {} downloaded posters",
                    movies.size(),
                    downloadedVideos,
                    downloadedPosters
            );
        } catch (RuntimeException exception) {
            log.error("Could not update Pasaka in-memory catalog", exception);
        }
    }

    private Movie downloadPoster(Movie movie) {
        if (movie.externalPosterUrl() == null || movie.externalPosterUrl().isBlank()) {
            return movie;
        }

        Path cachedPoster = downloadedPosters.get(movie.id());
        if (cachedPoster != null && Files.isRegularFile(cachedPoster)) {
            return movie.toBuilder()
                    .posterPath(cachedPoster.getFileName().toString())
                    .build();
        }

        try {
            Path downloadedPoster = imageDownloaderService.download(movie.externalPosterUrl());
            downloadedPosters.put(movie.id(), downloadedPoster);
            return movie.toBuilder()
                    .posterPath(downloadedPoster.getFileName().toString())
                    .build();
        } catch (RuntimeException exception) {
            log.warn("Could not download poster for movie {}", movie.id(), exception);
            return movie;
        }
    }

    private Movie downloadTrailer(Movie movie) {
        if (movie.externalTrailerUrl() == null || movie.externalTrailerUrl().isBlank()) {
            return movie;
        }

        Path cachedVideo = downloadedVideos.get(movie.id());
        if (cachedVideo != null && Files.isRegularFile(cachedVideo)) {
            return withDownloadedMovie(movie, cachedVideo);
        }

        try {
            Path downloadedVideo = youtubeVideoDownloaderService.download(movie.externalTrailerUrl());
            downloadedVideos.put(movie.id(), downloadedVideo);
            return withDownloadedMovie(movie, downloadedVideo);
        } catch (RuntimeException exception) {
            log.warn("Could not download trailer for movie {}", movie.id(), exception);
            return movie;
        }
    }

    private Movie withDownloadedMovie(Movie movie, Path downloadedVideo) {
        return movie.toBuilder()
                .trailerPath(downloadedVideo.getFileName().toString())
                .build();
    }
}
