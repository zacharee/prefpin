package prefpin

/**
 * Binds a method to the OnPreferenceChange handler of a preference for the specific key.
 */
@Retention
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class OnPrefChangeString(
        vararg val value: String,
        val clazz: String = "androidx.preference.Preference"
)
