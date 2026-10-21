package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.provider.CinemaDataProvider;
import lt.rildev.movieappbe.service.DownloadService;
import lt.rildev.movieappbe.service.ImageDownloaderService;
import lt.rildev.movieappbe.service.MovieMediaService;
import lt.rildev.movieappbe.service.MovieService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public abstract class SyncTask {
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
        log.info("Starting {} synchronization", cinema.displayName());
        try {
            List<Movie> existingMovies = movieService.findAll();
            log.debug("Loaded {} existing movies before {} synchronization", existingMovies.size(), cinema.displayName());

            List<Movie> sourceMovies = dataProvider.getMovies(null, null, null);
            log.info("Fetched {} movies from {}", sourceMovies.size(), cinema.displayName());

            List<Movie> movies = sourceMovies.stream()
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
                        log.debug("Saved {} movie {}", cinema.displayName(), updatedMovie.id());
                        return updatedMovie;
                    })
                    .toList();

            long moviesWithVideos = movies.stream()
                    .filter(movie -> movie.trailerPath() != null)
                    .count();
            long moviesWithPosters = movies.stream()
                    .filter(movie -> movie.posterPath() != null)
                    .count();

            log.info(
                    "Synchronized {} catalog: {} movies, {} with trailers, {} with posters",
                    cinema.displayName(),
                    movies.size(),
                    moviesWithVideos,
                    moviesWithPosters
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
            Path downloadedVideo = videoDownloader.download(movie.externalTrailerUrl());
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
