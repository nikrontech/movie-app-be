package lt.rildev.movieappbe.repository;

import lt.rildev.movieappbe.model.MergedMovie;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MergedMovieRepository extends MongoRepository<MergedMovie, String> {
}
