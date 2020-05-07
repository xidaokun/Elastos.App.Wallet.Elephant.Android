package com.breadwallet.presenter.activities.crc

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.breadwallet.R
import com.breadwallet.did.DidDataSource
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.customviews.BRButton
import com.breadwallet.presenter.customviews.LoadingDialog
import com.breadwallet.presenter.entities.VoteEntity
import com.breadwallet.presenter.interfaces.BRAuthCompletion
import com.breadwallet.tools.adapter.VoteNodeAdapter
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.AuthManager
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.StringUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.vote.CrcEntity
import com.breadwallet.vote.PayLoadEntity
import com.breadwallet.wallet.wallets.ela.ElaDataSource
import com.breadwallet.wallet.wallets.ela.ElaDataUtils
import com.breadwallet.wallet.wallets.ela.WalletElaManager
import com.breadwallet.wallet.wallets.ela.response.create.ElaOutput
import com.elastos.jni.AuthorizeManager
import com.elastos.jni.UriFactory
import com.google.gson.Gson
import java.math.BigDecimal
import java.util.*


@Suppress("UNREACHABLE_CODE")
class CrcVoteActivity : BRActivity() {

    private var mLoadingDialog: LoadingDialog? = null

    private val uriFactory: UriFactory = UriFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crc_vote_layout)

        if (intent!=null) {
            if (!StringUtil.isNullOrEmpty(intent.action) && intent.action==Intent.ACTION_VIEW) {
                uriFactory.parse(intent.data.toString())
            } else {
                uriFactory.parse(intent.getStringExtra("vote_scheme_uri"))
            }
        }

        val result = ElaDataUtils.checkSchemeUrl(this, uriFactory.host, uriFactory.url)
        if (!StringUtil.isNullOrEmpty(result) && result != "success") {
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mLoadingDialog = LoadingDialog(this, R.style.progressDialog)
        initLinster()
        initData()
    }

    fun initLinster() {
        findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.vote_cancle_btn).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.view_all_members).setOnClickListener {
            val type = uriFactory.host
            UiUtils.startCrcMembersActivity(this, "CrcVoteActivity",
                    if (type=="elacrcvote") uriFactory.candidates
                    else BRSharedPrefs.getCrcCd(this@CrcVoteActivity),

                    if (type=="elacrcvote") uriFactory.votes
                    else BRSharedPrefs.getCrcVotes(this@CrcVoteActivity))
        }

        findViewById<View>(R.id.vote_confirm_btn).setOnClickListener {
//            val balance = BRSharedPrefs.getCachedBalance(this@CrcVoteActivity, "ELA")
//            if (balance.toLong() <= 0) {
//                Toast.makeText(this@CrcVoteActivity, getString(R.string.vote_balance_not_insufficient), Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//            if (mCandidates.size > 36) {
//                Toast.makeText(this@CrcVoteActivity, getString(R.string.beyond_max_vote_node), Toast.LENGTH_SHORT).show()
//                return
//            }
            if (verifyUri()) {
                sendCrcTx()
            }
        }
    }


    fun sendCrcTx() {
        AuthManager.getInstance().authPrompt(this, this.getString(R.string.pin_author_vote), getString(R.string.pin_author_vote_msg), true, false, object : BRAuthCompletion {
            override fun onComplete() {
                try {
                    showDialog()
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(Runnable {

                        val type = uriFactory.host

                        //dpos payload
                        val dposNodes = Utils.spliteByComma(if (type=="eladposvote") uriFactory.candidatePublicKeys else BRSharedPrefs.getDposCd(this@CrcVoteActivity))
                        val address = WalletElaManager.getInstance(this@CrcVoteActivity).address
                        val amout = 0L
                        var publickeys: ArrayList<PayLoadEntity>?
                        if(dposNodes == null) {
                            publickeys = null
                        } else {
                            publickeys = ArrayList()
                            for(dposNode in dposNodes) {
                                val payLoadEntity = PayLoadEntity()
                                payLoadEntity.candidate = dposNode
                                payLoadEntity.value = amout
                                publickeys.add(payLoadEntity)
                            }
                        }

                        //crc payload
                        val crcNodes = Utils.spliteByComma(if (type=="elacrcvote") uriFactory.candidates else BRSharedPrefs.getCrcCd(this@CrcVoteActivity))
                        var crcCandidates: ArrayList<PayLoadEntity>?
                        if(crcNodes == null) {
                            crcCandidates = null
                        } else {
                            val crcEntities = CrcDataSource.getInstance(this@CrcVoteActivity).queryCrcsByIds(crcNodes)
                            crcCandidates = ArrayList()
                            for(i in crcEntities.indices) {
                                val payLoadEntity = PayLoadEntity()
                                payLoadEntity.candidate = crcEntities[i].Did
                                payLoadEntity.value = amout
                                crcCandidates.add(payLoadEntity)
                            }
                        }

                        val transactions = ElaDataSource.getInstance(this@CrcVoteActivity).
                                createElaTx(address, address, amout, "vote", publickeys, crcCandidates) { elaOutput: ElaOutput, candidateCrcs: MutableList<PayLoadEntity>?, publickeys: MutableList<PayLoadEntity>? ->
                                    try {
                                        if(publickeys != null) {
                                            for(i in publickeys.indices) {
                                                publickeys[i].value = elaOutput.amount
                                            }
                                        }

                                        val crcVotes = Utils.spliteByComma(
                                                if (type=="elacrcvote")
                                                    uriFactory.votes
                                                else
                                                    BRSharedPrefs.getCrcVotes(this@CrcVoteActivity))

                                        if(null!=crcVotes && null!=candidateCrcs) {
                                            for(i in crcVotes.indices) {
                                                candidateCrcs[i].value = BigDecimal(crcVotes[i]).multiply(BigDecimal(elaOutput.amount)).divide(BigDecimal(100)).toLong()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                        if (null == transactions) {
                            dismissDialog()
                            finish()
                            return@Runnable
                        }

                        val mRwTxid = ElaDataSource.getInstance(this@CrcVoteActivity).sendElaRawTx(transactions)
                        if (StringUtil.isNullOrEmpty(mRwTxid)) {
                            dismissDialog()
                            finish()
                            return@Runnable
                        }
                        callBackUrl(mRwTxid)
                        callReturnUrl(mRwTxid)
                        BRSharedPrefs.cacheCrcCd(this@CrcVoteActivity, uriFactory.candidates)
                        BRSharedPrefs.cacheCrcVotes(this@CrcVoteActivity, uriFactory.votes)
                        BRSharedPrefs.cacheDposCd(this@CrcVoteActivity, uriFactory.candidatePublicKeys)
                        dismissDialog()
                        finish()
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancel() {
                //nothing
            }
        })
    }

    fun initData() {
        val dposNodesTv = findViewById<TextView>(R.id.dpos_vote_nodes_tv)
        val crcNodesTv = findViewById<TextView>(R.id.crc_vote_nodes_tv)
        val balance = BRSharedPrefs.getCachedBalance(this, "ELA")

        if(balance.toLong() <= 0) findViewById<BRButton>(R.id.vote_confirm_btn).setColor(getColor(R.color.light_gray))

        //total vote counts
        findViewById<TextView>(R.id.votes_counts).text = balance.subtract(BigDecimal(0.0001)).toLong().toString()

        val type = uriFactory.host
        if(type!="eladposvote" && type!="elacrcvote") return
        val dposNodes = Utils.spliteByComma(if (type=="eladposvote") uriFactory.candidatePublicKeys else BRSharedPrefs.getDposCd(this))
        val crcNodes = Utils.spliteByComma(if (type=="elacrcvote") uriFactory.candidates else BRSharedPrefs.getCrcCd(this))

        if(dposNodes==null && crcNodes==null) return

        // dpos vote counts
        if(null==dposNodes || dposNodes.count() <= 0 ) {
            dposNodesTv.visibility = View.GONE
            findViewById<View>(R.id.dpos_vote_title).visibility = View.GONE
            findViewById<View>(R.id.vote_paste_tv).visibility = View.GONE
            findViewById<View>(R.id.dpos_vote_lv).visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.dpos_vote_title).text = String.format(getString(R.string.node_list_title), dposNodes?.size)
            dposNodesTv.text = String.format(getString(R.string.crc_vote_dpos_nodes), dposNodes.count())
            val producers = ElaDataSource.getInstance(this).queryDposProducers(dposNodes)
            findViewById<ListView>(R.id.dpos_vote_lv).adapter = VoteNodeAdapter(this, producers)
        }

        if(null==crcNodes || crcNodes.count()<=0) {
            crcNodesTv.visibility = View.GONE
            findViewById<View>(R.id.second_card).visibility = View.GONE
        } else {
            // crc counts
            crcNodesTv.text = String.format(getString(R.string.crc_vote_crc_nodes), crcNodes.count())
            //crc members lv
            BRExecutor.getInstance().forMainThreadTasks().execute{
                val crcs = CrcDataSource.getInstance(this@CrcVoteActivity).queryCrcsByIds(crcNodes)
                CrcDataSource.getInstance(this@CrcVoteActivity).updateCrcsArea(crcs)
                findViewById<FlowLayout>(R.id.numbers_flow_layout).also {
                    with(it) {
                        setAdapter(
                                crcs,
                                R.layout.crc_member_layout,
                                object : FlowLayout.ItemView<CrcEntity>() {
                                    override fun getCover(item: CrcEntity?, holder: FlowLayout.ViewHolder?, inflate: View?, position: Int) {
//                                        val languageCode = Locale.getDefault().language
//                                        if (!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")) {
//                                            holder?.setText(R.id.tv_label_name, item?.Nickname + " | " + item?.AreaZh)
//                                        } else {
//                                            holder?.setText(R.id.tv_label_name, item?.Nickname + " | " + item?.AreaEn)
//                                        }
                                        holder?.setText(R.id.tv_label_name, item?.Nickname)
                                    }
                                }
                        )
                    }
                }
            }
        }

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
    }

    private fun dismissDialog() {
        runOnUiThread {
            if (!isFinishing)
                mLoadingDialog?.dismiss()
        }
    }


    private fun showDialog() {
        runOnUiThread {
            if (!isFinishing)
                mLoadingDialog?.show()
        }
    }

    private fun verifyUri(): Boolean {
        val did = uriFactory.did
        val appId = uriFactory.appID
        val appName = uriFactory.appName
        val PK = uriFactory.publicKey

        return AuthorizeManager.verify(this, did, PK, appName, appId)
    }

    private fun callReturnUrl(txId: String) {
        if (StringUtil.isNullOrEmpty(txId)) return
        val returnUrl = uriFactory.returnUrl
        if (StringUtil.isNullOrEmpty(returnUrl)) {
//            Toast.makeText(this@CrcVoteActivity, "returnurl is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val url: String
        if (returnUrl.contains("?")) {
            url = "$returnUrl&TXID=$txId"
        } else {
            url = "$returnUrl?TXID=$txId"
        }
        DidDataSource.getInstance(this@CrcVoteActivity).callReturnUrl(url)
    }

    private fun callBackUrl(txid: String) {
        try {
            if (StringUtil.isNullOrEmpty(txid)) return
            val backurl = uriFactory.callbackUrl
            if (StringUtil.isNullOrEmpty(backurl)) return
            val txEntity = VoteEntity()
            txEntity.TXID = txid
            val ret = DidDataSource.getInstance(this).urlPost(backurl, Gson().toJson(txEntity))
        } catch (e: Exception) {
//            Toast.makeText(this@CrcVoteActivity, "callback error", Toast.LENGTH_SHORT)
            e.printStackTrace()
        }

    }

}