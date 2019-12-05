package com.junt.zxingdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.ZxingUtils;
import com.google.zxing.client.android.CaptureActivity;
import com.qw.soul.permission.SoulPermission;
import com.qw.soul.permission.bean.Permission;
import com.qw.soul.permission.callbcak.CheckRequestPermissionListener;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_SCAN = 11;
    private TextView textView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        //扫码
        findViewById(R.id.btnScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoulPermission.getInstance().checkAndRequestPermission(Manifest.permission.CAMERA, new CheckRequestPermissionListener() {
                    @Override
                    public void onPermissionOk(Permission permission) {
                        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                        intent.putExtra(CaptureActivity.AUTO_FOCUS, 500);//自动对焦时间
                        intent.putExtra(CaptureActivity.DOUBLE_TAP_ZOOM, true);//双击缩放
                        intent.putExtra(CaptureActivity.VIBRATE,true);//震动
                        intent.putExtra(CaptureActivity.BEEP,true);//蜂鸣音
                        startActivityForResult(intent, REQUEST_CODE_SCAN);
                    }

                    @Override
                    public void onPermissionDenied(Permission permission) {
                        Toast.makeText(MainActivity.this, "权限获取失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        //生成二维码
        findViewById(R.id.btnEncode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageBitmap(ZxingUtils.qrCodeWriter(300, 300, "963852"));
            }
        });

        findViewById(R.id.btnEncodeWithLogo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmapLogo= BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round);
                imageView.setImageBitmap(ZxingUtils.qrCodeWriterWithLogo(300,10,"这是二维码文字信息",bitmapLogo));
            }
        });

        //长按识别图中二维码
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String result = ZxingUtils.qrCodeReaderFromImg(imageView.getDrawable());
                Toast.makeText(MainActivity.this, "二维码内容:" + result, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "取消扫描", Toast.LENGTH_SHORT).show();
            } else {
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    if (bundle != null) {
                        if (bundle.getInt(CaptureActivity.RESULT_TYPE) == RESULT_OK) {
                            String result = bundle.getString(CaptureActivity.RESULT_STRING);
                            textView.setText("result：" + result);
                        }
                    }
                }
            }
        }
    }
}
