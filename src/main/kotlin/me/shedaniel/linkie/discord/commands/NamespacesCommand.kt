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

package me.shedaniel.linkie.discord.commands

import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import me.shedaniel.linkie.Namespaces
import me.shedaniel.linkie.discord.CommandBase
import me.shedaniel.linkie.discord.MessageCreator
import me.shedaniel.linkie.discord.basicEmbed
import me.shedaniel.linkie.discord.utils.description
import me.shedaniel.linkie.discord.validateEmpty

object NamespacesCommand : CommandBase {
    override suspend fun execute(event: MessageCreateEvent, message: MessageCreator, prefix: String, user: User, cmd: String, args: MutableList<String>, channel: MessageChannel) {
        args.validateEmpty(prefix, cmd)
        message.reply {
            setTitle("List of Namespaces")
            description = Namespaces.namespaces.keys.joinToString(", ")
            basicEmbed(user)
        }.subscribe()
    }
}