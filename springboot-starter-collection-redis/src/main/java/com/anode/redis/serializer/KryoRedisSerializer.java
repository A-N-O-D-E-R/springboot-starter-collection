package com.anode.redis.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.xerial.snappy.Snappy;

import java.util.function.Consumer;

/**
 * Kryo-based Redis serializer with optional Snappy compression.
 * Provides efficient binary serialization for Redis values.
 */
public class KryoRedisSerializer<T> implements RedisSerializer<T> {

    private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 8) {
        protected Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryoConfigurer.accept(kryo);
            return kryo;
        }
    };

    private final boolean enableCompression;

    private final Consumer<Kryo> kryoConfigurer;

    public KryoRedisSerializer(boolean enableCompression, Consumer<Kryo> kryoConfigurer) {
        this.enableCompression = enableCompression;
        this.kryoConfigurer = null != kryoConfigurer ? kryoConfigurer : kryo -> {
        };
    }

    @Override
    public byte[] serialize(T t) {
        if (t == null) {
            return new byte[0];
        }
        Kryo kryo = kryoPool.obtain();
        try (Output output = new Output(4096, -1)) {
            kryo.writeClassAndObject(output, t);
            output.flush();
            return enableCompression ? Snappy.compress(output.toBytes()) : output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        } finally {
            kryoPool.free(kryo);
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        Kryo kryo = kryoPool.obtain();
        try (Input input = new Input(enableCompression ? Snappy.uncompress(bytes) : bytes)) {
            return (T) kryo.readClassAndObject(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize object", e);
        } finally {
            kryoPool.free(kryo);
        }
    }
}
