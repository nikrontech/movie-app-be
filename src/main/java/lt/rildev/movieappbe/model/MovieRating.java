package lt.rildev.movieappbe.model;

import lombok.Builder;

import java.util.List;

@Builder
public record MovieRating(
        String imdbId,
        String imdbRating,
        String imdbVotes,
        String metascore,
        List<RatingSource> sources
) {
}
