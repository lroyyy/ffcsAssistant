package cn.getfei.util.log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

public class CustomFileStreamHandler extends StreamHandler{
    //输出流
    private MeteredStream msOut;
    //日志玩家的写入的字节数, limit 为0 表示没有限制
    private int limit = 50000;
    //是否添加的玩家末尾
    private boolean append;
    //保存存在的日志文件
    private LinkedList<File> files;
    //希望写入的日志文件
    private File file;
    //希望写入的日志路径
    private String fileUrl;
    //希望写入的当天日志路径
    private String fileUrlDay;
    //保存几天之内的日志文件
    private int dateInter = 1;
    //索引文件，用于记录当前日志记录信息，请不要认为的修改
    private File indexFile;

    /**
     * 初始化自定义文件流处理器
     * @param fileUrl 文件路径， 可以是个目录或希望的日志名称，如果是个目录则日志为“未命名”
     * 指定日志名称时不需要包括日期，程序会自动生成日志文件的生成日期及相应的编号
     * @param limit 每个日志希望写入的最大字节数，如果日志达到最大字节数则当天日期的一个新的编 号的日志文件将被创建，最新的日志记录在最大编号的文件中
     * @param dateInter 事务保存的日期间隔
     * @param append 是否将日志写入已存在的日志文件中
     * @throws java.lang.Exception
     */

    public CustomFileStreamHandler(String fileUrl, int limit, int dateInter, boolean append) throws Exception {
        super();
        this.fileUrl = fileUrl;
        if (dateInter <= 0) {
            throw new IllegalArgumentException("时间间隔必须大于0");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("写入日志文件的最大字节数必须大于0");
        }
        this.limit = limit;
        this.dateInter = dateInter;
        this.append = append;
        openWriteFiles();
    }

    public CustomFileStreamHandler(String fileUrl, boolean append) throws Exception {
        super();
        this.fileUrl = fileUrl;
        this.append = append;
        openWriteFiles();
    }

    /**
     * 获得将要写入的文件
     */
    private synchronized void openWriteFiles() throws Exception {
        if (fileUrl == null) {
            throw new IllegalArgumentException("文件路径不能为null");
        }
        //files = getWritedLog();
        //checkLogFile();
        getWriteFile();
        if (append) {
            //openFile(files.getLast(), append);
            openFile(file, append);
        } else {
            getLastFile();
        }
    }

    private void getWriteFile()throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String trace = sdf.format(new Date().getTime());
        fileUrlDay = fileUrl+"_"+trace+".log";
        file = new File(fileUrlDay);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    /**
     * 打开需要写入的文件
     * @param file 需要打开的文件
     * @param append 是否将内容添加到文件末尾
     */
    private void openFile(File file, boolean append) throws Exception {
        //System.out.println("***opend = true " + file.toString());
        int len = 0;
        if (append) {
            len = (int) file.length();
        }
        FileOutputStream fout = new FileOutputStream(file.toString(), append);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        msOut = new MeteredStream(bout, len);
        setOutputStream(msOut);
    }

    /**
     * 将离现在最近的文件作为写入文件的文件
     * 例如 D:logmylog_30_2008-02-19.log
     * mylog表示自定义的日志文件名，2008-02-19表示日志文件的生成日期，30 表示此日期的第30个日志文件
     */
    private void getLastFile() {
        try {
            super.close();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String trace = sdf.format(new Date().getTime());
            int maxLogCount = 0; //获得当前最大的日志文件编号
            //System.out.println("********共有文件**********");
            for (File file : files) {
                //System.out.println(file.toString());
                String fileName = file.toString();
                //获得相同同日期日志文件的最大编号
                if (fileName.indexOf(trace) != -1) {
                    int last = fileName.lastIndexOf('_');
                    int beforeLast = fileName.lastIndexOf('_', last - 1);
                    maxLogCount = Integer.valueOf(fileName.substring(beforeLast + 1, last));
                }
            }

            //System.out.println("********");
            //System.out.println("maxLogCount == " + maxLogCount);
            File file = new File(fileUrl);
            String logIndex = (maxLogCount + 1) + "_" + trace;
            if (file.isDirectory()) { //是个目录
                files.add(new File(fileUrl + File.separator + "未命名_" + logIndex + ".log"));
            } else {
                files.add(new File(fileUrl + "_" + logIndex + ".log"));
            }
            writeLogIndex(logIndex, true);
            openFile(files.getLast(), false);
        } catch (Exception ex) {
            Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 读取已经记录的日志的时间信息
     */
    private LinkedList<File> getWritedLog() {
        LinkedList<File> fileList = new LinkedList<File>();
        BufferedReader br = null;
        try {
            File file = new File(fileUrl);
            if (fileUrl.endsWith("/") || fileUrl.endsWith("/")) {  //是个目录
                if (!file.exists()) {
                    file.mkdirs();
                }
            }
            if (file.isDirectory()) { //只有指定file存在且是个目录
                indexFile = new File(file.toString() + File.separator + "logindex");
            } else {
                indexFile = new File(file.getParent() + File.separator + "logindex");
            }
            if (!indexFile.exists()) {
                indexFile.createNewFile();
            }

            FileReader fr = null;
            fr = new FileReader(indexFile);
            br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() != 0) {
                    if (file.isDirectory()) { //是个目录
                        fileList.add(new File(fileUrl + File.separator + "未命名" + "_" + line + ".log"));
                    } else {
                        fileList.add(new File(fileUrl + "_" + line + ".log"));
                    }
                    System.out.println("line == " + line);
                }
            }
            return fileList;

        } catch (Exception ex) {
            Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    /**
     * 写入日志索引
     * @param logIndex 日志所以
     * @param isAppend 是否添加到索引文件中
     */
    private void writeLogIndex(String logIndex, boolean isAppend) {
        File file = new File(fileUrl);
        BufferedWriter bw = null;
        try {
            FileWriter fw = null;
            if (file.isDirectory()) { //是个目录
                //是个目录
                fw = new FileWriter(new File(file.toString() + File.separator + "logindex"), isAppend);
            } else {
                fw = new FileWriter(new File(file.getParent() + File.separator + "logindex"), isAppend);
            }
            bw = new BufferedWriter(fw);
            bw.newLine();
            bw.write(logIndex, 0, logIndex.length());
            bw.flush();

        } catch (Exception ex) {
            Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * 检查当前日志时间
     * 删除过期日志，并检查日志索引中是否包含了现在日期
     */
    private void checkLogFile() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String trace = sdf.format(new Date().getTime());
            boolean isIncludeNow = false;
            LinkedList<File> overdueLog = new LinkedList<File>(); //过期的日志文件
            long nowDate = sdf.parse(trace).getTime();
            for (File file : files) {
                if (file.toString().indexOf(trace) != -1) {
                    isIncludeNow = true;
                }
                if ((nowDate - sdf.parse(file.toString(), new ParsePosition(file.toString().lastIndexOf('_') + 1)).getTime()) / (86400 * 1000) > 5) {
                    overdueLog.add(file);
                    //System.err.println("将被删除的日志为 " + file);
                }
            }

            //删除过期日志记录, 并重写日志索引文件
            if (overdueLog.size() != 0) {
                files.removeAll(overdueLog);
                indexFile.delete();
                indexFile.createNewFile();
                String fileStr = null;
                for (File file : files) {
                    fileStr = file.toString();
                    writeLogIndex(fileStr.substring(fileStr.lastIndexOf('_') - 1, fileStr.lastIndexOf('.')), true);
                }
                //删除过期文件
                for (File file : overdueLog) {
                    file.delete();
                }
            }

            //如果没有包含当天的日期同时日志需要添加到文件末尾，则添加当天日期的日志文件
            if (!isIncludeNow && append) {
                File file = new File(fileUrl);
                String logIndex = 1 + "_" + trace;
                if (file.isDirectory()) {
                    //是个目录
                    files.add(new File(fileUrl + File.separator + "未命名_" + logIndex + ".log"));
                } else {
                    files.add(new File(fileUrl + "_" + logIndex + ".log"));
                }
                writeLogIndex(logIndex, true);
            }
        } catch (Exception ex) {
            Logger.getLogger(CustomFileStreamHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 发布日志信息
     */
    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        super.flush();
        if (limit > 0 && msOut.written >= limit) {
            getLastFile();
        }
    }

    /**
     * 抄自FileHandler的实现，用于跟踪写入文件的字节数
     * 这样以便提高效率
     */
    private class MeteredStream extends OutputStream {

        private OutputStream out;
        //记录当前写入字节数
        private int written;

        MeteredStream(OutputStream out, int written) {
            this.out = out;
            this.written = written;
        }

        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        @Override
        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff, off, len);
            written += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }


}
