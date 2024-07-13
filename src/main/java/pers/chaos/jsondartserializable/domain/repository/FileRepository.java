package pers.chaos.jsondartserializable.domain.repository;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import pers.chaos.jsondartserializable.domain.service.DartGenOption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文件输出仓库
 */
@AllArgsConstructor
public class FileRepository {
    private final VirtualFile parent;
    private final Project project;

    public VirtualFile findDartFile(String filename) {
       return parent.findChild(DartGenOption.FileSuffix.DART.filename(filename));
    }

    public void createDartFile(String filename, String fileContent) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile handle = parent.createChildData(null, DartGenOption.FileSuffix.DART.filename(filename));
                handle.setBinaryContent(fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
