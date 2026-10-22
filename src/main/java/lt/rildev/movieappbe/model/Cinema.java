package lt.rildev.movieappbe.model;

import lombok.Builder;

@Builder
public record Cinema(
        String id,
        String name,
        String address,
        City city
) {
}
