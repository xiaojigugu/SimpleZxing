package com.google.zxing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
     * @param w   width of bitmap
     * @param h   height of bitmap
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
     * @param size      二维码尺寸
     * @param logoScale 中间logo的尺寸
     * @param text      文字信息
     * @param mBitmap   Logo
     * @return 生成的二维码
     */
    public static Bitmap qrCodeWriterWithLogo(int size, int logoScale, String text, Bitmap mBitmap) {
        try {
            int logoSize = size / logoScale;
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            /*
             * 设置容错级别，默认为ErrorCorrectionLevel.L
             * 因为中间加入logo所以建议你把容错级别调至H,否则可能会出现识别不了
             */
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            //设置空白边距的宽度
            hints.put(EncodeHintType.MARGIN, 1); //default is 4
            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(text,
                    BarcodeFormat.QR_CODE, size, size, hints);

            int width = bitMatrix.getWidth();//矩阵高度
            int height = bitMatrix.getHeight();//矩阵宽度
            int halfW = width / 2;
            int halfH = height / 2;

            //缩放logo图片
            Matrix m = new Matrix();
            float sx = (float) 2 * logoSize / mBitmap.getWidth();
            float sy = (float) 2 * logoSize / mBitmap.getHeight();
            m.setScale(sx, sy);
            mBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
                    mBitmap.getWidth(), mBitmap.getHeight(), m, false);

            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (x > halfW - logoSize && x < halfW + logoSize
                            && y > halfH - logoSize
                            && y < halfH + logoSize) {
                        //该位置用于存放图片信息
                        //记录图片每个像素信息
                        pixels[y * width + x] = mBitmap.getPixel(x - halfW
                                + logoSize, y - halfH + logoSize);
                    } else {
                        if (bitMatrix.get(x, y)) {
                            pixels[y * size + x] = 0xff000000;
                        } else {
                            pixels[y * size + x] = 0xffffffff;
                        }
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read content from the given image
     *
     * @param drawable img
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
