package prefpin.sample

import android.os.Bundle
import android.preference.PreferenceActivity

class AboutActivity : PreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, AboutFragment())
                .commit()
    }
}
