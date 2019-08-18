package prefpin.sample

import android.annotation.SuppressLint
import android.preference.PreferenceActivity
import android.os.Bundle

@SuppressLint("ExportedPreferenceActivity")
class SettingsActivity : PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
    }
}
