package com.marvin.nettyadvanced.protocol;

import com.google.gson.Gson;
import com.marvin.message.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-23 03:40
 **/
public interface Serializer {
    <T> T deserialize(Class<T> clazz, byte[] bytes);

    <T> byte[] serialize(T object);

    enum Algorithm implements Serializer{
        JAVA {
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                    return (T) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException("反序列化失败",e);
                }
            }

            @Override
            public <T> byte[] serialize(T object) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(object);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("序列化失败",e);
                }
            }
        },
        JSON{
            @Override
            public <T> T deserialize(Class<T> clazz, byte[] bytes) {
                System.out.println(456456);
                String json = new String(bytes,StandardCharsets.UTF_8);
                return new Gson().fromJson(json,clazz);
            }

            @Override
            public <T> byte[] serialize(T object) {
                System.out.println(123123);
                String json = new Gson().toJson(object);
                return json.getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
