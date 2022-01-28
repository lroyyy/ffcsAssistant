package cn.getfei.core;

import cn.easyproject.easyocr.EasyOCR;
import cn.easyproject.easyocr.ImageType;
import cn.getfei.util.log.CustomFileStreamHandler;
import cn.getfei.util.EasyOCRUtils;
import cn.getfei.util.SeleniumUtils;
import cn.getfei.util.log.CustomFormatter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * @Auther: zhengf
 * @Date: 2021/01/13 9:17
 * @Description:
 * @Version:1.0
 */
public class Task {
    /**
     * 默认隐式等待秒数
     */
    private static final long DEFAULT_IMPLICITLY_WAIT_SECONDS = 10;
    /**
     * 默认工时
     */
    public static final int DEFAULT_ONE_DAY_MAN_HOURS = 8;

    private WebDriver driver;

    private static final String URL = "http://eis.ffcs.cn/irj/servlet/prt/portal/prtroot/com.sap.portal.navigation.portallauncher.default";

    private static final String CHROME_DRIVER_PATH = "D:\\DevelopTools\\chromedriver\\chromedriver.exe";

    private static final String TMP_DIR = "D:\\Work\\ideas\\ffcsAssistant\\res\\img\\before\\";
//    private static final String TMP_DIR = "res/img/after/";
//    private static final String VERIFICATION_CODE_IMG_BEFORE_PATH = "res/img/before/";
    private static final String VERIFICATION_CODE_IMG_BEFORE_PATH = TMP_DIR;
    private static final String VERIFICATION_CODE_IMG_AFTER_PATH = "res/img/after/";

    private static Logger logger;

    public Task() throws Exception {
        init();
    }

    private void init() throws Exception {
        initLogger();
        initWebDriver();
    }

    private void initLogger() throws Exception {
        logger = Logger.getLogger("logger");
        CustomFileStreamHandler fh = new CustomFileStreamHandler("console", 0, 1000, true);
        fh.setEncoding("UTF-8");
        fh.setFormatter(new CustomFormatter());
//        logger.setUseParentHandlers(false);
//        logger.addHandler(fh);
    }

    private void initWebDriver() {
        //chrome参数
        ChromeOptions options = new ChromeOptions();
        //无界面参数 禁用沙盒，否则报错
//        options.addArguments("headless","no-sandbox");
        // 禁用阻止弹出窗口
        options.addArguments("--disable-popup-blocking");
        // 禁用保存密码提示框
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        // 默认浏览器检查
        options.addArguments("no-default-browser-check");

        //构造driver并加载url
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);//设置驱动的路径

        driver = new ChromeDriver(options);
        String url = URL;
        driver.get(url);

        //设置全局隐式等待时限
        driver.manage().timeouts().implicitlyWait(DEFAULT_IMPLICITLY_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    public boolean login() {
        //填写表单
        String username = "os.wangh";
        driver.findElement(By.name("j_user")).sendKeys(username);
        String password = "FFcs@1234";
        driver.findElement(By.name("j_password")).sendKeys(password);
        int count = 0;
        WebElement imgVerificationCode = driver.findElement(By.xpath("//img[@src='http://bem.ffcs.cn:81/mis/Images.aspx']"));
        // 识别并输入验证码
        EasyOCR ocr = EasyOCRUtils.getEisOcr();
        try {
            while (true) {
                count++;
                String verificationCodeImgFileName = System.currentTimeMillis() + ".jpg";
                String verificationCodeImgFilePath = VERIFICATION_CODE_IMG_BEFORE_PATH + verificationCodeImgFileName;
                try {
                    SeleniumUtils.getVerificationCodeImg(driver, imgVerificationCode, verificationCodeImgFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.info("验证码识别失败");
                    continue;
                }
                System.setProperty("java.io.tmpdir", TMP_DIR);
                String verificationCode = ocr.discernAndAutoCleanImage(verificationCodeImgFilePath,ImageType.CAPTCHA_NORMAL);
    //            String verificationCode = ocr.discernToFileAndGetAndAutoCleanImage(verificationCodeImgFilePath
    //                    , VERIFICATION_CODE_IMG_AFTER_PATH+verificationCodeImgFileName,ImageType.CAPTCHA_NORMAL);
                logger.info("识别出：" + verificationCode);
                if ("".equals(verificationCode)) {
                    //点击验证码图片以刷新验证码
                    imgVerificationCode.click();
                    continue;
                }

                //填写验证码
                WebElement inputVerificationCode = driver.findElement(By.id("txtValidateNum"));
                inputVerificationCode.clear();
                inputVerificationCode.sendKeys(verificationCode);
                //点击登录
//                WebElement element = driver.findElement(By.xpath("//img[@src='/irj/portalapps/com.ffcs.portal.runtime.logon/layout\\loading_l.png']"));
//                element.click();
                //alert
                if (SeleniumUtils.isAlertPresent(driver)) {
                    Alert alert = driver.switchTo().alert();
                    String text = alert.getText();
                    if ("验证码错误".equals(text)) {
                        driver.switchTo().alert().accept();
                        //点击验证码图片以刷新验证码
                        imgVerificationCode.click();
                        continue;
                    }
                    if ("用户验证失败".equals(text)) {
                        logger.info("用户名或密码错误");
                        return false;
                    }
                }
                logger.info("尝试次数：" + count);
                return true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void toWork() {
        switchToIbIframe();
        //通过ip校验
//        String correctIp=driver.findElement(By.id("pcIpAddress")).getAttribute("value");//正确ip
//        WebElement inputLocalIpAddress=driver.findElement(By.id("pcLocalIpAddress"));
//        String localIpAddress=inputLocalIpAddress.getAttribute("value");//本机ip
//        if(!correctIp.equals(localIpAddress)){
//            JavascriptExecutor js=(JavascriptExecutor)driver;
//            js.executeScript("document.getElementById('pcLocalIpAddress').value='"+correctIp+"'");
//            logger.info("本机ip："+localIpAddress+"，用户绑定ip："+correctIp);
//        }
        //点击上班
        driver.findElement(By.id("ibOn")).click();
        //判断是否成功
        SeleniumUtils.switchToNewWindow(driver);
//        WebElement iframe4 = driver.findElement(By.name("meizzCalendarIframe"));
//        driver.switchTo().frame(iframe4);
        WebElement amRemarkTd = driver.findElement(By.id("tdAmRemark"));
        if ("正常".equals(amRemarkTd.getText())) {
            logger.info("上班打卡成功！");
        }
    }

    public void getOffWork() {
        //点击下班
        switchToIbIframe();
        driver.findElement(By.id("ibOff")).click();
        //填写工时并确认
        SeleniumUtils.switchToNewWindow(driver);
        WebElement timeInput = driver.findElements(By.className("cinput")).get(0);
        timeInput.clear();
        timeInput.sendKeys(String.valueOf(DEFAULT_ONE_DAY_MAN_HOURS));
        driver.findElement(By.id("btnsave")).click();
        //判断是否成功
        WebElement pmRemarkTd = driver.findElement(By.id("tdPmRemark"));
        if ("正常".equals(pmRemarkTd.getText())) {
            logger.info("下班打卡成功！");
        }
    }

    private void switchToIbIframe() {//                     ivuFrm_page0ivu0
        //SeleniumUtils.switchToIframe(driver, "ivuFrm_page0ivu0", "EISBottom", "iLeftIframe");
        SeleniumUtils.switchToIframe(driver, "ivuFrm_page0ivu0");
    }

    public static void main(String[] args) {
        try {
            Task task = new Task();
            boolean loginSuccess = task.login();
            if (!loginSuccess) {
                return;
            }
//        task.toWork();
        task.getOffWork();
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            logger.info(sw.toString());
        }
    }

}
