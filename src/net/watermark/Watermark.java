/*
 * Copyright 2012 by Christoph Gaffga licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package net.watermark;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import net.util.Bits;

/**
 * Implementation of a watermarking also. See https://code.google.com/p/dct-watermark/
 * 
 * @author Christoph Gaffga
 */
public class Watermark {

    /** Valid characters and their order in our 6-bit charset. */
    final static String validChars = " abcdefghijklmnopqrstuvwxyz0123456789.-,:/()?!\"'#*+_%$&=<>[];@§\n";

    /** The width and height of our quantization box in pixels (n-times n pixel per bit). */
    final static int bitBoxSize = 8;

    /** Number of bytes used for Reed-Solomon error correction. */
    static final int nRS = 8;

    /** Maximal length in of characters for text messages. */
    final static int maxLen = (128 / bitBoxSize * (128 / bitBoxSize) - nRS * 8) / 6;

    /** Kind of the opacity of the marks when added to the image. */
    static final double robustness = 1.0; /* 1.0 does nothing, default */

    /** Seed for randomization of the watermark. */
    private long randomizeWatermarkSeed = 19;

    /** Seed for randomization of the embedding. */
    private long randomizeEmbeddingSeed = 24;

    /**
     * @param args
     */
    public static void main(final String[] args) {

        final Watermark watermark = new Watermark();

        try {

            final String message = "¡This is a TEST!";
            Bits bits = new Bits();
            if (true) {

                // read source image...
                final BufferedImage image = ImageIO.read(new File("lena.jpg"));
                System.out.println("w=" + image.getWidth());
                System.out.println("h=" + image.getHeight());

                System.out.println("Message: " + message);

                bits = watermark.string2Bits(message);

                System.out.println("MaxLen: " + maxLen);
                System.out.println("BitLen: " + message.length() * 6);

                bits = Bits.bitsReedSolomonEncode(bits, nRS);

                // create watermark image...
                final int[][] watermarkBitmap = new int[128][128];
                for (int y = 0; y < 128 / bitBoxSize * bitBoxSize; y++) {
                    for (int x = 0; x < 128 / bitBoxSize * bitBoxSize; x++) {
                        if (bits.size() > x / bitBoxSize + y / bitBoxSize * (128 / bitBoxSize)) {
                            watermarkBitmap[y][x] = bits.getBit(x / bitBoxSize + y / bitBoxSize * (128 / bitBoxSize)) ? 255
                                    : 0;
                        }
                    }
                }

                // 寫入檔案 watermark
                writeRaw("water1.raw", watermarkBitmap);

                // embedding...
                final int[][] grey = watermark.embed(image, watermarkBitmap);
                // write JPEG...
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        final Color color = new Color(image.getRGB(x, y));
                        final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                        // adjust brightness of the pixel...
                        hsb[2] = (float) (grey[y][x] / 255.0);
                        final Color colorNew = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
                        image.setRGB(x, y, colorNew.getRGB());

                    }
                }
                ImageIO.write(image, "jpeg", new File("lena2.jpg"));

            } // END embed

            // extraction...
            final BufferedImage image = ImageIO.read(new File("lena2.jpg"));
            final int[][] extracted = watermark.extract(image);

            // 寫入檔案 Watermark
            writeRaw("water2.raw", extracted);

            // black/white the extracted result...
            for (int y = 0; y < 128 / bitBoxSize * bitBoxSize; y += bitBoxSize) {
                for (int x = 0; x < 128 / bitBoxSize * bitBoxSize; x += bitBoxSize) {
                    int sum = 0;
                    for (int y2 = y; y2 < y + bitBoxSize; y2++) {
                        for (int x2 = x; x2 < x + bitBoxSize; x2++) {
                            sum += extracted[y2][x2];
                        }
                    }
                    sum = sum / (bitBoxSize * bitBoxSize);
                    for (int y2 = y; y2 < y + bitBoxSize; y2++) {
                        for (int x2 = x; x2 < x + bitBoxSize; x2++) {
                            extracted[y2][x2] = sum > 127 ? 255 : 0;
                        }
                    }
                }
            }

            // write B/W result...
            writeRaw("water3.raw", extracted);

            // reconstruct bits...
            Bits bits2 = new Bits();
            for (int y = 0; y < 128 / bitBoxSize * bitBoxSize; y += bitBoxSize) {
                for (int x = 0; x < 128 / bitBoxSize * bitBoxSize; x += bitBoxSize) {
                    bits2.addBit(extracted[y][x] > 127);
                }
            }
            bits2 = new Bits(bits2.getBits(0, maxLen * 6 + 8 * nRS));

            // count errors...
            int errors = 0;
            for (int i = 0; i < bits.size(); i++) {
                if (bits.getBit(i) != bits2.getBit(i)) {
                    errors++;
                }
            }
            System.out.println("Error Bits (of " + bits.size() + "): " + errors);

            bits2 = Bits.bitsReedSolomonDecode(bits2, nRS);

            // reconstruct message...
            final String message2 = watermark.bits2String(bits2).trim();
            System.out.println("Reconstructed Message: " + message2);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static final void writeRaw(final String filename, final int[][] data) throws IOException {
        final FileOutputStream fos = new FileOutputStream(filename);
        final OutputStream os = new BufferedOutputStream(fos, 1024);
        final DataOutputStream dos = new DataOutputStream(os);
        for (final int[] element : data) {
            for (int j = 0; j < element.length; j++) {
                dos.writeByte(element[j]);
            }
        }
        dos.close();
    }

    private int[][] embed(final BufferedImage src, final int[][] water1) throws IOException {
        final int width = (src.getWidth() + 7) / 8 * 8;
        final int height = (src.getHeight() + 7) / 8 * 8;

        // original image process
        final int N = 8;
        final int buff1[][] = new int[height][width]; // Original image
        final int buff2[][] = new int[height][width]; // DCT Original image coefficients
        final int buff3[][] = new int[height][width]; // IDCT Original image coefficients

        final int b1[][] = new int[N][N]; // DCT input
        final int b2[][] = new int[N][N]; // DCT output

        final int b3[][] = new int[N][N]; // IDCT input
        final int b4[][] = new int[N][N]; // IDCT output

        // watermark image process
        final int W = 4;
        final int water2[][] = new int[128][128]; // random watermark image
        final int water3[][] = new int[128][128]; // DCT watermark image coefficients

        final int w1[][] = new int[W][W]; // DCT input
        final int w2[][] = new int[W][W]; // DCT output
        final int w3[][] = new int[W][W]; // quantization output
        final int mfbuff1[][] = new int[128][128]; // embed coefficients
        final int mfbuff2[] = new int[width * height]; // 2 to 1

        // random process...
        int a, b, c;
        final int tmp[] = new int[128 * 128];

        // random embed...
        int c1;
        int cc = 0;
        final int tmp1[] = new int[128 * 128];

        // divide 8x8 block...
        int k = 0, l = 0;

        // init buf1 from src image...
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                final Color color = new Color(src.getRGB(x, y));
                final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                // use brightness of the pixel...
                buff1[y][x] = (int) (hsb[2] * 255.0);
            }
        }

        // 將 Original image 切成 8*8 的 block 作 DCT 轉換
        // System.out.println("Original image         ---> FDCT");

        for (int y = 0; y < height; y += N) {
            for (int x = 0; x < width; x += N) {
                for (int i = y; i < y + N; i++) {
                    for (int j = x; j < x + N; j++) {
                        b1[k][l] = buff1[i][j];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;
                final DCT o1 = new DCT(); // 宣告 DCT 物件
                o1.ForwardDCT(b1, b2); // 引用 DCT class 中,ForwardDCT的方法

                for (int p = y; p < y + N; p++) {
                    for (int q = x; q < x + N; q++) {
                        buff2[p][q] = b2[k][l];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;

            }
        }
        // System.out.println("                       OK!      ");

        // watermark image 作 random 處理
        // System.out.println("Watermark image        ---> Random");
        final Random r = new Random(randomizeWatermarkSeed); // 設定亂數產生器的seed
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                while (true) {
                    c = r.nextInt(128 * 128);
                    if (tmp[c] == 0) {
                        break;
                    }
                }
                a = c / 128;
                b = c % 128;
                water2[i][j] = water1[a][b];
                tmp[c] = 1;
            }
        }
        // System.out.println("                       OK!      ");

        // 將 watermark image 切成 4x4 的 block 作 DCT 轉換與quantization
        k = 0;
        l = 0;
        // System.out.println("Watermark image        ---> FDCT & Quantization");
        for (int y = 0; y < 128; y += W) {
            for (int x = 0; x < 128; x += W) {
                for (int i = y; i < y + W; i++) {
                    for (int j = x; j < x + W; j++) {
                        w1[k][l] = water2[i][j];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;

                // 宣告 DCT2 物件
                final DCT2 wm1 = new DCT2();

                // 引用DCT2 class 中,ForwardDCT的方法
                wm1.ForwardDCT(w1, w2);

                final Qt qw1 = new Qt(); // 宣告 Qt 物件
                qw1.WaterQt(w2, w3, robustness); // 引用Qt class 中,WaterQt的方法

                for (int p = y; p < y + W; p++) {
                    for (int q = x; q < x + W; q++) {
                        water3[p][q] = w3[k][l];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;
            }
        }
        // System.out.println("                       OK!      ");

        // Embedding Watermark water3[128][128] -->buff2[512][512]
        // System.out.println("Watermarked image      ---> Embedding");

        // Random Embedding
        final Random r1 = new Random(randomizeEmbeddingSeed); // 設定亂數產生器的seed
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                while (true) {
                    c1 = r1.nextInt(128 * 128);
                    if (tmp1[c1] == 0) {
                        break;
                    }
                }
                a = c1 / 128;
                b = c1 % 128;
                mfbuff1[i][j] = water3[a][b];
                tmp1[c1] = 1;
            }
        }

        // 二維 轉 一維
        final zigZag scan = new zigZag();
        scan.two2one(mfbuff1, mfbuff2);

        // WriteBack coefficients
        for (int i = 0; i < height; i += N) {
            for (int j = 0; j < width; j += N) {
                buff2[i + 1][j + 4] = mfbuff2[cc];
                cc++;
                buff2[i + 2][j + 3] = mfbuff2[cc];
                cc++;
                buff2[i + 3][j + 2] = mfbuff2[cc];
                cc++;
                buff2[i + 4][j + 1] = mfbuff2[cc];
                cc++;
            }
        }
        cc = 0;
        // System.out.println("                       OK!      ");

        // 將 Watermarked image 切成 8*8 的 block 作 IDCT 轉換
        // System.out.println("Watermarked image      ---> IDCT");
        k = 0;
        l = 0;
        for (int y = 0; y < height; y += N) {
            for (int x = 0; x < width; x += N) {
                for (int i = y; i < y + N; i++) {
                    for (int j = x; j < x + N; j++) {
                        b3[k][l] = buff2[i][j];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;

                final DCT o2 = new DCT(); // 宣告 DCT 物件
                o2.InverseDCT(b3, b4); // 引用DCT class 中,InverseDCT的方法

                for (int p = y; p < y + N; p++) {
                    for (int q = x; q < x + N; q++) {
                        buff3[p][q] = b4[k][l];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;
            }
        }
        // System.out.println("                       OK!      ");

        return buff3;
    }

    private int[][] extract(final BufferedImage src) throws IOException {
        final int width = (src.getWidth() + 7) / 8 * 8;
        final int height = (src.getHeight() + 7) / 8 * 8;

        // original image
        final int N = 8;
        final int buff1[][] = new int[height][width]; // watermarked image
        final int buff2[][] = new int[height][width]; // DCT watermarked image coefficients
        final int b1[][] = new int[N][N]; // DCT input
        final int b2[][] = new int[N][N]; // DCT output

        // watermark
        final int W = 4;
        final int water1[][] = new int[128][128]; // extract watermark image
        final int water2[][] = new int[128][128]; // DCT watermark image coefficients
        final int water3[][] = new int[128][128]; // random watermark image

        final int w1[][] = new int[W][W]; // dequantization output
        final int w2[][] = new int[W][W]; // DCT input
        final int w3[][] = new int[W][W]; // DCT output

        // random process
        int a, b, c, c1;
        final int tmp[] = new int[128 * 128];
        final int tmp1[] = new int[128 * 128];
        int cc = 0;

        // middle frequency coefficients
        // final int mfbuff1[] = new int[128 * 128];
        final int mfbuff1[] = new int[width * height];
        final int mfbuff2[][] = new int[128][128]; // 1 to 2

        // divide 8x8 block
        int k = 0, l = 0;

        // init buf1 from watermarked image src...
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                final Color color = new Color(src.getRGB(x, y));
                final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                // use brightness of the pixel...
                buff1[y][x] = (int) (hsb[2] * 255.0);
            }
        }

        // 將 watermark image 切成 8x8 的 block 作 DCT 轉換
        // System.out.println("Watermarked image         ---> FDCT");
        for (int y = 0; y < height; y += N) {
            for (int x = 0; x < width; x += N) {
                for (int i = y; i < y + N; i++) {
                    for (int j = x; j < x + N; j++) {
                        b1[k][l] = buff1[i][j];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;

                final DCT o1 = new DCT(); // 宣告 DCT 物件
                o1.ForwardDCT(b1, b2); // 引用DCT class 中,ForwardDCT的方法

                for (int p = y; p < y + N; p++) {
                    for (int q = x; q < x + N; q++) {
                        buff2[p][q] = b2[k][l];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;
            }
        }
        // System.out.println("                       OK!      ");

        // extract middle frequency coefficients...
        // System.out.println("watermark image       ---> Extracting");
        for (int i = 0; i < height; i += N) {
            for (int j = 0; j < width; j += N) {
                mfbuff1[cc] = buff2[i + 1][j + 4];
                cc++;
                mfbuff1[cc] = buff2[i + 2][j + 3];
                cc++;
                mfbuff1[cc] = buff2[i + 3][j + 2];
                cc++;
                mfbuff1[cc] = buff2[i + 4][j + 1];
                cc++;
            }
        }
        cc = 0;

        // 一維 轉 二維
        final zigZag scan = new zigZag(); // 宣告 zigZag 物件
        scan.one2two(mfbuff1, mfbuff2); // 引用zigZag class 中,one2two的方法

        // random extracting
        final Random r1 = new Random(randomizeEmbeddingSeed);
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                while (true) {
                    c1 = r1.nextInt(128 * 128);
                    if (tmp1[c1] == 0) {
                        break;
                    }
                }
                a = c1 / 128;
                b = c1 % 128;
                water1[a][b] = mfbuff2[i][j];
                tmp1[c1] = 1;
            }
        }
        // System.out.println("                       OK!      ");

        k = 0;
        l = 0;
        // System.out.println("Watermark image       ---> Dequantization & IDCT");
        for (int y = 0; y < 128; y += W) {
            for (int x = 0; x < 128; x += W) {

                for (int i = y; i < y + W; i++) {
                    for (int j = x; j < x + W; j++) {
                        w1[k][l] = water1[i][j];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;

                final Qt qw2 = new Qt(); // 宣告 Qt 物件
                qw2.WaterDeQt(w1, w2, robustness); // 引用Qt class 中,WaterDeQt的方法

                final DCT2 wm2 = new DCT2(); // 宣告 DCT2 物件
                wm2.InverseDCT(w2, w3); // 引用DCT2 class 中,InverseDCT的方法
                for (int p = y; p < y + W; p++) {
                    for (int q = x; q < x + W; q++) {
                        water2[p][q] = w3[k][l];
                        l++;
                    }
                    l = 0;
                    k++;
                }
                k = 0;
            }
        }
        // System.out.println("                       OK!      ");

        // System.out.println("Watermark image       ---> Re Random");
        final Random r = new Random(randomizeWatermarkSeed); // 設定亂數產生器的seed
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) {
                while (true) {
                    c = r.nextInt(128 * 128);
                    if (tmp[c] == 0) {
                        break;
                    }
                }
                a = c / 128;
                b = c % 128;
                water3[a][b] = water2[i][j];
                tmp[c] = 1;
            }
        }
        // System.out.println("                       OK!      ");

        return water3;
    }

    private Bits string2Bits(String s) {
        final Bits bits = new Bits();

        // remove invalid characters...
        s = s.toLowerCase();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (validChars.indexOf(c) < 0) {
                s = s.substring(0, i) + s.substring(i + 1);
                i--;
            }
        }

        // shorten if needed...
        if (s.length() > maxLen) {
            s = s.substring(0, maxLen);
        }
        // padding if needed...
        while (s.length() < maxLen) {
            s += " ";
        }

        // create watermark bits...
        System.out.println("Max bits: " + 128 / bitBoxSize * (128 / bitBoxSize));
        for (int j = 0; j < s.length(); j++) {
            bits.addValue(validChars.indexOf(s.charAt(j)), 6);
        }

        return bits;
    }

    private String bits2String(Bits bits) {
        final StringBuilder buf = new StringBuilder();
        {
            for (int i = 0; i < maxLen; i++) {
                final int c = (int) bits.getValue(i * 6, 6);
                buf.append(validChars.charAt(c));
            }
        }
        return buf.toString();
    }

}
