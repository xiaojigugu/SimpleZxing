# ZxingDemo
  
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
       implementation 'com.github.xiaojigugu:SimpleZxing:1.1.0'  
     }  
