/*
 * Copyright (c) 2019, 2020, 2021 shedaniel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("ConstantConditionIf")

package me.shedaniel.linkie.discord

import com.soywiz.klock.minutes
import com.soywiz.klock.seconds
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.event.domain.lifecycle.ReadyEvent
import me.shedaniel.linkie.LinkieConfig
import me.shedaniel.linkie.Namespace
import me.shedaniel.linkie.Namespaces
import me.shedaniel.linkie.discord.utils.event
import me.shedaniel.linkie.utils.info
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule
import kotlin.properties.Delegates

val api: DiscordClient by lazy {
    DiscordClientBuilder.create(System.getenv("TOKEN") ?: System.getProperty("linkie.token") ?: throw NullPointerException("Invalid Token: null")).build()
}
val isDebug: Boolean = System.getProperty("linkie-debug") == "true"
var commandMap: CommandMap = CommandMap(CommandHandler, if (isDebug) "@" else "!")
var trickMap: CommandMap = CommandMap(TrickHandler, if (isDebug) "@@" else "!!")
var gateway by Delegates.notNull<GatewayDiscordClient>()

inline fun start(
    config: LinkieConfig,
    setup: () -> Unit,
) {
    if (isDebug)
        info("Linkie Bot (Debug Mode)")
    else info("Linkie Bot")
    Timer().schedule(0, Duration.ofMinutes(1).toMillis()) {
        System.gc()
    }
    gateway = api.login().doOnSuccess {
        it.eventDispatcher.on(ReadyEvent::class.java).subscribe {
            cycle(5.minutes, delay = 5.seconds) {
                gateway.guilds.count().subscribe { size ->
                    info("Serving on $size servers")
                    gateway.updatePresence(ClientPresence.online(ClientActivity.watching("Serving on $size servers"))).subscribe()
                }
            }
        }
    }.block()!!
    Namespaces.init(config)
    // pretendInit(config)
    setup()
    event(commandMap::onMessageCreate)
    event(trickMap::onMessageCreate)
}

private fun pretendInit(config: LinkieConfig) {
    fun registerNamespace(namespace: Namespace) {
        namespace.getDependencies().forEach(::registerNamespace)
        Namespaces.namespaces[namespace.id] = namespace
    }

    Namespaces.config = config
    Namespaces.gameJarProvider = config.gameJarProvider?.let { it(config) }
    config.namespaces.forEach(::registerNamespace)
}
