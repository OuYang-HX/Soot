import soot.*;
import soot.options.Options;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Soot {
    public static void main(String[] args) {

        String sourceClasses="D:\\FusionCheck\\testProject\\InfoLeakTest\\SetAndGet\\target\\classes";
        String processProjectName=(sourceClasses.split("\\\\"))[4];
        List<String> sourceDirList = Arrays.stream(sourceClasses.split(";")).collect(Collectors.toList());
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_dir(sourceDirList);//需要分析的.class文件路径
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().set_keep_line_number(true);
        Options.v().set_output_format(Options.output_format_shimple);
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
        for(SootClass sootClass:Scene.v().getApplicationClasses()){
            CreateJimple.write(sootClass,processProjectName);
        }
    }
}
