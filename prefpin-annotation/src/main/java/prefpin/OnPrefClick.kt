package prefpin

import androidx.annotation.StringRes
import kotlin.reflect.KClass

/**
 * Binds a method to the OnPreferenceClick handler of a preference for the specific key.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class OnPrefClick(
        @StringRes vararg val value: Int
)
