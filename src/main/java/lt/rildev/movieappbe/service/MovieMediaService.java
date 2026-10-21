package lt.rildev.movieappbe.service;

import lt.rildev.movieappbe.model.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MovieMediaService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);

    public Movie reuseDownloadedMedia(Movie movie, List<Movie> existingMovies) {
        Movie matchingMovie = existingMovies.stream()
                .filter(candidate -> !candidate.id().equals(movie.id()))
                .filter(candidate -> matches(movie, candidate))
                .filter(candidate -> candidate.trailerPath() != null || candidate.posterPath() != null)
                .findFirst()
                .orElse(null);

        if (matchingMovie == null) {
            log.debug("No previously downloaded media found for movie {}", movie.id());
            return movie;
        }

        log.info(
                "Reusing downloaded media for movie {} from source movie {}: trailer={}, poster={}",
                movie.id(),
                matchingMovie.id(),
                matchingMovie.trailerPath() != null,
                matchingMovie.posterPath() != null
        );

        return movie.toBuilder()
                .trailerPath(movie.trailerPath() == null ? matchingMovie.trailerPath() : movie.trailerPath())
                .posterPath(movie.posterPath() == null ? matchingMovie.posterPath() : movie.posterPath())
                .build();
    }

    private boolean matches(Movie left, Movie right) {
        if (!normalize(identityTitle(left)).equals(normalize(identityTitle(right)))) {
            return false;
        }

        if (left.releaseYear() != null && right.releaseYear() != null
                && !left.releaseYear().equals(right.releaseYear())) {
            return false;
        }

        return left.durationMinutes() == null
                || right.durationMinutes() == null
                || Math.abs(left.durationMinutes() - right.durationMinutes()) <= 5;
    }

    private String identityTitle(Movie movie) {
        if (movie.originalTitle() != null && !movie.originalTitle().isBlank()) {
            return movie.originalTitle();
        }

        String title = movie.title();
        if (title == null) {
            return "";
        }

        int openingParenthesis = title.lastIndexOf('(');
        int closingParenthesis = title.lastIndexOf(')');
        return openingParenthesis >= 0 && closingParenthesis > openingParenthesis
                ? title.substring(openingParenthesis + 1, closingParenthesis)
                : title;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\b(19|20)\\d{2}\\b", "")
                .replaceAll("(?i)^(5 euru savaite|nk ?26|sca ?26|nemokamas seansas senjorams|pasiklyde ekrane pristato)\\s*[:|-]?\\s*", "")
                .replaceAll("(?i)\\b(dubliuotas|dubbed)\\b", "");
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").trim();
    }
}
