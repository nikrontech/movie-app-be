package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.provider.CinemaDataProvider;
import lt.rildev.movieappbe.service.DownloadService;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SyncTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CinemaDataProvider dataProvider;
    private final MovieService movieService;
    private final DownloadService videoDownloader;
    private final ImageDownloaderService imageDownloader;
    private final Map<String, Path> downloadedVideos = new ConcurrentHashMap<>();
    private final Map<String, Path> downloadedPosters = new ConcurrentHashMap<>();

    protected SyncTask(
            CinemaDataProvider dataProvider,
            MovieService movieService,
            DownloadService videoDownloader,
            ImageDownloaderService imageDownloader
    ) {
        this.dataProvider = dataProvider;
        this.movieService = movieService;
        this.videoDownloader = videoDownloader;
        this.imageDownloader = imageDownloader;
    }

    protected final void synchronize(Cinema cinema) {
        try {
            List<Movie> movies = dataProvider.getMovies(null, null, null).stream()
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
                    "Synchronized {} catalog: {} movies, {} downloaded trailers, {} downloaded posters",
                    cinema.displayName(),
                    movies.size(),
                    downloadedVideos,
                    downloadedPosters
            );
        } catch (RuntimeException exception) {
            log.error("Could not update {} catalog", cinema.displayName(), exception);
        }
    }

    private Movie downloadTrailer(Movie movie) {
        if (videoDownloader == null
                || movie.externalTrailerUrl() == null
                || movie.externalTrailerUrl().isBlank()) {
            return movie;
        }

        try {
            Path downloadedVideo = downloadedVideos.get(movie.id());
            if (downloadedVideo == null || !Files.isRegularFile(downloadedVideo)) {
                downloadedVideo = videoDownloader.download(movie.externalTrailerUrl());
                downloadedVideos.put(movie.id(), downloadedVideo);
            }
            return movie.toBuilder()
                    .trailerPath(downloadedVideo.getFileName().toString())
                    .build();
        } catch (RuntimeException exception) {
            log.warn("Could not download trailer for movie {}", movie.id(), exception);
            return movie;
        }
    }

    private Movie downloadPoster(Movie movie) {
        if (imageDownloader == null
                || movie.externalPosterUrl() == null
                || movie.externalPosterUrl().isBlank()) {
            return movie;
        }

        try {
            Path cachedPoster = downloadedPosters.get(movie.id());
            if (cachedPoster == null || !Files.isRegularFile(cachedPoster)) {
                cachedPoster = imageDownloader.download(movie.externalPosterUrl());
                downloadedPosters.put(movie.id(), cachedPoster);
            }
            return movie.toBuilder()
                    .posterPath(cachedPoster.getFileName().toString())
                    .build();
        } catch (RuntimeException exception) {
            log.warn("Could not download poster for movie {}", movie.id(), exception);
            return movie;
        }
    }
}
