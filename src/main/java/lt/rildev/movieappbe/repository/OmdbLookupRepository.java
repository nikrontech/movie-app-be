package lt.rildev.movieappbe.repository;

import lt.rildev.movieappbe.omdb.OmdbLookup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OmdbLookupRepository extends MongoRepository<OmdbLookup, String> {
}
