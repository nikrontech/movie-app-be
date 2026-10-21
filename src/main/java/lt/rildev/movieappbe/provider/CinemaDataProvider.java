package lt.rildev.movieappbe.provider;

import lt.rildev.movieappbe.model.Cinema;
import lt.rildev.movieappbe.model.Genre;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.model.Screening;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CinemaDataProvider {

    String provider();

    List<Cinema> getCinemas();

    List<Movie> getMovies(String cinemaId, Instant from, Instant to);

    Optional<Movie> getMovie(String movieId);

    List<Screening> getScreenings(String cinemaId, Instant from, Instant to);

    List<Screening> getMovieScreenings(
            String cinemaId,
            String movieId,
            Instant from,
            Instant to
    );

    List<Genre> getGenres();
}
