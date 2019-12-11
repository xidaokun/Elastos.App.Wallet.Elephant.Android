package org.moment.lib.widgets.videolist.model;

import android.media.MediaPlayer;

import org.moment.lib.widgets.videolist.widget.TextureVideoView;


public interface VideoLoadMvpView {

    TextureVideoView getVideoView();

    void videoBeginning();

    void videoStopped();

    void videoPrepared(MediaPlayer player);

    void videoResourceReady(String videoPath);
}
