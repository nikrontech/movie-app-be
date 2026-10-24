package lt.rildev.movieappbe.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CinemaSyncTask {

    private static final Logger log = LoggerFactory.getLogger(CinemaSyncTask.class);

    private final PasakaSyncTask pasakaSyncTask;
    private final VueSyncTask vueSyncTask;
    private final ForumSyncTask forumSyncTask;
    private final ApolloSyncTask apolloSyncTask;
    private final MovieMergeTask movieMergeTask;
    private final OmdbSyncTask omdbSyncTask;
    private final boolean syncEnabled;
    private final boolean pasakaEnabled;
    private final boolean vueEnabled;
    private final boolean forumEnabled;
    private final boolean apolloEnabled;
    private final boolean mergeEnabled;
    private final boolean omdbEnabled;

    public CinemaSyncTask(
            PasakaSyncTask pasakaSyncTask,
            VueSyncTask vueSyncTask,
            ForumSyncTask forumSyncTask,
            ApolloSyncTask apolloSyncTask,
            MovieMergeTask movieMergeTask,
            OmdbSyncTask omdbSyncTask,
            @Value("${cinema.sync.enabled:true}") boolean syncEnabled,
            @Value("${cinema.sync.pasaka.enabled:false}") boolean pasakaEnabled,
            @Value("${cinema.sync.vue.enabled:false}") boolean vueEnabled,
            @Value("${cinema.sync.forum.enabled:false}") boolean forumEnabled,
            @Value("${cinema.sync.apollo.enabled:false}") boolean apolloEnabled,
            @Value("${cinema.merge.enabled:false}") boolean mergeEnabled,
            @Value("${cinema.omdb.enabled:false}") boolean omdbEnabled
    ) {
        this.pasakaSyncTask = pasakaSyncTask;
        this.vueSyncTask = vueSyncTask;
        this.forumSyncTask = forumSyncTask;
        this.apolloSyncTask = apolloSyncTask;
        this.movieMergeTask = movieMergeTask;
        this.omdbSyncTask = omdbSyncTask;
        this.syncEnabled = syncEnabled;
        this.pasakaEnabled = pasakaEnabled;
        this.vueEnabled = vueEnabled;
        this.forumEnabled = forumEnabled;
        this.apolloEnabled = apolloEnabled;
        this.mergeEnabled = mergeEnabled;
        this.omdbEnabled = omdbEnabled;
    }

    @Scheduled(
            initialDelayString = "${cinema.sync.initial-delay-ms:1000}",
            fixedDelayString = "${cinema.sync.fixed-delay-ms:3600000}"
    )
    public void synchronize() {
        if (!syncEnabled) {
            log.debug("Cinema synchronization is disabled");
            return;
        }

        if (pasakaEnabled) {
            pasakaSyncTask.synchronizePasaka();
        }
        if (vueEnabled) {
            vueSyncTask.synchronizeVue();
        }
        if (forumEnabled) {
            forumSyncTask.synchronizeForum();
        }
        if (apolloEnabled) {
            apolloSyncTask.synchronizeApollo();
        }
        if (mergeEnabled) {
            movieMergeTask.mergeMovies();
        }
        if (omdbEnabled) {
            omdbSyncTask.synchronize();
        }

        log.debug(
                "Cinema synchronization completed: Pasaka={}, VUE={}, Forum={}, Apollo={}, Merge={}, OMDb={}",
                pasakaEnabled,
                vueEnabled,
                forumEnabled,
                apolloEnabled,
                mergeEnabled,
                omdbEnabled
        );
    }
}
