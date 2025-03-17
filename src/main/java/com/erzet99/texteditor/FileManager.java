package com.erzet99.texteditor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {
    public static Path currentFile;

    public static List<String> getContentFromFile(String fileName) {
        Path path = Path.of(fileName);
        currentFile = path;
        if(Files.exists(currentFile)) {
            try (Stream<String> stream = Files.lines(currentFile)) {
                List<String> content =  stream.collect(Collectors.toCollection(ArrayList::new));
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String saveContentToFile(List<String> content) {
        try {
            Files.write(currentFile, content);
            return("File saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
            return("Error during saving file: " + e.getMessage());
        }
    }
}
