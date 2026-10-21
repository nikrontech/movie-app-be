package lt.rildev.movieappbe.service;

import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Movie save(Movie movie) {
        Movie savedMovie = movieRepository.save(movie);
        log.debug("Saved movie {} from provider {}", savedMovie.id(), savedMovie.provider());
        return savedMovie;
    }

    public Optional<Movie> findById(String id) {
        return movieRepository.findById(id);
    }

    public List<Movie> findAll() {
        return movieRepository.findAll();
    }
}
