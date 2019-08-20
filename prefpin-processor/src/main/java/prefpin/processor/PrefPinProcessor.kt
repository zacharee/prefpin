package prefpin.processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*

import java.io.IOException
import java.util.LinkedHashMap
import java.util.LinkedHashSet

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

import prefpin.BindPref
import prefpin.BindPrefString
import prefpin.OnPrefChange
import prefpin.OnPrefChangeString
import prefpin.OnPrefClick
import prefpin.OnPrefClickString

@AutoService(Processor::class)
class PrefPinProcessor : AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): Set<String> {
        val types = LinkedHashSet<String>()
        types.add(BindPref::class.java.canonicalName)
        types.add(OnPrefClick::class.java.canonicalName)
        types.add(OnPrefChange::class.java.canonicalName)
        types.add(BindPrefString::class.java.canonicalName)
        types.add(OnPrefClickString::class.java.canonicalName)
        types.add(OnPrefChangeString::class.java.canonicalName)
        return types
    }

    override fun process(annotations: Set<TypeElement>,
                         roundEnvironment: RoundEnvironment): Boolean {

        val bindingMap = LinkedHashMap<TypeElement, LinkedHashSet<Element>>()
        parsePreferenceBinding(roundEnvironment, bindingMap, BindPref::class.java)
        parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefClick::class.java)
        parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefChange::class.java)
        parsePreferenceBinding(roundEnvironment, bindingMap, BindPrefString::class.java)
        parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefClickString::class.java)
        parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefChangeString::class.java)

        if (bindingMap.isNotEmpty()) {
            for ((key, value) in bindingMap) {
                val targetClassName = key.qualifiedName.toString()

                try {
                    writeBinding(targetClassName, value)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }

        return true
    }

    private fun parsePreferenceBinding(roundEnv: RoundEnvironment,
                                       bindingMap: MutableMap<TypeElement, LinkedHashSet<Element>>, annotation: Class<out Annotation>) {
        for (element in roundEnv.getElementsAnnotatedWith(annotation)) {
//            if (element.modifiers.contains(Modifier.PRIVATE)) {
//                processingEnv.messager
//                        .printMessage(Diagnostic.Kind.ERROR,
//                                "Binding annotation can not applied to private fields or methods.", element)
//            }

            if (annotation == BindPref::class.java || annotation == BindPrefString::class.java) {
                checkPreferenceAnnotation(element)
            }

            val targetPrefFragment = element.enclosingElement as TypeElement
            if (bindingMap.containsKey(targetPrefFragment)) {
                bindingMap[targetPrefFragment]?.add(element)
            } else {
                val fields = LinkedHashSet<Element>()
                fields.add(element)
                bindingMap[targetPrefFragment] = fields
            }
        }
    }

    /**
     * Checks whether a field is annotated correctly with [prefpin.BindPref] and prints error
     * message for incorrect one.
     */
    private fun checkPreferenceAnnotation(element: Element): Boolean {
        return if (isSubtypeOfType(element.asType(), "androidx.preference.Preference")) {
            true
        } else {
            processingEnv.messager
                    .printMessage(Diagnostic.Kind.ERROR,
                            "@PrefPin must be applied to Preference or its subclass fields", element)
            println("Bad Annotation")
            false
        }
    }

    @Throws(IOException::class)
    private fun writeBinding(targetClassName: String, annotationFields: Set<Element>) {
        var packageName: String? = null
        val lastDot = targetClassName.lastIndexOf('.')
        if (lastDot > 0) {
            packageName = targetClassName.substring(0, lastDot)
        }

        val targetSimpleClassName = targetClassName.substring(lastDot + 1)
        val bindingClassName = targetClassName + BINDING_CLASS_NAME_POSTFIX
        val bindingSimpleClassName = bindingClassName.substring(lastDot + 1)

        val targetClass = ClassName(packageName!!, targetSimpleClassName)

        val binding = TypeSpec.classBuilder(bindingSimpleClassName)
                .addModifiers(KModifier.PUBLIC)
                .addFunction(buildConstructor(targetClass, annotationFields))
                .build()

        val file = FileSpec.builder(packageName, bindingSimpleClassName)
                .addType(binding)
                .build()

        file.writeTo(processingEnv.filer)
    }

    private fun buildConstructor(targetClass: ClassName, annotationFields: Set<Element>): FunSpec {
        val constructorBuilder = FunSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(KModifier.PUBLIC)
                .addParameter("target", targetClass)

        for (element in annotationFields) {
            buildFieldBinding(constructorBuilder, element)
            buildOnClickBinding(constructorBuilder, element)
            buildOnChangeBinding(constructorBuilder, element)
        }

        return constructorBuilder.build()
    }

    private fun buildFieldBinding(constructorBuilder: FunSpec.Builder, element: Element) {
        val bindPref = element.getAnnotation(BindPref::class.java)
        if (bindPref != null) {
            val resourceId = bindPref.value

            constructorBuilder.addStatement(
                    "target.%L = target.findPreference<%T>(target.getString(%L)) as %T",
                    element.simpleName,
                    element.asType(),
                    resourceId,
                    element.asType()
            )
        }

        val bindPrefString = element.getAnnotation(BindPrefString::class.java)
        if (bindPrefString != null) {
            val key = bindPrefString.value

            constructorBuilder.addStatement(
                    "target.%L = target.findPreference<%T>(\"%L\") as %T",
                    element.simpleName,
                    element.asType(),
                    key,
                    element.asType()
            )
        }
    }

    private fun buildOnClickBinding(constructorBuilder: FunSpec.Builder, element: Element) {
        val onPrefClick = element.getAnnotation(OnPrefClick::class.java)
        if (onPrefClick != null) {
            val resourceIds = onPrefClick.value

            for (resourceId in resourceIds) {
                constructorBuilder.addStatement(
                        "target.findPreference<%T>(target.getString(%L))" +
                        "!!.setOnPreferenceClickListener(object : %T {\n" +
                                "override fun onPreferenceClick(preference: %T): Boolean {\n" +
                                        "\t\ttarget.%L(preference)\n" +
                                        "\t\treturn true\n" +
                                "\t}\n" +
                        "})", PREFERENCE, resourceId, CLICK_LISTENER, PREFERENCE, element.simpleName
                )
            }
        }

        val onPrefClickString = element.getAnnotation(OnPrefClickString::class.java)
        if (onPrefClickString != null) {
            val keys = onPrefClickString.value

            for (key in keys) {
                constructorBuilder.addStatement(
                        "target.findPreference<%T>(\"%L\")" +
                        "!!.setOnPreferenceClickListener(object : %T {\n" +
                                "override fun onPreferenceClick(preference: %T): Boolean {\n" +
                                "\t\ttarget.%L(preference)\n" +
                                "\t\treturn true\n" +
                                "\t}\n" +
                                "})", PREFERENCE, key, CLICK_LISTENER, PREFERENCE, element.simpleName
                )
            }
        }
    }

    private fun buildOnChangeBinding(constructorBuilder: FunSpec.Builder, element: Element) {
        val onPrefChange = element.getAnnotation(OnPrefChange::class.java)
        if (onPrefChange != null) {
            val resourceIds = onPrefChange.value

            for (resourceId in resourceIds) {
                constructorBuilder.addStatement(
                        "target.findPreference<%T>(target.getString(%L))" +
                        "!!.setOnPreferenceChangeListener(object : %T {\n" +
                                "override fun onPreferenceChange(preference: %T, newValue: Any): Boolean {\n" +
                                        "\t\ttarget.%L(preference, newValue)\n" +
                                        "\t\treturn true\n" +
                                "\t}\n" +
                        "})", PREFERENCE, resourceId, CHANGE_LISTENER, PREFERENCE, element.simpleName
                )
            }
        }

        val onPrefChangeString = element.getAnnotation(OnPrefChangeString::class.java)
        if (onPrefChangeString != null) {
            val keys = onPrefChangeString.value

            for (key in keys) {
                constructorBuilder.addStatement(
                        "target.findPreference<%T>(\"%L\")" +
                        "!!.setOnPreferenceChangeListener(object : %T {\n" +
                                "override fun onPreferenceChange(preference: %T, newValue: Any): Boolean {\n" +
                                "\t\ttarget.%L(preference, newValue)\n" +
                                "\t\treturn true\n" +
                                "\t}\n" +
                                "})", PREFERENCE, key, CHANGE_LISTENER, PREFERENCE, element.simpleName
                )
            }
        }
    }

    private fun isSubtypeOfType(typeMirror: TypeMirror, otherType: String): Boolean {
        if (isTypeEqual(typeMirror, otherType)) {
            return true
        }
        if (typeMirror.kind != TypeKind.DECLARED) {
            return false
        }
        val declaredType = typeMirror as DeclaredType
        val typeArguments = declaredType.typeArguments
        if (typeArguments.size > 0) {
            val typeString = StringBuilder(declaredType.asElement().toString())
            typeString.append('<')
            for (i in typeArguments.indices) {
                if (i > 0) {
                    typeString.append(',')
                }
                typeString.append('?')
            }
            typeString.append('>')
            if (typeString.toString() == otherType) {
                return true
            }
        }
        val element = declaredType.asElement() as? TypeElement ?: return false
        val superType = element.superclass
        if (isSubtypeOfType(superType, otherType)) {
            return true
        }
        for (interfaceType in element.interfaces) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true
            }
        }
        return false
    }

    companion object {
        const val BINDING_CLASS_NAME_POSTFIX = "_PrefBinding"
        private val PREFERENCE = ClassName("androidx.preference", "Preference")
        private val CLICK_LISTENER = ClassName("androidx.preference.Preference", "OnPreferenceClickListener")
        private val CHANGE_LISTENER = ClassName("androidx.preference.Preference", "OnPreferenceChangeListener")
        private val UI_THREAD = ClassName("androidx.annotation", "UiThread")

        private fun isTypeEqual(typeMirror: TypeMirror, otherType: String): Boolean {
            return otherType == typeMirror.toString()
        }
    }
}
