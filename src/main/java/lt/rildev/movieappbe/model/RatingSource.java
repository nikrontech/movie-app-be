package lt.rildev.movieappbe.model;

import lombok.Builder;

@Builder
public record RatingSource(
        String source,
        String value
) {
}
