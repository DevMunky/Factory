package dev.munky.factory.page

import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.BlockFace
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class MachinePage(playerRef: PlayerRef, interactionType: InteractionType) : InteractiveCustomUIPage<MachinePage.Data>(
    playerRef,
    if (interactionType == InteractionType.Use) CustomPageLifetime.CanDismissOrCloseThroughInteraction
    else CustomPageLifetime.CanDismiss,
    Data.CODEC
) {
    override fun build(
        p0: Ref<EntityStore?>,
        p1: UICommandBuilder,
        p2: UIEventBuilder,
        p3: Store<EntityStore?>
    ) {
        TODO("Not yet implemented")
    }

    class Data {
        var outputSide: BlockFace = BlockFace.Up
            private set

        companion object {
            val CODEC = BuilderCodec.builder(Data::class.java, ::Data)
                .append(
                    KeyedCodec("BlockFace", EnumCodec(BlockFace::class.java)),
                    { data, face -> data.outputSide = face },
                    Data::outputSide
                ).add()
                .build()
        }
    }
}