package prefpin.sample

import android.content.Intent
import android.os.Bundle
import androidx.preference.*

import prefpin.BindPref
import prefpin.OnPrefChange
import prefpin.OnPrefClick
import prefpin.PrefPin

class SettingsFragment : PreferenceFragmentCompat() {
    @BindPref(R.string.pref_edit_key)
    var editPreference: EditTextPreference? = null
    @BindPref(R.string.pref_checkbox_key)
    var checkBoxPreference: CheckBoxPreference? = null
    @BindPref(R.string.pref_switch_key)
    var switchPreference: SwitchPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        PrefPin.bind(this)
    }

    override fun onResume() {
        super.onResume()
        editPreference!!.summary = preferenceManager.sharedPreferences.getString(editPreference!!.key, "")
        checkBoxPreference!!.isChecked = true
        switchPreference!!.isChecked = true
    }

    @OnPrefClick(R.string.pref_about_key)
    fun showGeneral(preference: Preference) {
        val intent = Intent(activity, AboutActivity::class.java)
        startActivity(intent)
    }

    @OnPrefChange(R.string.pref_edit_key, R.string.pref_checkbox_key, R.string.pref_switch_key)
    fun onNameUpdate(preference: Preference, newValue: Any) {
        preference.summary = newValue.toString()
    }
}
