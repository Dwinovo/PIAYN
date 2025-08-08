package com.dwinovo.piayn.schem.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.nbt.ByteArrayTag;

/**
 * VarInt 编解码工具类
 * 处理 Sponge Schematic 格式中的 varint 数组编解码
 */
public class VarIntUtil {
    
    /**
     * 将整数列表编码为 varint 字节数组
     */
    @Nonnull
    public static ByteArrayTag encodeVarintArray(@Nonnull List<Integer> data) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            
            for (Integer value : data) {
                writeVarint(dataOutputStream, value);
            }
            
            return new ByteArrayTag(byteArrayOutputStream.toByteArray());
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode varint array", e);
        }
    }
    
    /**
     * 写入单个 varint 值
     */
    public static void writeVarint(@Nonnull DataOutputStream outputStream, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            outputStream.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        outputStream.writeByte(value & 0x7F);
    }
    
    /**
     * 解码 varint 字节数组为整数列表
     */
    @Nonnull
    public static List<Integer> decodeVarintArray(@Nonnull byte[] data) {
        List<Integer> result = new ArrayList<>();
        
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            
            while (dataInputStream.available() > 0) {
                result.add(readVarint(dataInputStream));
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode varint array", e);
        }
        
        return result;
    }
    
    /**
     * 读取单个 varint 值
     */
    public static int readVarint(@Nonnull DataInputStream inputStream) throws IOException {
        int result = 0;
        int shift = 0;
        byte currentByte;
        
        do {
            currentByte = inputStream.readByte();
            result |= (currentByte & 0x7F) << shift;
            shift += 7;
        } while ((currentByte & 0x80) != 0);
        
        return result;
    }
    
    private VarIntUtil() {
        // 工具类，禁止实例化
    }
}
