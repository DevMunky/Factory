package dev.munky.libtech.util

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.codec.builder.BuilderField
import com.hypixel.hytale.codec.schema.config.Schema
import com.hypixel.hytale.codec.schema.metadata.Metadata
import com.hypixel.hytale.codec.validation.Validator
import com.hypixel.hytale.codec.validation.Validators
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

@KotlinFirstApi(reason = "Util", substitute = "Hytale BuilderCodec")
fun <T : Any> codec(clahh: KClass<T>, ctor: () -> T, block: CodecDsl<T>.() -> Unit) = CodecDsl(clahh, ctor).apply(block).build()

@KotlinFirstApi(reason = "Util", substitute = "Hytale BuilderCodec")
class CodecDsl<T : Any>(
    clahh : KClass<T>,
    ctor: () -> T
) {
    private val builder = BuilderCodec.builder(clahh.java, ctor)

    fun build() : BuilderCodec<T> = builder.build()

    fun <F> field(
        prop: KMutableProperty1<T, F>,
        codec: Codec<F>,
        id: String = prop.name.codecCase(),
        block: Field<F>.() -> Unit = {}
    ) {
        Field(
            builder.append(
                KeyedCodec(id, codec),
                prop::set,
                prop::get
            )
        ).apply(block)
    }

    inner class Field<F>(
        private val fieldBuilder: BuilderField.FieldBuilder<T, F, *>
    ) {
        fun deprecated() = validate(Validators.deprecated<F>())

        fun validate(validator: Validator<in F>) {
            fieldBuilder.addValidator(validator)
        }

        fun meta(meta: Metadata) {
            fieldBuilder.metadata(meta)
        }

        fun meta(block: Schema.() -> Unit) {
            fieldBuilder.metadata(block)
        }

        fun build() = fieldBuilder.add()
    }
}

fun String.codecCase(): String {
    var str = ""
    var previous = '\u0000'
    for (c in this) {
        str += when {
            c.isLowerCase() && !previous.isLetter() -> c.uppercase()
            c in arrayOf('-', '.', ' ') -> '_'
            c.isUpperCase() && previous.isLetter() -> " $c"
            else -> c
        }
        previous = c
    }
    return str
}