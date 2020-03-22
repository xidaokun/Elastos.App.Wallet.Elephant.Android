package com.breadwallet.presenter.activities.crc

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.breadwallet.R

class CrcVoteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crc_vote_layout)


    }


    fun initView() {
        val backBtn = findViewById<View>(R.id.back_button)
        backBtn.setOnClickListener {
            finish()
        }
    }


}