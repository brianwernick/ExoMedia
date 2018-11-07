package com.devbrackets.android.exomediademo.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.devbrackets.android.exomedia.ExoMedia;
import com.devbrackets.android.exomedia.listener.VideoControlsSeekListener;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoControlsCore;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.devbrackets.android.exomediademo.App;
import com.devbrackets.android.exomediademo.R;
import com.devbrackets.android.exomediademo.data.MediaItem;
import com.devbrackets.android.exomediademo.data.Samples;
import com.devbrackets.android.exomediademo.manager.PlaylistManager;
import com.devbrackets.android.exomediademo.playlist.VideoApi;
import com.devbrackets.android.exomediademo.ui.subtitle.AspectRatioFrameLayout;
import com.devbrackets.android.exomediademo.ui.subtitle.SubtitleView;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.util.EventLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class VideoPlayerActivity extends Activity implements VideoControlsSeekListener {
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)

    protected VideoApi videoApi;
    protected VideoView videoView;
    protected PlaylistManager playlistManager;
    protected AspectRatioFrameLayout subtitleFrameLayout;
    protected SubtitleView subtitleView;
    protected AppCompatImageButton captionsButton;

    protected int selectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        retrieveExtras();
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playlistManager.removeVideoApi(videoApi);
        playlistManager.invokeStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playlistManager.invokeStop();
    }

    @Override
    public boolean onSeekStarted() {
        playlistManager.invokeSeekStarted();
        return true;
    }

    @Override
    public boolean onSeekEnded(long seekTime) {
        playlistManager.invokeSeekEnded(seekTime);
        return true;
    }

    /**
     * Retrieves the extra associated with the selected playlist index
     * so that we can start playing the correct item.
     */
    protected void retrieveExtras() {
        Bundle extras = getIntent().getExtras();
        selectedIndex = extras.getInt(EXTRA_INDEX, 0);
    }

    protected void init() {
        setupPlaylistManager();

        subtitleView = findViewById(R.id.subtitleView);
        videoView = findViewById(R.id.video_play_activity_video_view);
        subtitleFrameLayout = findViewById(R.id.video_player_activity_subtitle_frame_layout);

        captionsButton = new AppCompatImageButton(this);
        captionsButton.setBackgroundResource(android.R.color.transparent);
        captionsButton.setImageResource(R.drawable.ic_closed_caption_white_24dp);
        captionsButton.setOnClickListener(v -> showCaptionsMenu());

        videoView.setHandleAudioFocus(false);
        VideoControlsCore videoControlsCore = videoView.getVideoControlsCore();
        if (videoControlsCore instanceof VideoControls) {
            VideoControls videoControls = (VideoControls) videoControlsCore;
            videoControls.setSeekListener(this);
            if (videoView.trackSelectionAvailable()) {
                videoControls.addExtraView(captionsButton);
            }
        }
        videoView.setAnalyticsListener(new EventLogger(null));

        videoView.setOnVideoSizedChangedListener((intrinsicWidth, intrinsicHeight, pixelWidthHeightRatio) -> {
            float videoAspectRatio;
            if (intrinsicWidth == 0 || intrinsicHeight == 0) {
                videoAspectRatio = 1F;
            } else {
                videoAspectRatio = intrinsicWidth * pixelWidthHeightRatio / intrinsicHeight;
            }
            subtitleFrameLayout.setAspectRatio(videoAspectRatio);
        });
        videoView.setCaptionListener(subtitleView);

        videoApi = new VideoApi(videoView);
        playlistManager.addVideoApi(videoApi);
        playlistManager.play(0, false);
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private void setupPlaylistManager() {
        playlistManager = ((App)getApplicationContext()).getPlaylistManager();

        List<MediaItem> mediaItems = new LinkedList<>();
        for (Samples.Sample sample : Samples.getVideoSamples()) {
            MediaItem mediaItem = new MediaItem(sample, false);
            mediaItems.add(mediaItem);
        }

        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setId(PLAYLIST_ID);
    }

    private void showCaptionsMenu() {
        Map<ExoMedia.RendererType, TrackGroupArray> availableTracks = videoView.getAvailableTracks();
        if (availableTracks == null) {
            return;
        }
        TrackGroupArray trackGroupArray = availableTracks.get(ExoMedia.RendererType.CLOSED_CAPTION);
        if (trackGroupArray == null || trackGroupArray.isEmpty()) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(this, captionsButton);
        Menu menu = popupMenu.getMenu();
        // Add Menu Items
        MenuItem disabledItem = menu.add(0, CC_DISABLED, 0, getString(R.string.disable));
        disabledItem.setCheckable(true);
        MenuItem defaultItem = menu.add(0, CC_DEFAULT, 0, getString(R.string.auto));
        defaultItem.setCheckable(true);

        boolean selected = false;
        for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
            int selectedIndex = videoView.getSelectedTrackIndex(ExoMedia.RendererType.CLOSED_CAPTION, groupIndex);
            Log.d("Captions", "Selected Caption Track: " + groupIndex + " | " + selectedIndex);
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);
            for (int index = 0; index < trackGroup.length; index++) {
                Format format = trackGroup.getFormat(index);

                // Skip over non text formats.
                if (!format.sampleMimeType.startsWith("text")) {
                    continue;
                }

                String title = format.label;
                if (title == null) {
                    title = format.language;
                }
                if (title == null) {
                    title = format.id;
                }
                if (title == null) {
                    title = groupIndex + ":" + index;
                }

                int itemId = groupIndex * CC_GROUP_INDEX_MOD + index;
                MenuItem item = menu.add(0, itemId, 0, title);
                item.setCheckable(true);
                if (index == selectedIndex) {
                    item.setChecked(true);
                    selected = true;
                }
            }
        }

        if (!selected) {
            if (videoView.isRendererEnabled(ExoMedia.RendererType.CLOSED_CAPTION)) {
                defaultItem.setChecked(true);
            } else {
                disabledItem.setChecked(true);
            }
        }

        menu.setGroupCheckable(0, true, true);
        popupMenu.setOnMenuItemClickListener(menuItem -> onTrackSelected(menuItem));
        popupMenu.show();
    }

    private static final int CC_GROUP_INDEX_MOD = 1000;
    private static final int CC_DISABLED = -1001;
    private static final int CC_DEFAULT = -1000;

    private boolean onTrackSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        int itemId = menuItem.getItemId();
        if (itemId == CC_DEFAULT) {
            videoView.clearSelectedTracks(ExoMedia.RendererType.CLOSED_CAPTION);
        } else if (itemId == CC_DISABLED) {
            videoView.setRendererEnabled(ExoMedia.RendererType.CLOSED_CAPTION, false);
        } else {
            int trackIndex = itemId % CC_GROUP_INDEX_MOD;
            int groupIndex = itemId / CC_GROUP_INDEX_MOD;
            videoView.setTrack(ExoMedia.RendererType.CLOSED_CAPTION, groupIndex, trackIndex);
        }
        return true;
    }
}
