package prefpin

import androidx.annotation.UiThread
import androidx.preference.PreferenceFragmentCompat

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * Field and method binding for Android [preference][android.preference.Preference].
 *
 *
 * <pre>`
 * public class SettingFragment extends PreferenceFragment{
 * @BindPref(R.string.pref_general_key) Preference generalPreference;
 * @BindPref(R.string.pref_name_key) EditTextPreference namePreference;
 *
 * @Override protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * addPreferencesFromResource(R.xml.settings);
 * PrefPin.bind(this);
 * }
 *
 * @OnPrefClick(R.string.pref_name_key) public void onClick(Preference preference) {
 * }
 *
 * @OnPrefChange(R.string.pref_name_key) public void onPrefChange(Preference
 * preference, Object newObject) {
 * }
 * }
`</pre> *
 */
class PrefPin private constructor() {
    init {
        throw AssertionError("No instance.")
    }

    companion object {
        private val BINDING_CLASS_NAME_POSTFIX = "_PrefBinding"

        @UiThread
        fun bind(target: PreferenceFragmentCompat) {
            createBinding(target)
        }

        private fun createBinding(target: PreferenceFragmentCompat) {
            val constructor = findBindingConstructor(target)
            if (constructor != null) {
                try {
                    constructor.newInstance(target)
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }

            }
        }

        private fun findBindingConstructor(target: PreferenceFragmentCompat): Constructor<*>? {
            val targetClassName = target.javaClass.name
            val bindingClassName = targetClassName + BINDING_CLASS_NAME_POSTFIX
            var constructor: Constructor<*>? = null
            try {
                val bindingClass = target.javaClass.classLoader!!.loadClass(bindingClassName)
                constructor = bindingClass.getConstructor(target.javaClass)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }

            return constructor
        }
    }
}
