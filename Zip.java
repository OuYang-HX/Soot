import java.io.*;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class ZipTest {
    public static void main(String[] args) throws IOException {
        String classPath="D:\\FusionCheck\\test\\test1\\out\\artifacts\\test1_jar\\test1.zip";
        String className="com.huawei.useCase.MainClass";
        String classDir=classPath.substring(0,classPath.lastIndexOf("."))+File.separator+className.substring(0,className.lastIndexOf(".")).replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        File dir = new File(classDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String targetFile=className.substring(className.lastIndexOf(".")+1)+ ".class";
        try{
            ZipInputStream zipInput = new ZipInputStream(new FileInputStream(classPath));
            ZipEntry zipEntry;
            while((zipEntry=zipInput.getNextEntry())!=null){
                if(zipEntry.getName().endsWith(targetFile)){
                    File temp=new File(dir+File.separator+targetFile);
                    if(!temp.exists()){
                        temp.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(temp);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    byte[] bytes = new byte[1024];
                    int n;
                    while((n=zipInput.read(bytes))!=-1){
                        bufferedOutputStream.write(bytes, 0, n);
                    }
                    //关闭流
                    bufferedOutputStream.close();
                    fileOutputStream.close();
                }
            }
        }catch (Throwable e){
           throw new ZipException();
        }finally {
            try{
            }catch (Throwable t){
                System.out.println("Error");
            }
        }
    }
}
