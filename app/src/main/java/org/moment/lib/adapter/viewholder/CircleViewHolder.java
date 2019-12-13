package org.moment.lib.adapter.viewholder;

import android.media.MediaPlayer;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import org.moment.lib.widgets.CommentListView;
import org.moment.lib.widgets.ExpandTextView;
import org.moment.lib.widgets.PraiseListView;
import org.moment.lib.widgets.SnsPopupWindow;
import org.moment.lib.widgets.videolist.model.VideoLoadMvpView;
import org.moment.lib.widgets.videolist.widget.TextureVideoView;

public abstract class CircleViewHolder extends RecyclerView.ViewHolder implements VideoLoadMvpView {

    public final static int TYPE_URL = 1;
    public final static int TYPE_IMAGE = 2;
    public final static int TYPE_VIDEO = 3;

    public int viewType;

    public ImageView headIv;
    public TextView nameTv;
    public TextView urlTipTv;
    /** 动态的内容 */
    public ExpandTextView contentTv;
    public TextView timeTv;
    public TextView deleteBtn;
    public ImageView snsBtn;
    public PraiseListView praiseListView;

    public LinearLayout digCommentBody;
    public View digLine;

    public CommentListView commentList;
    public SnsPopupWindow snsPopupWindow;

    public CircleViewHolder(View itemView, int viewType) {
        super(itemView);
        this.viewType = viewType;

        ViewStub viewStub = itemView.findViewById(R.id.viewStub);

        initSubView(viewType, viewStub);

        headIv = itemView.findViewById(R.id.headIv);
        nameTv = itemView.findViewById(R.id.nameTv);
        digLine = itemView.findViewById(R.id.lin_dig);

        contentTv = itemView.findViewById(R.id.contentTv);
        urlTipTv = itemView.findViewById(R.id.urlTipTv);
        timeTv = itemView.findViewById(R.id.timeTv);
        deleteBtn = itemView.findViewById(R.id.deleteBtn);
        snsBtn = itemView.findViewById(R.id.snsBtn);
        praiseListView = itemView.findViewById(R.id.praiseListView);

        digCommentBody = itemView.findViewById(R.id.digCommentBody);
        commentList = itemView.findViewById(R.id.commentList);

        snsPopupWindow = new SnsPopupWindow(itemView.getContext());

    }

    public abstract void initSubView(int viewType, ViewStub viewStub);

    @Override
    public TextureVideoView getVideoView() {
        return null;
    }

    @Override
    public void videoBeginning() {

    }

    @Override
    public void videoStopped() {

    }

    @Override
    public void videoPrepared(MediaPlayer player) {

    }

    @Override
    public void videoResourceReady(String videoPath) {

    }
}
