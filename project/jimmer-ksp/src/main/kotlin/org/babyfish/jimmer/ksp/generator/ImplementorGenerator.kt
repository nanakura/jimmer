package org.babyfish.jimmer.ksp.generator

import com.squareup.kotlinpoet.*
import org.babyfish.jimmer.ksp.meta.ImmutableType
import kotlin.reflect.KClass

class ImplementorGenerator(
    private val type: ImmutableType,
    private val parent: TypeSpec.Builder
) {

    fun generate() {
        parent.addType(
            TypeSpec
                .classBuilder(IMPLEMENTOR)
                .addModifiers(KModifier.PRIVATE, KModifier.ABSTRACT)
                .addSuperinterface(type.className)
                .addSuperinterface(IMMUTABLE_SPI_CLASS_NAME)
                .apply {
                    addGetFun(Int::class)
                    addGetFun(String::class)
                    addTypeFun()
                    addToStringFun()
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.addGetFun(
        argType: KClass<*>
    ) {
        addFunction(
            FunSpec
                .builder("__get")
                .addParameter("prop", if (argType == Int::class) INT else STRING)
                .addModifiers(KModifier.OVERRIDE)
                .returns(ANY.copy(nullable = true))
                .addCode(
                    CodeBlock
                        .builder()
                        .beginControlFlow("return when (prop)")
                        .apply {
                            for (prop in type.properties.values) {
                                if (argType == Int::class) {
                                    addStatement("%L -> %L", prop.id, prop.name)
                                } else {
                                    addStatement("%S -> %L", prop.name, prop.name)
                                }
                            }
                            addElseBranchForProp(argType)
                        }
                        .endControlFlow()
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addTypeFun() {
        addFunction(
            FunSpec
                .builder("__type")
                .addModifiers(KModifier.OVERRIDE)
                .returns(IMMUTABLE_TYPE_CLASS_NAME)
                .addCode("return %T.type", type.draftClassName(PRODUCER))
                .build()
        )
    }

    private fun TypeSpec.Builder.addToStringFun() {
        addFunction(
            FunSpec
                .builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(STRING)
                .addCode("return %T.toString(this)", IMMUTABLE_OBJECTS_CLASS_NAME)
                .build()
        )
    }
}