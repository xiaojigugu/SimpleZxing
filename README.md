
# ZxingDemo
  [![](https://www.jitpack.io/v/xiaojigugu/SimpleZxing.svg)](https://www.jitpack.io/#xiaojigugu/SimpleZxing)
###### 优化内容：  
    1.去除多余格式支持，仅支持二维码  
    2.增加自动对焦间隔控制  
    3.增加双击自动缩放功能  
    4.增大识别区域（>扫描区域）  
    5.增加生成二维码方法  
    6.增加从图片读取二维码功能  


###### How to Use：

    Step 1.  
    Add it in your root build.gradle at the end of repositories:  
     allprojects {  
        repositories {  
          ...  
         maven { url 'https://jitpack.io' }  
       }  
      }  
      
    Step 2.     
    Add the dependency:  
     dependencies {  
       implementation 'com.github.xiaojigugu:SimpleZxing:1.1.2'  
     }  
     
     step 3.
```java 
     //扫码
                        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                        intent.putExtra(CaptureActivity.AUTO_FOCUS, 500);//自动对焦时间
                        intent.putExtra(CaptureActivity.DOUBLE_TAP_ZOOM, true);//双击缩放
                        intent.putExtra(CaptureActivity.VIBRATE,true);//震动
                        intent.putExtra(CaptureActivity.BEEP,true);//蜂鸣音
                        startActivityForResult(intent, REQUEST_CODE_SCAN);
    //解析结果        
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
```
