package com.dslplatform.json.runtime;

import com.dslplatform.json.*;

public final class ArrayEncoder<T> implements JsonWriter.WriteObject<T[]> {

	private final DslJson json;
	private final JsonWriter.WriteObject<T> encoder;
	private Class<?> lastCachedClass;
	private JsonWriter.WriteObject lastCachedWriter;

	public ArrayEncoder(
			final DslJson json,
			@Nullable final JsonWriter.WriteObject<T> encoder) {
		if (json == null) throw new IllegalArgumentException("json can't be null");
		this.json = json;
		this.encoder = encoder;
	}

	private static final byte[] EMPTY = {'[', ']'};

	@Override
	public void write(final JsonWriter writer, @Nullable final T[] value) {
		if (value == null) writer.writeNull();
		else if (value.length == 0) writer.writeAscii(EMPTY);
		else if (encoder != null) {
			writer.writeByte(JsonWriter.ARRAY_START);
			encoder.write(writer, value[0]);
			for (int i = 1; i < value.length; i++) {
				writer.writeByte(JsonWriter.COMMA);
				encoder.write(writer, value[i]);
			}
			writer.writeByte(JsonWriter.ARRAY_END);
		} else {
			boolean pastFirst = false;
			writer.writeByte(JsonWriter.ARRAY_START);
			Class<?> lastClass = null;
			JsonWriter.WriteObject lastEncoder = null;
			for (final T e : value) {
				if (pastFirst) {
					writer.writeByte(JsonWriter.COMMA);
				} else {
					pastFirst = true;
				}
				if (e == null) writer.writeNull();
				else {
					final Class<?> currentClass = e.getClass();
					if (currentClass == lastCachedClass) {
						lastEncoder = lastCachedWriter;
						lastClass = lastCachedClass;
					} else if (currentClass != lastClass) {
						lastClass = currentClass;
						lastEncoder = json.tryFindWriter(lastClass);
						if (lastEncoder == null) {
							throw new ConfigurationException("Unable to find writer for " + lastClass);
						}
						if (lastCachedClass == null) {
							synchronized (this) {
								if (lastCachedClass == null) {
									lastCachedWriter = lastEncoder;
									lastCachedClass = lastClass;
								}
							}
						}
					}
					lastEncoder.write(writer, e);
				}
			}
			writer.writeByte(JsonWriter.ARRAY_END);
		}
	}
}
