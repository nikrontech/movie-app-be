package lt.rildev.movieappbe.repository;

import lt.rildev.movieappbe.omdb.OmdbMovie;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OmdbMovieRepository extends MongoRepository<OmdbMovie, String> {

    Optional<OmdbMovie> findFirstByTitleAndYear(String title, String year);

    Optional<OmdbMovie> findFirstByTitle(String title);
}
