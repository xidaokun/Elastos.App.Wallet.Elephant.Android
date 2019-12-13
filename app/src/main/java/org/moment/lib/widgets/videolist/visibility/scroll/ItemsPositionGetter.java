package org.moment.lib.widgets.videolist.visibility.scroll;

import android.view.View;

public interface ItemsPositionGetter {
    View getChildAt(int position);

    int indexOfChild(View view);

    int getChildCount();

    int getLastVisiblePosition();

    int getFirstVisiblePosition();
}
