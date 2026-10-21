package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.provider.PasakaDataProvider;
import lt.rildev.movieappbe.service.MovieService;
import lt.rildev.movieappbe.service.YoutubeVideoDownloaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasakaSyncTask {

    private static final Logger log = LoggerFactory.getLogger(PasakaSyncTask.class);

    private final PasakaDataProvider pasakaDataProvider;
    private final YoutubeVideoDownloaderService youtubeVideoDownloaderService;
    private final MovieService movieService;
    private final Map<String, Path> downloadedVideos = new ConcurrentHashMap<>();

    public PasakaSyncTask(
            PasakaDataProvider pasakaDataProvider,
            YoutubeVideoDownloaderService youtubeVideoDownloaderService,
            MovieService movieService
    ) {
        this.pasakaDataProvider = pasakaDataProvider;
        this.youtubeVideoDownloaderService = youtubeVideoDownloaderService;
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
                        movieService.save(updatedMovie);
                        return updatedMovie;
                    })
                    .toList();
            long downloadedVideos = movies.stream()
                    .filter(movie -> movie.trailerPath() != null)
                    .count();
            log.info(
                    "Synchronized Pasaka catalog: {} movies, {} downloaded trailers",
                    movies.size(),
                    downloadedVideos
            );
        } catch (RuntimeException exception) {
            log.error("Could not update Pasaka in-memory catalog", exception);
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
