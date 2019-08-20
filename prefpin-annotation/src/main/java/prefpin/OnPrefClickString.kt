package prefpin

/**
 * Binds a method to the OnPreferenceClick handler of a preference for the specific key.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class OnPrefClickString(
        vararg val value: String
)
