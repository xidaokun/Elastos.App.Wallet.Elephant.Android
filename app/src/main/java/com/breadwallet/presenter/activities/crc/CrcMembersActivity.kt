package com.breadwallet.presenter.activities.crc

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SimpleAdapter
import com.breadwallet.R
import com.breadwallet.presenter.customviews.MaxHeightLv
import com.breadwallet.tools.util.Utils
import com.elastos.jni.UriFactory


class CrcMembersActivity : AppCompatActivity() {

    private val uriFactory: UriFactory = UriFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crc_members_layout)

        initView()
        initData()
    }

    fun initView() {
        uriFactory.parse(intent.getStringExtra("crc_scheme_uri"))
    }

    fun initData() {
        val crcNodes = Utils.spliteByComma(uriFactory.candidates)
        val crcRankEntities = CrcDataSource.getInstance(this).getMembersByIds(crcNodes)
        val data = ArrayList<Map<String, Any>>()
        for(crcEntity in crcRankEntities) {
            val item = HashMap<String, Any>()
            item["content"] = crcEntity.Nickname + "|" + crcEntity.Area + "|" + crcEntity.Votes
            data.add(item)
        }
        findViewById<MaxHeightLv>(R.id.council_lv).also {
            with(it) {
                adapter = SimpleAdapter(this@CrcMembersActivity, data, R.layout.crc_member_item_layout, arrayOf("content"), intArrayOf(R.id.crc_members_detail))

            }
        }
    }
}
