/*
 * Copyright 2012 by Christoph Gaffga licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import net.watermark.Watermark;

/**
 * For starting from command line.
 * 
 * @author cgaffga
 */
public class Main {

    /**
     * @param args
     */
    public static void main(final String[] args) {
        int boxSize = 10;
        int errorCorrection = 6;
        double opacity = 1.0;
        long seed1 = 19;
        long seed2 = 24;
        Float quality = null;

        if (args.length < 2 || args[0].equals("-h") || args[0].equals("--help")) {
            System.out.println("Watermark Usage:  dct-watermark [e|x|-h] [-options] src.jpg [dst.jpg text] \n");
            System.out.println("  Commands:");
            System.out.println("  e    embedding mode");
            System.out.println("  x    extraction mode");
            System.out.println("  Options:");
            System.out.println("  -b   box size in pixel, the size of pixels (x*x) for each bit (int)");
            System.out.println("  -e   number of bytes for Reed-Solomon error correction (int)");
            System.out.println("  -o   opacity of the watermark (float, 0..1, 1.0=solid)");
            System.out.println("  -s1  first seed for random number initialization (long)");
            System.out.println("  -s2  second seed for random number initialization (long)");
            System.out.println("  -q   JPEG quality (float, 0..1)");
            System.out.println("  -d   turn debugging mode on");
            System.out.println("The bigger the box, the higher the error correction, the more solid the watermark - ");
            System.out
                    .println("the bigger the robustness of the watermark, but you can store less or it's less invisible.");
            System.out.println("Random seeds for embedding and extraction must be equal.");
            System.out
                    .println("If the watermark is totally broken, you get a ReedSolomonException, as it can't be fixed");
            System.out.println("Capacity: (128 / b)^2 - e * 8 bits, 6 bits per character");
            System.out.println("by the error correction also.");
            System.out.println("\nExamples:");
            System.out.println("dct-watermark e -d lena.jpg lena2.jpg \"Hello World\"");
            System.out.println("dct-watermark x -d lena2.jpg");
            System.out.println("dct-watermark e -d lena.jpg lena2.jpg \"Hello World\"");
            System.out.println("dct-watermark x -d lena2.jpg");
            System.out.println("dct-watermark e -d -b 8 -e 20 -o 0.7 lena.jpg lena2.jpg \"Hello World\"");
            System.out.println("dct-watermark x -d -b 8 -e 20 lena2.jpg");
            System.out.println();
        } else {
            try {
                final String command = args[0];
                int i;
                for (i = 1; i < args.length - 1; i++) {
                    if (args[i].equals("-b")) {
                        boxSize = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("-e")) {
                        errorCorrection = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("-o")) {
                        opacity = Double.parseDouble(args[++i]);
                    } else if (args[i].equals("-s1")) {
                        seed1 = Long.parseLong(args[++i]);
                    } else if (args[i].equals("-s2")) {
                        seed2 = Long.parseLong(args[++i]);
                    } else if (args[i].equals("-q")) {
                        quality = Float.parseFloat(args[++i]);
                    } else if (args[i].equals("-d")) {
                        Watermark.debug = true;
                    } else {
                        break;
                    }
                }
                final String file = args[i++];

                final BufferedImage image = ImageIO.read(new File(file));
                final Watermark water = new Watermark(boxSize, errorCorrection, opacity, seed1, seed2);

                if (command.equals("e")) { // embedding...
                    final String file2 = args[i++];
                    String message = "";
                    for (int j = i; j < args.length; j++) {
                        if (j > i) {
                            message += " ";
                        }
                        message += args[j];
                    }

                    if (Watermark.debug) {
                        System.out.println("Source Image:");
                        System.out.println("Image:            " + file);
                        System.out.println("Image width:      " + image.getWidth());
                        System.out.println("Image height:     " + image.getHeight());
                        System.out.println("Output JPEG qual.:" + quality);
                        System.out.println("Watermark:");
                        System.out.println("Text:             " + message);
                        System.out.println("Text length:      " + message.length());
                        System.out.println("Watermark properties:");
                        System.out.println("Bit box size:     " + water.getBitBoxSize());
                        System.out.println("Error cor. bytes: " + water.getByteLenErrorCorrection());
                        System.out.println("Opacity:          " + water.getOpacity());
                        System.out.println("Seed 1 (embed):   " + water.getRandomizeEmbeddingSeed());
                        System.out.println("Seed 2 (wmark):   " + water.getRandomizeWatermarkSeed());
                        System.out.println("Watermark Capacity:");
                        System.out.println("Max bits total:   " + water.getMaxBitsTotal());
                        System.out.println("Max bits message: " + water.getMaxBitsData());
                        System.out.println("Max text length:  " + water.getMaxTextLen());
                    }

                    water.embed(image, message);

                    // write back to JPEG...
                    final Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
                    final ImageWriter writer = iter.next();
                    final ImageWriteParam iwp = writer.getDefaultWriteParam();
                    if (quality != null) {
                        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        iwp.setCompressionQuality(quality);
                    }
                    final FileImageOutputStream output = new FileImageOutputStream(new File(file2));
                    writer.setOutput(output);
                    final IIOImage imageIO = new IIOImage(image, null, null);
                    writer.write(null, imageIO, iwp);
                    writer.dispose();
                } else if (command.equals("x")) { // extraction...

                    if (Watermark.debug) {
                        System.out.println("Source Image:");
                        System.out.println("Image:            " + file);
                        System.out.println("Image width:      " + image.getWidth());
                        System.out.println("Image height:     " + image.getHeight());
                        System.out.println("Watermark properties:");
                        System.out.println("Bit box size:     " + water.getBitBoxSize());
                        System.out.println("Error cor. bytes: " + water.getByteLenErrorCorrection());
                        System.out.println("Seed 1 (embed):   " + water.getRandomizeEmbeddingSeed());
                        System.out.println("Seed 2 (wmark):   " + water.getRandomizeWatermarkSeed());
                        System.out.println("Watermark Capacity:");
                        System.out.println("Max bits total:   " + water.getMaxBitsTotal());
                        System.out.println("Max bits message: " + water.getMaxBitsData());
                        System.out.println("Max text length:  " + water.getMaxTextLen());
                        System.out.println();
                    }

                    final String message = water.extractText(image);
                    System.out.println(message);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

}
