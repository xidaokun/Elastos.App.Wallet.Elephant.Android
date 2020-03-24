package com.breadwallet.presenter.activities.crc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.breadwallet.R
import com.breadwallet.presenter.customviews.FlowLayout
import com.breadwallet.tools.adapter.VoteNodeAdapter
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.StringUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.vote.CityEntity
import com.breadwallet.vote.CrcRankEntity
import com.breadwallet.wallet.wallets.ela.ElaDataSource
import com.elastos.jni.UriFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal


class CrcVoteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crc_vote_layout)

        initView()
        initLinster()
        initData()
    }

    private val uriFactory: UriFactory = UriFactory()
    fun initView() {
        if (intent!=null) {
            if (!StringUtil.isNullOrEmpty(intent.action) && intent.action==Intent.ACTION_VIEW) {
                uriFactory.parse(intent.data.toString())
            } else {
                uriFactory.parse(intent.getStringExtra("crc_scheme_uri"))
            }
        }

    }

    fun initLinster() {
        findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.vote_cancle_btn).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.vote_confirm_btn).setOnClickListener {

        }
    }

    fun initData() {
        val dposNodesTv = findViewById<TextView>(R.id.dpos_vote_nodes_tv)
        val crcNodesTv = findViewById<TextView>(R.id.crc_vote_nodes_tv)
        val balance = BRSharedPrefs.getCachedBalance(this, "ELA")

        //total vote counts
        findViewById<TextView>(R.id.votes_counts).text = balance.subtract(BigDecimal(0.0001)).toLong().toString()

        val dposNodes = Utils.spliteByComma(BRSharedPrefs.getCandidate(this).trim())
        val crcNodes = Utils.spliteByComma(uriFactory.candidates.trim())

        // dpos vote counts
        if(dposNodes.count() <= 0 ) {
            dposNodesTv.visibility = View.GONE
            findViewById<View>(R.id.publickeys_title).visibility = View.GONE
            findViewById<View>(R.id.vote_paste_tv).visibility = View.GONE
            findViewById<View>(R.id.publickeys_lv).visibility = View.GONE
        } else {
            dposNodesTv.text = String.format(getString(R.string.crc_vote_dpos_nodes), dposNodes.count())
            val producers = ElaDataSource.getInstance(this).getProducersByPK(dposNodes)
            findViewById<ListView>(R.id.publickeys_lv).adapter = VoteNodeAdapter(this, producers)
        }
        // crc counts
        crcNodesTv.text = String.format(getString(R.string.crc_vote_crc_nodes), crcNodes.count())

        //balance
        findViewById<TextView>(R.id.vote_ela_balance).text = String.format(getString(R.string.vote_balance), balance.toString())
        // fee
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            try {
                val fee = ElaDataSource.getInstance(this).nodeFee
                val feeStr = BigDecimal(fee).divide(BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toString()
                BRExecutor.getInstance().forMainThreadTasks().execute {
                    findViewById<TextView>(R.id.vote_text_hint1).setText(String.format(getString(R.string.vote_hint), feeStr)) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            var crcRankEntitys = ElaDataSource.getInstance(this).crcWithRank
            val cityStr = readCities("/city/cities")
            val cities = Gson().fromJson<List<CityEntity>>(cityStr, object : TypeToken<List<CityEntity>>() {
            }.type)

            for (crcEntity in crcRankEntitys) {
                for(cityEntity in cities) {
                    if(cityEntity.code == crcEntity.Location) {
                        //TODO 中英文适配
                        crcEntity.Area = cityEntity.en
                    }
                }
            }

            BRExecutor.getInstance().forMainThreadTasks().execute {
                val mFlowLayout = findViewById<FlowLayout>(R.id.numbers_float_layout)
                mFlowLayout.setAlignByCenter(FlowLayout.AlienState.CENTER)
                mFlowLayout.setAdapter(crcRankEntitys, R.layout.crc_member_layout, object : FlowLayout.ItemView<CrcRankEntity>() {
                    internal fun getCover(item: CrcRankEntity, holder: FlowLayout.ViewHolder, inflate: View, position: Int) {
                        val content = item.Nickname + "|" + item.Area
                        holder.setText(R.id.tv_label_name, content)
                    }
                })
            }
        }

    }

    fun readCities(filename : String): String {
        val inputStream = assets.open(filename)
        val size = inputStream.available()
        var buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        return String(buffer)
    }


}