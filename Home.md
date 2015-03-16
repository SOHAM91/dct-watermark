

# Robust Watermarks for color JPEG in Java #

A Java implementation of an algorithm using DCT to embed binary data or text messages into a color pictures. It resist some cropping, scaling or JPEG compression. The implementation also uses Reed-Solomon error correction to extract the exact bits, even in case of watermark destruction.

## Watermarks ##
here is an example watermark:

<a href='https://code.google.com/p/dct-watermark/source/browse/wiki/images/water1.raw'><img src='https://dct-watermark.googlecode.com/svn/wiki/images/water1.png' /> water1.raw</a> Here you see the bits 8x8 pixel wide that form a 128x128 pixel watermark that **will be embedded** into an image.

<a href='https://code.google.com/p/dct-watermark/source/browse/wiki/images/water2.raw'><img src='https://dct-watermark.googlecode.com/svn/wiki/images/water2.png' /> water2.raw</a> This is a watermark that **was extracted from an image**. It's not that crisp as before, and that is why you can't store much information in it.

<a href='https://code.google.com/p/dct-watermark/source/browse/wiki/images/water3.raw'><img src='https://dct-watermark.googlecode.com/svn/wiki/images/water3.png' /> water3.raw</a> **Resurrected watermark** after some statistical sharpening. Now we can read the bits again. As you can see **there are errors** compared to water1.raw. Some bits are faulty. If not too many, it can be fixed via [error correction](http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction).

## Example Usage ##
Download the [dct-watermark.jar](https://code.google.com/p/dct-watermark/source/browse/trunk/dct-watermark.jar), and start it via Java from the command line. To **embed** a watermark _Hello World_ in a picture called _[lena.jpg](http://en.wikipedia.org/wiki/Lenna)_ run the following (result will be in _[lena2.jpg](https://code.google.com/p/dct-watermark/source/browse/trunk/lena2.jpg)_)):
```
java -jar dct-watermark.jar e -d lena.jpg lena2.jpg "Hello World"
```

To **extract** a watermark from a picture call this:
```
java -jar dct-watermark.jar x -d lena2.jpg
```

You can also give a seed to protect the data:
```
java -jar dct-watermark.jar e -d -s1 44 -s2 12 lena.jpg lena2.jpg "Hello World"
java -jar dct-watermark.jar x -d -s1 44 -s2 12 lena2.jpg
```
For more options (e.g. error correction, bit-size, opacity/visibility, quality) type:
```
java -jar dct-watermark.jar -h
```

**Note:** If you wanna upload to someplace that compresses the photo again/itself (like [facebook albums](http://www.facebook.com/photo.php?fbid=10150503288762499&set=a.358839827498.152299.522392498&type=1)), _always_ set the JPEG quality to 100% via switch <tt>-q 1.0</tt>

## Online Demos ##
currently there is no online demo, hope I can upload a webapp soon.