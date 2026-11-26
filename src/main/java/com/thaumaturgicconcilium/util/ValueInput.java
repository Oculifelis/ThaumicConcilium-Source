package com.thaumaturgicconcilium.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** Read side of a saved CompoundTag, with the defaulting getters the rest of the mod uses. */
public record ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
    public int getIntOr(String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public byte getByteOr(String key, byte fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getByte(key) : fallback;
    }

    public float getFloatOr(String key, float fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
    }

    public boolean getBooleanOr(String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }

    public String getStringOr(String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public Optional<String> getString(String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag t = tag.get(key);
        if (t == null) {
            return Optional.empty();
        }
        return codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), t).result();
    }

    public <T> Optional<T> read(MapCodec<T> codec) {
        return codec.codec().parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result();
    }
}
