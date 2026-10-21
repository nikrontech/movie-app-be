package lt.rildev.movieappbe.omdb;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "omdb_movies")
@Builder
public record OmdbMovie(
        @Id String imdbId,
        String title,
        String year,
        String imdbRating,
        String imdbVotes,
        String metascore,
        List<OmdbRating> ratings
) {
}
