package com.thaumaturgicconcilium.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/** Write side of a saved CompoundTag. */
public record ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    public void putByte(String key, byte value) {
        tag.putByte(key, value);
    }

    public void putFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    public void putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    public void putString(String key, String value) {
        tag.putString(key, value);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).result().ifPresent(t -> tag.put(key, t));
    }

    public <T> void storeNullable(String key, Codec<T> codec, T value) {
        if (value != null) {
            store(key, codec, value);
        }
    }

    public <T> void store(MapCodec<T> codec, T value) {
        codec.codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value).result().ifPresent(t -> {
            if (t instanceof CompoundTag c) {
                tag.merge(c);
            }
        });
    }
}
