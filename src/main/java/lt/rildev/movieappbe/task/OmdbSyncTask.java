package lt.rildev.movieappbe.task;

import lombok.extern.slf4j.Slf4j;
import lt.rildev.movieappbe.model.MergedMovie;
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
            if (movie.rating() != null) {
                continue;
            }

            checked++;
            java.util.Optional<lt.rildev.movieappbe.model.MovieRating> rating =
                    omdbService.findRating(movie.originalTitle(), movie.releaseYear());
            if (rating.isEmpty()) {
                rating = omdbService.findRating(movie.title(), movie.releaseYear());
            }

            rating
                    .ifPresent(omdbRating -> {
                        mergedMovieService.save(movie.toBuilder().rating(omdbRating).build());
                        log.info("Added OMDb rating to merged movie {}", movie.id());
                    });
        }

        log.info("OMDb synchronization checked {} movies", checked);
    }
}
