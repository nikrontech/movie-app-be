package lt.rildev.movieappbe.task;

import lt.rildev.movieappbe.model.MergedMovie;
import lt.rildev.movieappbe.model.Movie;
import lt.rildev.movieappbe.service.MergedMovieService;
import lt.rildev.movieappbe.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MovieMergeTask {

    private static final Logger log = LoggerFactory.getLogger(MovieMergeTask.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern PARENTHESIZED = Pattern.compile("\\(([^()]*)\\)");

    private final MovieService movieService;
    private final MergedMovieService mergedMovieService;

    public MovieMergeTask(MovieService movieService, MergedMovieService mergedMovieService) {
        this.movieService = movieService;
        this.mergedMovieService = mergedMovieService;
    }

    public void mergeMovies() {
        try {
            Map<String, List<Movie>> groups = new LinkedHashMap<>();
            for (Movie movie : movieService.findAll()) {
                groups.computeIfAbsent(identity(movie), ignored -> new ArrayList<>()).add(movie);
            }

            List<MergedMovie> mergedMovies = groups.values().stream().map(this::merge).toList();
            mergedMovieService.replaceAll(mergedMovies);
            log.info("Merged {} source movies into {} movies", movieService.findAll().size(), mergedMovies.size());
        } catch (RuntimeException exception) {
            log.error("Could not merge movie records", exception);
        }
    }

    private MergedMovie merge(List<Movie> movies) {
        Movie preferred = movies.stream()
                .max((left, right) -> Integer.compare(completeness(left), completeness(right)))
                .orElseThrow();
        Set<String> providers = movies.stream().map(Movie::provider)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return MergedMovie.builder()
                .id("merged:" + identity(movies.get(0)))
                .title(cleanDisplayTitle(preferred.title()))
                .originalTitle(firstNonBlank(movies, Movie::originalTitle))
                .durationMinutes(preferred.durationMinutes())
                .description(firstNonBlank(movies, Movie::description))
                .shortDescription(firstNonBlank(movies, Movie::shortDescription))
                .releaseDate(firstNonNull(movies, Movie::releaseDate))
                .releaseYear(firstNonNull(movies, Movie::releaseYear))
                .genres(preferred.genres())
                .ageRating(preferred.ageRating())
                .externalPosterUrl(firstNonBlank(movies, Movie::externalPosterUrl))
                .externalThumbnailUrl(firstNonBlank(movies, Movie::externalThumbnailUrl))
                .externalTrailerUrl(firstNonBlank(movies, Movie::externalTrailerUrl))
                .thumbnailPath(firstNonBlank(movies, Movie::thumbnailPath))
                .sourceMovieIds(movies.stream().map(Movie::id).toList())
                .providers(new ArrayList<>(providers))
                .screenings(movies.stream().flatMap(movie -> movie.screenings().stream()).toList())
                .build();
    }

    private String identity(Movie movie) {
        String title = movie.originalTitle();
        if (title == null || title.isBlank()) title = parenthesizedTitle(movie.title());
        if (title == null || title.isBlank()) title = movie.title();
        String normalizedTitle = normalize(title);

        if (normalizedTitle.isBlank()) {
            normalizedTitle = normalize(movie.title());
        }

        if (normalizedTitle.isBlank()) {
            normalizedTitle = movie.provider() + ":" + movie.id();
        }

        String year = movie.releaseYear() == null ? "unknown" : movie.releaseYear().toString();
        return normalizedTitle + ":" + year;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = YEAR.matcher(normalized).replaceAll(" ");
        normalized = normalized.replaceAll(
                "^(5 euru savaite|nk ?26|sca ?26|nemokamas seansas senjorams|pasiklyde ekrane pristato)\\s*[:|-]?\\s*",
                ""
        );
        normalized = normalized.replaceAll("\\b(dubliuotas|dubbed)\\b", " ");
        return NON_ALPHANUMERIC.matcher(normalized).replaceAll(" ").trim();
    }

    private String parenthesizedTitle(String title) {
        if (title == null) return null;
        Matcher matcher = PARENTHESIZED.matcher(title);
        String result = null;
        while (matcher.find()) result = matcher.group(1);

        if (result == null || normalize(result).isBlank()) {
            return title;
        }
        return result;
    }

    private String cleanDisplayTitle(String title) {
        if (title == null) {
            return null;
        }

        return title
                .replaceFirst(
                        "(?i)^(5 eurų savaitė|nk ?26|sca ?26|nemokamas seansas senjorams|pasiklydę ekrane pristato)\\s*[:|-]?\\s*",
                        ""
                )
                .replaceFirst("(?i)\\s*\\((dubliuotas|dubbed|išankstinė premjera)\\)\\s*$", "")
                .trim();
    }

    private int completeness(Movie movie) {
        int score = 0;
        if (movie.originalTitle() != null && !movie.originalTitle().isBlank()) score++;
        if (movie.description() != null && !movie.description().isBlank()) score++;
        if (movie.externalPosterUrl() != null && !movie.externalPosterUrl().isBlank()) score++;
        if (movie.externalTrailerUrl() != null && !movie.externalTrailerUrl().isBlank()) score++;
        if (movie.screenings() != null && !movie.screenings().isEmpty()) score++;
        return score;
    }

    private String firstNonBlank(List<Movie> movies, Function<Movie, String> getter) {
        return movies.stream().map(getter)
                .filter(value -> value != null && !value.isBlank())
                .findFirst().orElse(null);
    }

    private <T> T firstNonNull(List<Movie> movies, Function<Movie, T> getter) {
        return movies.stream()
                .map(getter)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}
