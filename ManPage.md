# manpage for dct-watermark.sh.

# dct-watermark man page #

show this help via:
<pre>
./dct-watermark.sh -h<br>
java -jar dct-watermark.jar -h<br>
</pre>

this is just a copy of the [help screen](https://code.google.com/p/dct-watermark/source/browse/trunk/src/Main.java#31) shown with the -h option:
<pre>
Watermark Usage:  dct-watermark [e|x|-h] [-options] src.jpg [dst.jpg text]<br>
<br>
Commands:<br>
e    embedding mode<br>
x    extraction mode<br>
Options:<br>
-b   box size in pixel, the size of pixels (x*x) for each bit (int)<br>
-e   number of bytes for Reed-Solomon error correction (int)<br>
-o   opacity of the watermark (float, 0..1, 1.0=solid)<br>
-s1  first seed for random number initialization (long)<br>
-s2  second seed for random number initialization (long)<br>
-q   JPEG quality (float, 0..1)<br>
-d   turn debugging mode on<br>
The bigger the box, the higher the error correction, the more solid the watermark -<br>
the bigger the robustness of the watermark, but you can store less or it's less invisible.<br>
Random seeds for embedding and extraction must be equal.<br>
If the watermark is totally broken, you get a ReedSolomonException, as it can't be fixed<br>
Capacity: (128 / b)^2 - e * 8 bits, 6 bits per character<br>
by the error correction also.<br>
<br>
Examples:<br>
dct-watermark e -d lena.jpg lena2.jpg "Hello World"<br>
dct-watermark x -d lena2.jpg<br>
dct-watermark e -d lena.jpg lena2.jpg "Hello World"<br>
dct-watermark x -d lena2.jpg<br>
dct-watermark e -d -b 8 -e 20 -o 0.7 lena.jpg lena2.jpg "Hello World"<br>
dct-watermark x -d -b 8 -e 20 lena2.jpg<br>
</pre>