package lt.rildev.movieappbe.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "merged_movies")
@Builder
public record MergedMovie(
        @Id String id,
        String title,
        String originalTitle,
        Integer durationMinutes,
        String description,
        String shortDescription,
        Instant releaseDate,
        Integer releaseYear,
        List<Genre> genres,
        AgeRating ageRating,
        String externalPosterUrl,
        String externalThumbnailUrl,
        String externalTrailerUrl,
        String thumbnailPath,
        List<String> sourceMovieIds,
        List<String> providers,
        List<Screening> screenings
) {
}
