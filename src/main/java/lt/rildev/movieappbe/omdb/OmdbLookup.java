package lt.rildev.movieappbe.omdb;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "omdb_lookups")
@Builder
public record OmdbLookup(
        @Id String id,
        boolean found,
        Instant checkedAt
) {
}
