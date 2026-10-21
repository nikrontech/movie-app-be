package lt.rildev.movieappbe.service;

import lt.rildev.movieappbe.model.MergedMovie;
import lt.rildev.movieappbe.repository.MergedMovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MergedMovieService {

    private final MergedMovieRepository repository;

    public MergedMovieService(MergedMovieRepository repository) {
        this.repository = repository;
    }

    public void replaceAll(List<MergedMovie> movies) {
        repository.deleteAll();
        repository.saveAll(movies);
    }
}
