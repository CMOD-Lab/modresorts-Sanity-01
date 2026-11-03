package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

import com.acme.modres.mbean.IOUtils;
import com.google.gson.Gson;

public class JsonInputStream extends FileInputStream {

  private final File file;
  private final InputStream delegate;

  public JsonInputStream(File file) throws FileNotFoundException {
    super(resolveBackingFile(file));
    this.file = file;
    byte[] inMemoryContent = IOUtils.getInMemoryFileContent(file);
    if (inMemoryContent != null) {
      this.delegate = new ByteArrayInputStream(inMemoryContent);
    } else {
      this.delegate = new FileInputStream(file);
    }
  }

  private static File resolveBackingFile(File file) throws FileNotFoundException {
    byte[] inMemoryContent = IOUtils.getInMemoryFileContent(file);
    if (inMemoryContent != null) {
      try {
        File tempFile = Files.createTempFile("json-input-stream", ".tmp").toFile();
        tempFile.deleteOnExit();
        return tempFile;
      } catch (IOException e) {
        throw new FileNotFoundException(e.getMessage());
      }
    }
    return file;
  }

  public Object parseJsonAs(Class<?> cls) {
    if (file != null && file.exists()) {
      Object jsonObject = null;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(delegate))) {
        Gson gson = new Gson();
        jsonObject = gson.fromJson(reader, cls);
      } catch (Exception e) {
        e.printStackTrace();
      } catch (Throwable e) {
        e.printStackTrace();
      }
      return jsonObject;
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    try {
      delegate.close();
    } finally {
      super.close();
    }
  }
}
