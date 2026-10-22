package lt.rildev.movieappbe.task;

import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.model.MergedMovie;
import lt.rildev.movieappbe.model.MovieRating;
import lt.rildev.movieappbe.service.MergedMovieService;
import lt.rildev.movieappbe.service.OmdbService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OmdbSyncTask {

    private final MergedMovieService mergedMovieService;
    private final OmdbService omdbService;

    public OmdbSyncTask(MergedMovieService mergedMovieService, OmdbService omdbService) {
        this.mergedMovieService = mergedMovieService;
        this.omdbService = omdbService;
    }

    public void synchronize() {
        List<MergedMovie> movies = mergedMovieService.findAll();
        int checked = 0;

        for (MergedMovie movie : movies) {
            if (movie.rating() != null
                    && present(movie.director())
                    && present(movie.cast())
                    && present(movie.description())) {
                continue;
            }

            checked++;
            java.util.Optional<lt.rildev.movieappbe.omdb.OmdbMovie> details =
                    omdbService.findDetails(movie.originalTitle(), movie.releaseYear());
            if (details.isEmpty()) {
                details = omdbService.findDetails(movie.title(), movie.releaseYear());
            }

            details.ifPresent(omdb -> {
                MovieRating rating = movie.rating() == null
                        ? omdbService.toRating(omdb)
                        : movie.rating();
                mergedMovieService.save(movie.toBuilder()
                        .rating(rating)
                        .director(prefer(movie.director(), omdb.director()))
                        .cast(prefer(movie.cast(), omdb.actors()))
                        .description(prefer(movie.description(), omdb.plot()))
                        .build());
                log.info("Enriched missing fields for merged movie {}", movie.id());
                    });
        }

        log.info("OMDb synchronization checked {} movies", checked);
    }

    private String prefer(String primary, String fallback) {
        return present(primary) ? primary : fallback;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && !"N/A".equalsIgnoreCase(value);
    }
}
