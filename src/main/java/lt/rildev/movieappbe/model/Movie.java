package lt.rildev.movieappbe.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "movies")
@Builder(toBuilder = true)
public record Movie(
        @Id
        String id,
        String provider,
        String title,
        String originalTitle,
        String slug,
        String detailUrl,
        Integer durationMinutes,
        String description,
        String shortDescription,
        Instant releaseDate,
        Integer releaseYear,
        List<Genre> genres,
        AgeRating ageRating,
        String externalPosterUrl,
        String externalTrailerUrl,
        String trailerPath,
        String posterPath,
        Boolean premiere,
        List<Screening> screenings
) {
}
