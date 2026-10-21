package lt.rildev.movieappbe.model;

import lombok.Builder;

@Builder
public record AgeRating(
        String code,
        String description
) {
}
