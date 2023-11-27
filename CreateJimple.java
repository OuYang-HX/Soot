/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2016-2022. All rights reserved.
 */

import com.huawei.us.common.file.UsFileLiteUtils;
import com.huawei.us.common.file.UsFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Printer;
import soot.SootClass;
import soot.SourceLocator;
import soot.options.Options;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 输出jimple文件
 *
 * @author ltf
 * @since 2021-01-25
 */
public class CreateJimple {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateJimple.class);

    public static void write(SootClass sClass,String processProjectName) {
        try {
            String filename = SourceLocator.v().getFileNameFor(sClass, Options.output_format_shimple);
            filename = "JimpleResult" + File.separator + processProjectName + File.separator + filename;
            int index = filename.lastIndexOf(File.separator);
            String fileNameTemp = filename.substring(0, index);
            if (!UsFileLiteUtils.isSecurityFileName(fileNameTemp)) {
                LOGGER.error("invalid character in pathname");
                return;
            }
            File directory = UsFileUtils.getFile(fileNameTemp);
            if (!directory.exists()) {
                LOGGER.info("dir {} is created {}", directory.getName(), directory.mkdirs());
            }
            if (!UsFileLiteUtils.isSecurityFileName(filename)) {
                LOGGER.error("invalid character in pathname");
                return;
            }
            File file = UsFileUtils.getFile(filename);
            if (!file.exists()) {
                LOGGER.info("dir {} is created {}", file.getName(), file.createNewFile());
            }
            File outputFile = new File("sootOutput");
            if (outputFile.exists()) {
                LOGGER.info("dir {} is deleted {}", outputFile.getName(), outputFile.delete());
            }
            try (OutputStream streamOut = Files.newOutputStream(UsFileUtils.getFile(filename).toPath());
                 PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut, StandardCharsets.UTF_8))) {
                Printer.v().printTo(sClass, writerOut);
                writerOut.flush();
            }
        } catch (Exception e) {
            LOGGER.error("not find file");
        }
    }
}
