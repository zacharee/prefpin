package prefpin

import androidx.annotation.StringRes

/**
 * Binds a method to the OnPreferenceChange handler of a preference for the specific key.
 */
@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class OnPrefChange(
        @StringRes vararg val value: Int
)
