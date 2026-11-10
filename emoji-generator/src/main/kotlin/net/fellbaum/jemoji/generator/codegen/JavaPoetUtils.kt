package net.fellbaum.jemoji.generator.codegen

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import net.fellbaum.jemoji.generator.writeTextIfChanged
import java.io.File
import javax.lang.model.element.Modifier

const val JEMOJI_PACKAGE = "net.fellbaum.jemoji"
const val JEMOJI_GENERATED_PACKAGE = "$JEMOJI_PACKAGE.generated"
const val JEMOJI_INTERNAL_PACKAGE = "$JEMOJI_PACKAGE.internal"

/** Adds `@Generated` and writes the type as `<package>.<name>` into [outputDir]. */
fun TypeSpec.Builder.writeJavaFile(outputDir: File, packageName: String = JEMOJI_PACKAGE) {
    addAnnotation(
        AnnotationSpec.builder(ClassName.get("javax.annotation.processing", "Generated"))
            .addMember("value", $$"$S", "emoji-generator")
            .build()
    )
    val type = build()
    val javaFile = JavaFile.builder(packageName, type)
        .indent("    ")
        .skipJavaLangImports(true)
        .build()
    outputDir.resolve(packageName.replace('.', '/')).resolve("${type.name()}.java").writeTextIfChanged(javaFile.toString())
}

fun TypeSpec.Builder.suppressUnusedWarnings(): TypeSpec.Builder =
    addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", $$"$S", "unused").build())

fun jemojiType(simpleName: String): ClassName = ClassName.get(JEMOJI_PACKAGE, simpleName)

fun parameterizedType(container: Class<*>, element: Class<*>): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(container), ClassName.get(element))

fun parameterizedType(container: Class<*>, element: ClassName): ParameterizedTypeName =
    ParameterizedTypeName.get(ClassName.get(container), element)

/** `public <type> <name>() { return <field>; }` with the usual "Gets ..." javadoc. */
fun getter(
    name: String,
    returnType: TypeName,
    field: String,
    javadoc: String,
    vararg modifiers: Modifier = arrayOf(Modifier.PUBLIC),
): MethodSpec = MethodSpec.methodBuilder(name)
    .addJavadoc(javadoc)
    .addModifiers(*modifiers)
    .returns(returnType)
    .addStatement($$"return $N", field)
    .build()

fun getter(name: String, returnType: Class<*>, field: String, javadoc: String): MethodSpec =
    getter(name, TypeName.get(returnType), field, javadoc)
