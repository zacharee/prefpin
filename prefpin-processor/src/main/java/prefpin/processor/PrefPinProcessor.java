package prefpin.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import prefpin.BindPref;
import prefpin.OnPrefChange;
import prefpin.OnPrefClick;

@AutoService(Processor.class)
public class PrefPinProcessor extends AbstractProcessor {

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> types = new LinkedHashSet<>();
    types.add(BindPref.class.getCanonicalName());
    types.add(OnPrefClick.class.getCanonicalName());
    types.add(OnPrefChange.class.getCanonicalName());
    return types;
  }

  @Override public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnvironment) {

    Map<TypeElement, Set<Element>> bindingMap = new LinkedHashMap<>();
    parsePreferenceBinding(roundEnvironment, bindingMap, BindPref.class);
    parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefClick.class);
    parsePreferenceBinding(roundEnvironment, bindingMap, OnPrefChange.class);

    if (!bindingMap.isEmpty()) {
      for (Map.Entry<TypeElement, Set<Element>> entry : bindingMap.entrySet()) {
        String targetClassName = entry.getKey().getQualifiedName().toString();

        try {
          writeBinding(targetClassName, entry.getValue());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return true;
  }

  private void parsePreferenceBinding(RoundEnvironment roundEnv,
      Map<TypeElement, Set<Element>> bindingMap, Class<? extends Annotation> annotation) {
    for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
      if (element.getModifiers().contains(Modifier.PRIVATE)) {
        processingEnv.getMessager()
            .printMessage(Diagnostic.Kind.ERROR,
                "Binding annotation can not applied to private fields or methods.", element);
      }

      TypeElement targetPrefFragment = (TypeElement) element.getEnclosingElement();
      if (bindingMap.containsKey(targetPrefFragment)) {
        bindingMap.get(targetPrefFragment).add(element);
      } else {
        Set<Element> fields = new LinkedHashSet<>();
        fields.add(element);
        bindingMap.put(targetPrefFragment, fields);
      }
    }
  }

  /**
   * Checks whether a field is annotated correctly with {@link prefpin.BindPref} and prints error
   * message for incorrect one.
   */
  private boolean checkPreferenceAnnotation(Element element) {
    if (isSubtypeOfType(element.asType(), "android.preference.Preference")) {
      return true;
    } else {
      processingEnv.getMessager()
          .printMessage(Diagnostic.Kind.ERROR,
              "@PrefPin must be applied to Preference or its subclass fields", element);
      return false;
    }
  }

  private void writeBinding(String targetClassName, Set<Element> annotationFields)
      throws IOException {
    String packageName = null;
    int lastDot = targetClassName.lastIndexOf('.');
    if (lastDot > 0) {
      packageName = targetClassName.substring(0, lastDot);
    }

    String targetSimpleClassName = targetClassName.substring(lastDot + 1);
    String bindingClassName = targetClassName + "_PrefBinding";
    String bindingSimpleClassName = bindingClassName.substring(lastDot + 1);

    ClassName targetClass = ClassName.get(packageName, targetSimpleClassName);

    FieldSpec targetField = FieldSpec.builder(targetClass, "target", Modifier.PRIVATE).build();

    TypeSpec binding = TypeSpec.classBuilder(bindingSimpleClassName)
        .addModifiers(Modifier.PUBLIC)
        .addField(targetField)
        .addMethod(buildConstructor(targetClass, annotationFields))
        .build();

    JavaFile javaFile = JavaFile.builder(packageName, binding).build();

    javaFile.writeTo(processingEnv.getFiler());
  }

  private MethodSpec buildConstructor(ClassName targetClass, Set<Element> annotationFields) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(targetClass, "target", Modifier.FINAL)
        .addStatement("this.target = target");

    for (Element element : annotationFields) {
      buildFieldBinding(constructorBuilder, element);
      buildOnClickBinding(constructorBuilder, element);
      buildOnChangeBinding(constructorBuilder, element);
    }

    return constructorBuilder.build();
  }

  private void buildFieldBinding(MethodSpec.Builder constructorBuilder, Element element) {
    BindPref bindPref = element.getAnnotation(BindPref.class);
    if (bindPref != null) {
      int resourceId = bindPref.value();

      constructorBuilder.addStatement("target."
          + element.getSimpleName()
          + " = "
          + "("
          + element.asType().toString()
          + ")"
          + "target.findPreference(target.getString("
          + resourceId
          + "))");
    }
  }

  private void buildOnClickBinding(MethodSpec.Builder constructorBuilder, Element element) {
    OnPrefClick onPrefClick = element.getAnnotation(OnPrefClick.class);
    if (onPrefClick != null) {
      int resourceId = onPrefClick.value();

      constructorBuilder.addStatement("target.findPreference(target.getString("
          + resourceId
          + "))"
          + ".setOnPreferenceClickListener(new android.preference.Preference.OnPreferenceClickListener(){\n"
          + "@Override public boolean onPreferenceClick(android.preference.Preference preference) {\n"
          + "\t\ttarget."
          + element.getSimpleName()
          + "(preference);\n"
          + "\t\treturn true;\n"
          + "\t}\n"
          + "})");
    }
  }

  private void buildOnChangeBinding(MethodSpec.Builder constructorBuilder, Element element) {
    OnPrefChange onPrefChange = element.getAnnotation(OnPrefChange.class);
    if (onPrefChange != null) {
      int resourceId = onPrefChange.value();

      constructorBuilder.addStatement("target.findPreference(target.getString("
          + resourceId
          + "))"
          + ".setOnPreferenceChangeListener(new android.preference.Preference.OnPreferenceChangeListener() {\n"
          + "@Override public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {\n"
          + "\t\ttarget."
          + element.getSimpleName()
          + "(preference, newValue);\n"
          + "\t\treturn true;\n"
          + "\t}\n"
          + "})");
    }
  }

  private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
    if (isTypeEqual(typeMirror, otherType)) {
      return true;
    }
    if (typeMirror.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declaredType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
    if (typeArguments.size() > 0) {
      StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
      typeString.append('<');
      for (int i = 0; i < typeArguments.size(); i++) {
        if (i > 0) {
          typeString.append(',');
        }
        typeString.append('?');
      }
      typeString.append('>');
      if (typeString.toString().equals(otherType)) {
        return true;
      }
    }
    Element element = declaredType.asElement();
    if (!(element instanceof TypeElement)) {
      return false;
    }
    TypeElement typeElement = (TypeElement) element;
    TypeMirror superType = typeElement.getSuperclass();
    if (isSubtypeOfType(superType, otherType)) {
      return true;
    }
    for (TypeMirror interfaceType : typeElement.getInterfaces()) {
      if (isSubtypeOfType(interfaceType, otherType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
    return otherType.equals(typeMirror.toString());
  }
}
