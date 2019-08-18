package prefpin.sample

import android.os.Bundle
import android.preference.Preference
import androidx.preference.PreferenceFragmentCompat

import prefpin.BindPref
import prefpin.PrefPin

class AboutFragment : PreferenceFragmentCompat() {
    @BindPref(R.string.pref_author_key)
    internal var authorPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.about, rootKey)
        PrefPin.bind(this)
        authorPreference!!.summary = "@quangctkm9207"
    }
}
