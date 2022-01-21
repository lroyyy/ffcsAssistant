package cn.getfei.util;

import cn.easyproject.easyocr.EasyOCR;
import cn.easyproject.easyocr.ImageType;

/**
 * @Auther: zhengf
 * @Date: 2021/02/01 14:13
 * @Description:
 * @Version:1.0
 */
public class EasyOCRUtils {

    public static EasyOCR getEisOcr(){
        String tesseractPath = "D:/Program Files/Tesseract-OCR/tesseract";
//        String imgUrl1 = "res/img/img_NORMAL.jpg";
        String imgUrl1 = "res/img/1.png";
//        String imgUrl2 = "res/img/img_INTERFERENCE_LINE.png";
        EasyOCR e = new EasyOCR();
        e.setTesseractPath(tesseractPath);
        e.setTesseractOptions("digits");
//        e.setTesseractOptions("-psm 7 number");
        return e;
    }

}
