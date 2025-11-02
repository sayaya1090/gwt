package dev.sayaya

import dev.sayaya.gwt.GwtTestTaskExtension
import org.gradle.api.Action
import org.gradle.api.tasks.testing.Test

/**
 * Test 태스크에 gwt { } 블록을 제공하는 확장 함수
 */
fun Test.gwt(action: Action<GwtTestTaskExtension>) {
    val extension = extensions.findByName("gwt") as? GwtTestTaskExtension
        ?: extensions.create("gwt", GwtTestTaskExtension::class.java)
    action.execute(extension)
}