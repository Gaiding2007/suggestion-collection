package com.gaiding.suggestion

import android.app.Application
import com.gaiding.suggestion.data.SessionStore

class SuggestionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionStore.init(this)
    }
}
