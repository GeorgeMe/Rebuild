package rejasupotaro.rebuild.activities;

import com.squareup.otto.Subscribe;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

import rejasupotaro.rebuild.R;
import rejasupotaro.rebuild.adapters.EpisodeDetailPagerAdapter;
import rejasupotaro.rebuild.events.BusProvider;
import rejasupotaro.rebuild.events.DownloadEpisodeCompleteEvent;
import rejasupotaro.rebuild.fragments.EpisodeMediaFragment;
import rejasupotaro.rebuild.models.Episode;
import rejasupotaro.rebuild.tools.MainThreadExecutor;
import rejasupotaro.rebuild.tools.MenuDelegate;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

public class EpisodeDetailActivity extends RoboActionBarActivity {

    private static final String EXTRA_EPISODE_ID = "extra_episode_id";

    @InjectExtra(value = EXTRA_EPISODE_ID)
    private int episodeId;

    private Episode currentEpisode;

    @InjectView(R.id.episode_detail_view_pager)
    private ViewPager viewPager;

    @InjectView(R.id.pager_tab_strip)
    private PagerTabStrip pagerTabStrip;

    private EpisodeMediaFragment episodeMediaFragment;

    @Inject
    private MenuDelegate menuDelegate;

    @Inject
    private MainThreadExecutor mainThreadExecutor;

    private EpisodeDetailPagerAdapter pagerAdapter;

    public static Intent createIntent(Context context, int episodeId) {
        Intent intent = new Intent(context, EpisodeDetailActivity.class);
        intent.putExtra(EXTRA_EPISODE_ID, episodeId);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_detail);

        BusProvider.getInstance().register(this);

        currentEpisode = Episode.findById(episodeId);
        setupActionBar(currentEpisode);
        episodeMediaFragment = (EpisodeMediaFragment) getSupportFragmentManager().findFragmentById(
                R.id.fragment_episode_media);
        episodeMediaFragment.setup(currentEpisode);
        setupViewPager(currentEpisode);
    }

    @Override
    public void onDestroy() {
        BusProvider.getInstance().unregister(this);
        super.onDestroy();
    }

    private void setupActionBar(Episode episode) {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        String originalTitle = episode.getTitle();
        int startIndex = originalTitle.indexOf(':');
        actionBar.setTitle("Episode " + originalTitle.substring(0, startIndex));
    }

    private void setupViewPager(Episode episode) {
        pagerAdapter = new EpisodeDetailPagerAdapter(getSupportFragmentManager(), episode);
        viewPager.setAdapter(pagerAdapter);

        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(Color.WHITE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.episode_detail, menu);
        return true;
    }

    public void close() {
        finish();
        overridePendingTransition(R.anim.zoom_in, R.anim.slide_down_exit);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                close();
                return true;
            case R.id.action_settings:
                menuDelegate.pressSettings();
                return true;
            case R.id.action_share:
                menuDelegate.pressShare(currentEpisode);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onEpisodeDownloadComplete(final DownloadEpisodeCompleteEvent event) {
        final Episode episode = Episode.findById(event.getEpisodeId());
        if (!currentEpisode.isSameEpisode(episode)) {
            return;
        }

        currentEpisode = episode;

        mainThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                episodeMediaFragment.setup(currentEpisode);
            }
        });
    }
}
