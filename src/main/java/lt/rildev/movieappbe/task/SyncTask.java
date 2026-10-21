package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.provider.CinemaDataProvider;
import lt.rildev.movieappbe.service.DownloadService;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieMediaService;
import lt.rildev.movieappbe.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class SyncTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final CinemaDataProvider dataProvider;
    private final MovieService movieService;
    private final MovieMediaService movieMediaService;
    private final DownloadService videoDownloader;
    private final ImageDownloaderService imageDownloader;

    protected SyncTask(
            CinemaDataProvider dataProvider,
            MovieService movieService,
            MovieMediaService movieMediaService,
            DownloadService videoDownloader,
            ImageDownloaderService imageDownloader
    ) {
        this.dataProvider = dataProvider;
        this.movieService = movieService;
        this.movieMediaService = movieMediaService;
        this.videoDownloader = videoDownloader;
        this.imageDownloader = imageDownloader;
    }

    protected final void synchronize(Cinema cinema) {
        try {
            List<Movie> existingMovies = movieService.findAll();
            List<Movie> movies = dataProvider.getMovies(null, null, null).stream()
                    .map(movie -> {
                        Movie savedMovie = existingMovies.stream()
                                .filter(existing -> existing.id().equals(movie.id()))
                                .findFirst()
                                .orElse(null);
                        Movie updatedMovie = movie.toBuilder()
                                .trailerPath(savedMovie == null ? null : savedMovie.trailerPath())
                                .posterPath(savedMovie == null ? null : savedMovie.posterPath())
                                .build();
                        updatedMovie = movieMediaService.reuseDownloadedMedia(updatedMovie, existingMovies);
                        updatedMovie = downloadTrailer(updatedMovie);
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
                || movie.externalTrailerUrl().isBlank()
                || movie.trailerPath() != null) {
            return movie;
        }

        try {
            java.nio.file.Path downloadedVideo = videoDownloader.download(movie.externalTrailerUrl());
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
                || movie.externalPosterUrl().isBlank()
                || movie.posterPath() != null) {
            return movie;
        }

        try {
            java.nio.file.Path cachedPoster = imageDownloader.download(movie.externalPosterUrl());
            return movie.toBuilder()
                    .posterPath(cachedPoster.getFileName().toString())
                    .build();
        } catch (RuntimeException exception) {
            log.warn("Could not download poster for movie {}", movie.id(), exception);
            return movie;
        }
    }
}
