package com.isuwang.dapeng.common;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class DumpUtil {

    private static void dump(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int availabe = buffer.readableBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  ASCII....
        System.out.println("=======[");
        int i = 0;
        for (; i < availabe; i++) {
            byte b = buffer.readByte();
            baos.write(b);

            String it = String.format("%02x ", b & 0xFF);
            System.out.print(it);

            if (i % 16 == 15) {
                byte[] array = baos.toByteArray();
                System.out.print(' ');
                for (int j = 0; j < array.length; j++) {
                    char ch = (char) array[j];
                    if (ch >= 0x20 && ch < 0x7F) System.out.print(ch);
                    else System.out.print('.');
                }
                baos = new ByteArrayOutputStream();
                System.out.println();
            }
        }
//        if(baos.size() > 0) {
//
//        }
        System.out.println("]======");

        buffer.readerIndex(readerIndex);
    }
}
