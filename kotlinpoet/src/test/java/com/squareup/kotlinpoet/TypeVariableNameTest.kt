/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.TypeVariableName.Companion.NULLABLE_ANY_LIST
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TypeVariableNameTest {
  @Test fun nullableAnyIsImplicitBound() {
    val typeVariableName = TypeVariableName("T")
    assertThat(typeVariableName.bounds).containsExactly(NULLABLE_ANY)
  }

  @Test fun oneTypeVariableNoBounds() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T"))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun <T> foo(): T? = null
      |
      """.trimMargin()
    )
  }

  @Test fun twoTypeVariablesNoBounds() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T"))
      .addTypeVariable(TypeVariableName("U"))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun <T, U> foo(): T? = null
      |
      """.trimMargin()
    )
  }

  @Test fun oneTypeVariableOneBound() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T", Serializable::class))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun <T : java.io.Serializable> foo(): T? = null
      |
      """.trimMargin()
    )
  }

  @Test fun twoTypeVariablesOneBoundEach() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T", Serializable::class))
      .addTypeVariable(TypeVariableName("U", Runnable::class))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun <T : java.io.Serializable, U : java.lang.Runnable> foo(): T? = null
      |
      """.trimMargin()
    )
  }

  @Test fun oneTypeVariableTwoBounds() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public fun <T> foo(): T? where T : java.io.Serializable, T : java.lang.Runnable = null
      |
      """.trimMargin()
    )
  }

  @Test fun twoTypeVariablesTwoBoundsEach() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
      .addTypeVariable(TypeVariableName("U", Comparator::class, Cloneable::class))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun <T, U> foo(): " +
        "T? where T : java.io.Serializable, T : java.lang.Runnable, " +
        "U : java.util.Comparator, U : kotlin.Cloneable = null\n"
    )
  }

  @Test fun threeTypeVariables() {
    val funSpec = FunSpec.builder("foo")
      .addTypeVariable(TypeVariableName("T", Serializable::class, Runnable::class))
      .addTypeVariable(TypeVariableName("U", Cloneable::class))
      .addTypeVariable(TypeVariableName("V"))
      .returns(TypeVariableName("T").copy(nullable = true))
      .addStatement("return null")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      "public fun <T, U : kotlin.Cloneable, V> foo(): " +
        "T? where T : java.io.Serializable, T : java.lang.Runnable = null\n"
    )
  }

  @Test fun addingBoundsRemovesImplicitBound() {
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(TypeVariableName("T").copy(bounds = listOf(Number::class.asTypeName())))
      .build()
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<T : kotlin.Number>
      |
      """.trimMargin()
    )
  }

  @Test fun inVariance() {
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.IN))
      .build()
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<in E : kotlin.Number>
      |
      """.trimMargin()
    )
  }

  @Test fun outVariance() {
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(TypeVariableName("E", Number::class, variance = KModifier.OUT))
      .build()
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<out E : kotlin.Number>
      |
      """.trimMargin()
    )
  }

  @Test fun invalidVariance() {
    assertThrows<IllegalArgumentException> {
      TypeVariableName("E", KModifier.FINAL)
    }
  }

  @Test fun reified() {
    val funSpec = FunSpec.builder("printMembers")
      .addModifiers(KModifier.INLINE)
      .addTypeVariable(TypeVariableName("T").copy(reified = true))
      .addStatement("println(T::class.members)")
      .build()
    assertThat(funSpec.toString()).isEqualTo(
      """
      |public inline fun <reified T> printMembers(): kotlin.Unit {
      |  println(T::class.members)
      |}
      |
      """.trimMargin()
    )
  }

  @Test fun anyBoundsIsLegal() {
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(TypeVariableName("E", ANY))
      .build()
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<E : kotlin.Any>
      |
      """.trimMargin()
    )
  }

  @Test fun filterOutNullableAnyBounds() {
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(TypeVariableName("E", NULLABLE_ANY))
      .build()
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<E>
      |
      """.trimMargin()
    )
  }

  @Test fun emptyBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName("E", bounds = *emptyArray<TypeName>())
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(typeVariable)
      .build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<E>
      |
      """.trimMargin()
    )
  }

  @Test fun noBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName("E")
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(typeVariable)
      .build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<E>
      |
      """.trimMargin()
    )
  }

  @Test fun genericClassNoBoundsShouldDefaultToAnyNullable() {
    val typeVariable = TypeVariableName.get(GenericClass::class.java.typeParameters[0])
    val typeSpec = TypeSpec.classBuilder("Taco")
      .addTypeVariable(typeVariable)
      .build()
    assertThat(typeVariable.bounds).isEqualTo(NULLABLE_ANY_LIST)
    assertThat(typeSpec.toString()).isEqualTo(
      """
      |public class Taco<T>
      |
      """.trimMargin()
    )
  }

  class GenericClass<T>

  @Test
  fun definitelyNonNullableType_simple() {
    val typeName = TypeVariableName("T").copy()
    assertThat(typeName.toString()).isEqualTo("T & Any")
  }

  @Test
  fun definitelyNonNullableType_errorWhenNonNullableMadeNullable() {
    assertFailsWith<IllegalArgumentException> {
      TypeVariableName("T").copy()
        .copy(nullable = true)
    }
  }

  @Test
  fun definitelyNonNullableType_errorWhenNullableMadeNonNullable() {
    assertFailsWith<IllegalArgumentException> {
      (TypeVariableName("T").copy(nullable = true) as TypeVariableName)
        .copy()
    }
  }

  @Test
  fun definitelyNonNullableType_errorWhenNonNullAny() {
    assertFailsWith<IllegalArgumentException> {
      TypeVariableName("T", listOf(ANY))
        .copy()
    }
  }
}
