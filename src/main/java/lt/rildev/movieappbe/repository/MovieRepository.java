package lt.rildev.movieappbe.repository;

import lt.rildev.movieappbe.model.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MovieRepository extends MongoRepository<Movie, String> {
}
