package net.sayaya

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.wisepersist.gradle.plugins.gwt.GwtSuperDev

abstract class GwtTest: GwtSuperDev() {
    @get:Input
    @get:Option(option = "webserverPort", description = "웹서버 포트")
    @get:Optional
    abstract val webserverPort: Property<Int>
}