package org.swdc.skybox.core.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EncryptedHeader {

    private String encodeLevel;
    private String resolverName;
    private Boolean isFolder;
    private List<DictionaryEntry> entries;
    private long folderOffset;
    private String name;

    public EncryptedHeader() {

    }

    public EncryptedHeader(String encodeLevel, String resolverName, Boolean isFolder, List<DictionaryEntry> entries) {
        this.encodeLevel = encodeLevel;
        this.resolverName = resolverName;
        this.isFolder = isFolder;
        this.entries = entries;
        this.folderOffset = -1L;
    }

}
