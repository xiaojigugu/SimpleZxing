package com.google.zxing;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;

/**
 * description :
 *
 * @author Junt
 * @date :2019/6/4 12:42
 */
public class ZxingUtils {
    /**
     * encode a new QR Code
     *
     * @param w width of bitmap
     * @param h height of bitmap
     * @param str content of QR code
     * @return bitmap contains a QR code
     */
    public static Bitmap qrCodeWriter(int w, int h, String str) {
        Bitmap bitmap = null;
        try {
            if (str == null || "".equals(str) || str.length() < 1) {
                return bitmap;
            }
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(str, BarcodeFormat.QR_CODE, w, h, hints);
            int[] pixels = new int[w * h];
            //下面这里按照二维码的算法，逐个生成二维码的图片，
            //两个for循环是图片横列扫描的结果
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * w + x] = 0xff000000;
                    } else {
                        pixels[y * w + x] = 0xffffffff;
                    }
                }
            }
            //生成二维码图片的格式，使用ARGB_8888
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        } catch (
                WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * Read content from the given image
     *
     * @param drawable  img
     */
    public static String qrCodeReaderFromImg(Drawable drawable) {
        String result = "";
        try {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
            Binarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
            QRCodeReader reader = new QRCodeReader();
            Result parserResult = reader.decode(binaryBitmap);
            result = parserResult.getText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
