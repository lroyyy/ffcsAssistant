package cn.getfei.util;

import org.openqa.selenium.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Auther: zhengf
 * @Date: 2021/01/14 9:59
 * @Description:
 * @Version:1.0
 */
public class SeleniumUtils {

    public static void switchToNewWindow(WebDriver driver) {
        String FirstHandle = driver.getWindowHandle();     //首先得到最先的窗口 权柄
        for (String winHandle : driver.getWindowHandles()) {    //得到浏览器所有窗口的权柄为Set集合，遍历
            if (winHandle.equals(FirstHandle)) {                //如果为 最先的窗口 权柄跳出
                continue;
            }
            driver.switchTo().window(winHandle);             //如果不为 最先的窗口 权柄，将 新窗口的操作权柄  给 driver
            break;
        }
    }

    public static void switchToIframe(WebDriver driver, String... iframeIds) {
        for (String iframeId : iframeIds) {
            WebElement iframe = driver.findElement(By.id(iframeId));
            driver.switchTo().frame(iframe);
        }
    }

    public static File captureElement(File screenshot, WebElement element) {
        try {
            ImageIO.setUseCache(false);
            BufferedImage img = ImageIO.read(screenshot);
            int width = element.getSize().getWidth();
            int height = element.getSize().getHeight();
            //获取指定元素的坐标
            Point point = element.getLocation();
            //从元素左上角坐标开始，按照元素的高宽对img进行裁剪为符合需要的图片
            BufferedImage dest = img.getSubimage(point.getX(), point.getY(), width, height);
            ImageIO.write(dest, "png", screenshot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return screenshot;
    }

    public static void getVerificationCodeImg(WebDriver driver, WebElement element,String outputPath) throws IOException {
// 获取到截图的文件
        File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

//根据据图中样式或者id 进行再次截图
        BufferedImage img = ImageIO.read(screenshotFile);
// 获得元素的高度和宽度
        int width = element.getSize().getWidth();
        int height = element.getSize().getHeight();
// 创建一个矩形使用上面的高度，和宽度
//        Rectangle rect = new Rectangle(width, height);
// 得到元素的坐标
        Point p = element.getLocation();
        BufferedImage dest = img.getSubimage(p.getX(), p.getY(), width, height);
// 存为png格式
        ImageIO.write(dest, "png", screenshotFile);
        if ((screenshotFile != null) && screenshotFile.exists()) {
            // 截取到的图片存到本地
            FileOutputStream out = null;
            FileInputStream in = null;

            in = new FileInputStream(screenshotFile);
            out = new FileOutputStream(outputPath);
            byte[] b = new byte[1024];
            while (true) {
                int temp = in.read(b, 0, b.length);
                // 如果temp = -1的时候，说明读取完毕
                if (temp == -1) {
                    break;
                }
                out.write(b, 0, temp);
            }
        }
    }

    public static boolean isAlertPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException Ex) {
            return false;
        }
    }

}
