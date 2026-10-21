package lt.rildev.movieappbe.model;

import lombok.Builder;

@Builder
public record Genre(
        String name,
        String slug
) {
}
