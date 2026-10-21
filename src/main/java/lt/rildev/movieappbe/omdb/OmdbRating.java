package lt.rildev.movieappbe.omdb;

import lombok.Builder;

@Builder
public record OmdbRating(
        String source,
        String value
) {
}
