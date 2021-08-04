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

package me.shedaniel.linkie.discord.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.interaction.ComponentInteractEvent
import discord4j.core.event.domain.interaction.InteractionCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.core.spec.MessageCreateSpec
import discord4j.core.spec.MessageEditSpec
import discord4j.discordjson.json.ImmutableWebhookMessageEditRequest
import discord4j.discordjson.json.MessageData
import discord4j.rest.util.AllowedMentions
import reactor.core.publisher.Mono

fun MessageChannel.sendMessage(spec: (MessageCreateSpec.Builder) -> Unit): Mono<Message> {
    val protected: MessageCreateSpec.Builder.() -> Unit = {
        allowedMentions(AllowedMentions.suppressAll())
        spec(this)
    }
    return createMessage(protected.build()) ?: Mono.empty()
}

fun MessageChannel.sendMessage(content: String): Mono<Message> = sendMessage {
    it.content(content)
}

fun MessageChannel.sendEmbedMessage(message: Message? = null, spec: EmbedCreateSpec.Builder.() -> Unit): Mono<Message> = sendMessage {
    it.addEmbed(spec.build())
    message?.let { message ->
        it.messageReference(message.id)
    }
}

fun Message.sendEdit(spec: MessageEditSpec.Builder.() -> Unit): Mono<Message> {
    val protected: MessageEditSpec.Builder.() -> Unit = {
        allowedMentionsOrNull(AllowedMentions.suppressAll())
        spec(this)
    }
    return edit(protected.build())
}

fun Message.sendEditEmbed(spec: EmbedCreateSpec.Builder.() -> Unit): Mono<Message> = sendEdit {
    addEmbed(spec.build())
}

fun Message.tryRemoveReaction(emoji: ReactionEmoji, userId: Snowflake) {
    channel.filter { it.type != Channel.Type.DM }.doOnError { }.subscribe { removeReaction(emoji, userId).subscribe() }
}

fun Message.tryRemoveAllReactions(): Mono<Void> {
    return channel.filter { it.type != Channel.Type.DM }.flatMap { removeAllReactions() }.doOnError { }
}

fun InteractionCreateEvent.replyMessage(spec: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit): Mono<Void> {
    val protected: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit = {
        allowedMentions(AllowedMentions.suppressAll())
        spec(this)
    }
    return reply(protected.build())
}

fun InteractionCreateEvent.replyEmbed(spec: EmbedCreateSpec.Builder.() -> Unit): Mono<Void> = replyMessage {
    addEmbed(spec)
}

fun ComponentInteractEvent.sendEdit(spec: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit): Mono<Void> {
    val protected: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit = {
        allowedMentions(AllowedMentions.suppressAll())
        spec(this)
    }
    return edit(protected.build())
}

fun ComponentInteractEvent.sendEditEmbed(spec: EmbedCreateSpec.Builder.() -> Unit): Mono<Void> = sendEdit {
    addEmbed(spec.build())
}

fun InteractionCreateEvent.sendOriginalEdit(spec: ImmutableWebhookMessageEditRequest.Builder.() -> Unit): Mono<MessageData> {
    val protected: ImmutableWebhookMessageEditRequest.Builder.() -> Unit = {
        allowedMentionsOrNull(AllowedMentions.suppressAll().toData())
        spec(this)
    }
    return interactionResponse.editInitialResponse(protected.build())
}

fun InteractionCreateEvent.sendOriginalEditEmbed(spec: EmbedCreateSpec.Builder.() -> Unit): Mono<MessageData> = sendOriginalEdit {
    addEmbed(spec.build().asRequest())
}

val InteractionCreateEvent.user: User
    get() = interaction.user

val User.discriminatedName: String
    get() = "${username}#${discriminator}"

fun Message.addReaction(unicode: String): Mono<Void> =
    addReaction(ReactionEmoji.unicode(unicode))

fun Message.subscribeReaction(unicode: String) {
    addReaction(unicode).subscribe()
}

fun Message.subscribeReactions(vararg unicodes: String) {
    if (unicodes.size <= 1) {
        unicodes.forEach(::subscribeReaction)
    } else {
        val list = unicodes.toMutableList()
        val first = list.first()
        var mono = addReaction(first)
        list.remove(first)
        for (s in list) {
            mono = mono.then(addReaction(s))
        }
        mono.subscribe()
    }
}

val String.discordEmote: ReactionEmoji
    get() = ReactionEmoji.unicode(this)
