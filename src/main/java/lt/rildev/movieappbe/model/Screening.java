package lt.rildev.movieappbe.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record Screening(
        String id,
        String movieId,
        Cinema cinema,
        Instant startsAt,
        Instant endsAt,
        String screen,
        String bookingUrl,
        String language,
        String subtitles,
        String format,
        String sound,
        String availability,
        Boolean soldOut,
        Boolean cancelled
) {
}
