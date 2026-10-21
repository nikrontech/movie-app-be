package lt.rildev.movieappbe.service;

import java.nio.file.Path;

public interface DownloadService {

    Path download(String url);
}
