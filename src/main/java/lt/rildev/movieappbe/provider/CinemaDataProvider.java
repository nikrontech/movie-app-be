package lt.rildev.movieappbe.provider;

import lt.rildev.movieappbe.model.Cinema;
import lt.rildev.movieappbe.model.Genre;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.model.Screening;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Provider-agnostic interface for retrieving cinema data.
 *
 * <p>Implementations should map the source cinema API into the common models
 * in {@code lt.rildev.movieappbe.model}.</p>
 */
public interface CinemaDataProvider {

    /**
     * Returns the identifier used by this provider.
     */
    String provider();

    /**
     * Returns cinemas available from this provider.
     */
    List<Cinema> getCinemas();

    /**
     * Returns movies available in the requested date range.
     * A null cinema ID means all cinemas supported by the provider.
     */
    List<Movie> getMovies(String cinemaId, Instant from, Instant to);

    /**
     * Returns one movie by its provider-specific ID or slug.
     */
    Optional<Movie> getMovie(String movieId);

    /**
     * Returns all screenings in a cinema and date range.
     */
    List<Screening> getScreenings(String cinemaId, Instant from, Instant to);

    /**
     * Returns screenings for one movie in a cinema and date range.
     */
    List<Screening> getMovieScreenings(
            String cinemaId,
            String movieId,
            Instant from,
            Instant to
    );

    /**
     * Returns genres supported by this provider.
     */
    List<Genre> getGenres();
}
