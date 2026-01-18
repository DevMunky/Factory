package dev.munky.libtech.util

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.ExtraInfo
import com.hypixel.hytale.codec.WrappedCodec
import com.hypixel.hytale.codec.codecs.EnumCodec
import com.hypixel.hytale.codec.exception.CodecException
import com.hypixel.hytale.codec.schema.SchemaContext
import com.hypixel.hytale.codec.schema.config.ArraySchema
import com.hypixel.hytale.codec.schema.config.Schema
import org.bson.BsonArray
import org.bson.BsonValue
import java.util.*

data class EnumSetCodec<E : Enum<E>>(private val enumClass: Class<E>) : Codec<EnumSet<E>>, WrappedCodec<E> {
    private val enumCodec = EnumCodec(enumClass)
    override fun getChildCodec(): Codec<E> = enumCodec

    override fun decode(bsonValue: BsonValue, extra: ExtraInfo): EnumSet<E> {
        val list = bsonValue.asArray()
        val out = EnumSet.noneOf(enumClass)
        if (list.isEmpty()) return out
        for (i in list.indices) {
            val value = list[i]
            extra.pushIntKey(i)

            try {
                val decoded = enumCodec.decode(value, extra)
                if (!out.add(decoded)) {
                    throw CodecException("The value is already in the set:$decoded")
                }
            } catch (e: Exception) {
                throw CodecException("Failed to decode", value, extra, e)
            } finally {
                extra.popKey()
            }
        }
        return out
    }

    override fun encode(set: EnumSet<E>, extra: ExtraInfo): BsonValue {
        val out = BsonArray()
        var key = 0

        for (v in set) {
            extra.pushIntKey(key++)

            try {
                out.add(enumCodec.encode(v, extra))
            } finally {
                extra.popKey()
            }
        }

        return out
    }

    override fun toSchema(context: SchemaContext): Schema = ArraySchema().apply {
        title = "EnumSet"
        setItem(context.refDefinition(enumCodec))
        uniqueItems = true
    }
}