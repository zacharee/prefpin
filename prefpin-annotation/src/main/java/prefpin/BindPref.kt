package prefpin

import androidx.annotation.StringRes

/**
 * Bind a field to the preference for the specified key.
 * type.
 * <pre>`
 * @BindView(R.string.pref_name_key) Preference namePreference;
`</pre> *
 */
@Retention
@Target(AnnotationTarget.FIELD)
annotation class BindPref(
        /**
         * Preference's key which is stored as a string resource.
         */
        @StringRes val value: Int
)
