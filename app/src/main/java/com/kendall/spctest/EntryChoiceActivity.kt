package com.kendall.spctest

import android.content.Intent
import com.firebase.example.internal.BaseEntryChoiceActivity
import com.firebase.example.internal.Choice

class EntryChoiceActivity : BaseEntryChoiceActivity() {

    override fun getChoices(): List<Choice> {
        return listOf(
            Choice(
                "Kotlin",
                "Run the Firebase Cloud Messaging written in Kotlin.",
                Intent(this, com.kendall.spctest.MainActivity::class.java),
            ),
        )
    }
}
