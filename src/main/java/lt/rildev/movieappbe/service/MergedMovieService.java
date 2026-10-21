package lt.rildev.movieappbe.service;

import lt.rildev.movieappbe.model.MergedMovie;
import lt.rildev.movieappbe.repository.MergedMovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MergedMovieService {

    private final MergedMovieRepository repository;

    public MergedMovieService(MergedMovieRepository repository) {
        this.repository = repository;
    }

    public void replaceAll(List<MergedMovie> movies) {
        log.info("Replacing merged movie collection with {} movies", movies.size());
        repository.deleteAll();
        repository.saveAll(movies);
        log.info("Saved {} merged movies", movies.size());
    }

    public MergedMovie save(MergedMovie movie) {
        return repository.save(movie);
    }

    public List<MergedMovie> findAll() {
        return repository.findAll();
    }
}
