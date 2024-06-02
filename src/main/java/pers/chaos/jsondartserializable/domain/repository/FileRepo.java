package pers.chaos.jsondartserializable.domain.repository;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import pers.chaos.jsondartserializable.domain.enums.DartConst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 文件输出仓库
 */
@AllArgsConstructor
public class FileRepo {
    private final VirtualFile parent;
    private final Project project;

    public VirtualFile findDartFile(String filename) {
       return parent.findChild(filename + DartConst.File.suffix);
    }

    public void createDartFile(String filename, String fileContent) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile handle = parent.createChildData(null, filename + DartConst.File.suffix);
                handle.setBinaryContent(fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
