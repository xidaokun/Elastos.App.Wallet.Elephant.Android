package org.chat.lib.widget;


import org.chat.lib.entity.BaseIndexPinyinBean;

import java.util.List;

public interface IIndexBarDataHelper {

    IIndexBarDataHelper convert(List<? extends BaseIndexPinyinBean> data);

    IIndexBarDataHelper fillInexTag(List<? extends BaseIndexPinyinBean> data);

    IIndexBarDataHelper sortSourceDatas(List<? extends BaseIndexPinyinBean> datas);

    IIndexBarDataHelper getSortedIndexDatas(List<? extends BaseIndexPinyinBean> sourceDatas, List<String> datas);

}
