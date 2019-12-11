package org.moment.lib.widgets.videolist.visibility.scroll;


import org.moment.lib.widgets.videolist.visibility.items.ListItem;

public interface ItemsProvider {

    ListItem getListItem(int position);

    int listItemSize();

}
