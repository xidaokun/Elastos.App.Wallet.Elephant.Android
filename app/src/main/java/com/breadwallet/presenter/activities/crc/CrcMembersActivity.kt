package com.breadwallet.presenter.activities.crc

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SimpleAdapter
import android.widget.TextView
import com.breadwallet.R
import com.breadwallet.presenter.customviews.MaxHeightLv
import com.breadwallet.tools.util.StringUtil
import com.breadwallet.tools.util.Utils
import com.elastos.jni.UriFactory
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CrcMembersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crc_members_layout)

        initData()
    }

    fun initData() {
        val crcNodes = Utils.spliteByComma(intent.getStringExtra("candidates")?: return)
        val crcRankEntities = CrcDataSource.getInstance(this).queryCrcsByIds(crcNodes)
        CrcDataSource.getInstance(this).updateCrcsArea(crcRankEntities)

        val data = ArrayList<Map<String, Any>>()
        findViewById<TextView>(R.id.council_title).text = String.format(getString(R.string.crc_vote_crc_nodes), crcNodes.count())

        for(crcEntity in crcRankEntities) {
            val item = HashMap<String, Any>()

            val languageCode = Locale.getDefault().language
            if (!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")) {
                item["content"] = crcEntity.Nickname + " | " + crcEntity.AreaZh + " | " + crcEntity.Votes
            } else {
                item["content"] = crcEntity.Nickname + " | " + crcEntity.AreaEn + " | " + crcEntity.Votes
            }

            data.add(item)
        }
        findViewById<MaxHeightLv>(R.id.council_lv).also {
            with(it) {
                adapter = SimpleAdapter(this@CrcMembersActivity, data, R.layout.crc_member_item_layout, arrayOf("content"), intArrayOf(R.id.crc_members_detail))

            }
        }
    }
}
